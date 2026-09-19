package lk.novalink.zerotrace.data.repository

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import lk.novalink.zerotrace.core.AuthJson
import lk.novalink.zerotrace.core.Pkce
import lk.novalink.zerotrace.core.UpdateManager
import lk.novalink.zerotrace.data.model.SubscriptionInfo
import lk.novalink.zerotrace.data.model.UserProfile
import lk.novalink.zerotrace.parser.ConfigParser
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

data class AuthState(
    /** null until the secure store has been read once at startup. */
    val sessionPresent: Boolean? = null,
    val profile: UserProfile? = null,
    val subscriptions: List<SubscriptionInfo> = emptyList(),
    /** A sign-in is in flight (browser opened, waiting for the callback) or the account is refreshing. */
    val loading: Boolean = false,
    val error: String? = null,
    val offlineChoice: Boolean = false
) {
    /** Launch gate: shown until the user signs in or picks "Use this device offline". */
    val showLoginGate: Boolean get() = sessionPresent == false && profile == null && !offlineChoice

    /** Still reading the secure store; avoid flashing the main UI before a possible gate. */
    val checkingSession: Boolean get() = sessionPresent == null && !offlineChoice
}

/**
 * Optional NetchSuite account: OAuth 2.0 + PKCE through the system browser and the
 * netchvpn://auth/callback deep link, plus syncing the account's active subscriptions into the
 * server list. The app stays fully usable without it.
 */
class AuthRepository(
    context: Context,
    private val configRepository: ConfigRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flags = appContext.getSharedPreferences(FLAGS_PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AuthState(offlineChoice = flags.getBoolean(KEY_OFFLINE, false)))
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private class Session(val access: String, val refresh: String, val avatar: String)
    private class Pending(val verifier: String, val state: String)
    private class HttpResult(val status: Int, val body: String)
    private class SessionExpiredException : Exception("SESSION_EXPIRED")

    private val securePrefs: SharedPreferences? by lazy { openSecurePrefs() }
    private val sessionLock = Any()
    private var cachedSession: Session? = null
    private var sessionLoaded = false
    private var pendingInMemory: Pending? = null
    private val refreshLock = Mutex()

    private val userAgent = "ZeroTrace-Android/${UpdateManager.getCurrentVersionName(appContext)}"

    init {
        scope.launch {
            val session = loadSession()
            _state.update { it.copy(sessionPresent = session != null) }
            if (session != null) refreshAccount()
        }
    }

    // ── Public actions ────────────────────────────────────────────────────

    /** Opens the system browser on the NetchSuite authorize page. */
    fun startLogin(activityContext: Context) {
        val verifier = Pkce.generateVerifier()
        val oauthState = Pkce.generateState()
        val url = Uri.parse("$API_BASE/auth/app/authorize").buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("code_challenge", Pkce.challengeFor(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", oauthState)
            .appendQueryParameter("device_name", deviceName())
            .appendQueryParameter("platform", PLATFORM)
            .build()

        savePending(Pending(verifier, oauthState))
        _state.update { it.copy(loading = true, error = null) }
        try {
            // NEW_TASK keeps the browser out of our task, so the callback finds our singleTop activity on top.
            activityContext.startActivity(Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            clearPending()
            _state.update { it.copy(loading = false, error = "No web browser found to sign in with.") }
        }
    }

    fun cancelLogin() {
        clearPending()
        _state.update { it.copy(loading = false, error = null) }
    }

    /** Completes the flow when the browser redirects to netchvpn://auth/callback. */
    fun handleCallback(uri: Uri) {
        val code = uri.getQueryParameter("code")
        val oauthState = uri.getQueryParameter("state")
        val denied = uri.getQueryParameter("error")
        scope.launch {
            val pending = takePending()
            if (denied != null) {
                _state.update { it.copy(loading = false, error = "Sign-in was cancelled.") }
                return@launch
            }
            if (code.isNullOrEmpty() || oauthState.isNullOrEmpty() || pending == null || oauthState != pending.state) {
                Log.w(TAG, "Ignoring callback: missing, expired or mismatched state")
                _state.update { it.copy(loading = false, error = "Sign-in failed — please try again.") }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            try {
                val res = http("POST", "/api/v1/auth/token", null, JsonObject().apply {
                    addProperty("grant_type", "authorization_code")
                    addProperty("code", code)
                    addProperty("code_verifier", pending.verifier)
                    addProperty("redirect_uri", REDIRECT_URI)
                    addProperty("device_name", deviceName())
                    addProperty("platform", PLATFORM)
                })
                val tokens = AuthJson.parseTokens(res.body)
                val access = tokens.access
                val refresh = tokens.refresh
                if (access == null || refresh == null) {
                    throw IOException(tokens.error.ifEmpty { AuthJson.describeBadResponse(res.status, res.body) })
                }
                saveSession(access, refresh, tokens.avatarUrl)
                _state.update { it.copy(sessionPresent = true) }
                refreshAccount()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = friendly(e)) }
            }
        }
    }

    fun refresh() {
        scope.launch { refreshAccount() }
    }

    /** Signs out. Configs stay; the user lands back in the app rather than on the login page. */
    fun logout() {
        scope.launch {
            val session = loadSession()
            if (session != null) {
                runCatching { http("POST", "/api/v1/auth/revoke", session.access, null) }
            }
            clearSession()
            flags.edit().putBoolean(KEY_OFFLINE, true).apply()
            _state.update {
                it.copy(
                    sessionPresent = false, profile = null, subscriptions = emptyList(),
                    loading = false, error = null, offlineChoice = true
                )
            }
        }
    }

    fun continueOffline() {
        flags.edit().putBoolean(KEY_OFFLINE, true).apply()
        _state.update { it.copy(offlineChoice = true) }
    }

    // ── Account fetch + sync ──────────────────────────────────────────────

    private suspend fun refreshAccount() {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val (profile, subs) = coroutineScope {
                val user = async { fetchUser() }
                val subscriptions = async { fetchSubscriptions() }
                user.await() to subscriptions.await()
            }
            _state.update { it.copy(profile = profile, subscriptions = subs, sessionPresent = true) }

            // Only active subscriptions become servers; activeIds covers all of them (even a link
            // that fails to parse) so a parse failure can never delete an existing config.
            val active = subs.filter { it.isActive }
            val parsed = active.mapNotNull { sub ->
                val cfg = ConfigParser.parseSingle(sub.configUrl) ?: return@mapNotNull null
                cfg.copy(name = sub.packageName.ifBlank { cfg.name }, subscriptionId = sub.id)
            }
            configRepository.syncSubscriptions(active.map { it.id }.toSet(), parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SessionExpiredException) {
            _state.update {
                it.copy(
                    profile = null, subscriptions = emptyList(), sessionPresent = false,
                    error = "Session expired, please sign in again."
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = friendly(e)) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    private suspend fun fetchUser(): UserProfile {
        val res = authorized("/api/v1/user")
        val profile = AuthJson.parseUser(res.body)
            ?: throw IOException(AuthJson.describeBadResponse(res.status, res.body))
        // /api/v1/user may not repeat the picture; use the one saved at sign-in.
        return if (profile.avatarUrl.isEmpty()) profile.copy(avatarUrl = loadSession()?.avatar.orEmpty()) else profile
    }

    private suspend fun fetchSubscriptions(): List<SubscriptionInfo> {
        val res = authorized("/api/v1/subscriptions")
        return AuthJson.parseSubscriptions(res.body)
            ?: throw IOException(AuthJson.describeBadResponse(res.status, res.body))
    }

    // ── HTTP ──────────────────────────────────────────────────────────────

    private fun http(method: String, path: String, bearer: String?, json: JsonObject?): HttpResult {
        val conn = (URL(API_BASE + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
            if (json != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val status = conn.responseCode
            val stream = if (status >= 400) conn.errorStream else conn.inputStream
            return HttpResult(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            conn.disconnect()
        }
    }

    /** GET with the access token; on 401 refreshes once (single-flight) and retries. */
    private suspend fun authorized(path: String): HttpResult = withContext(Dispatchers.IO) {
        val used = loadSession() ?: throw SessionExpiredException()
        var res = http("GET", path, used.access, null)
        if (res.status == 401) {
            // user + subscriptions are fetched concurrently; only one of them may spend the refresh
            // token (it can be single-use), the other reuses the fresh access token.
            val fresh = refreshLock.withLock {
                val now = loadSession() ?: throw SessionExpiredException()
                if (now.access != used.access) now else refreshTokens(now)
            }
            res = http("GET", path, fresh.access, null)
        }
        res
    }

    private fun refreshTokens(current: Session): Session {
        val res = http("POST", "/api/v1/auth/token", null, JsonObject().apply {
            addProperty("grant_type", "refresh_token")
            addProperty("refresh_token", current.refresh)
        })
        val tokens = AuthJson.parseTokens(res.body)
        val access = tokens.access
        val refresh = tokens.refresh
        if (access == null || refresh == null) {
            // Only a real rejection ends the session; a 5xx, WAF page or dropped connection must not sign the user out.
            if (res.status in 400..499) {
                clearSession()
                throw SessionExpiredException()
            }
            throw IOException(AuthJson.describeBadResponse(res.status, res.body))
        }
        saveSession(access, refresh, current.avatar)
        return Session(access, refresh, current.avatar)
    }

    private fun friendly(e: Throwable): String = when (e) {
        is UnknownHostException, is SocketTimeoutException, is ConnectException ->
            "Couldn't reach NetchSuite. Check your connection."
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
    }

    private fun deviceName(): String {
        val maker = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val name = if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model"
        return name.trim().replaceFirstChar { it.uppercase() }.ifEmpty { "ZeroTrace Android" }
    }

    // ── Secure storage ────────────────────────────────────────────────────

    // Tokens live in Keystore-backed encrypted prefs. That file is excluded from backups (see
    // backup_rules.xml / data_extraction_rules.xml) because a restored copy can't be decrypted.
    private fun openSecurePrefs(): SharedPreferences? {
        fun create(): SharedPreferences {
            val key = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            return EncryptedSharedPreferences.create(
                appContext, SECURE_PREFS, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return try {
            create()
        } catch (e: Exception) {
            // Wiped Keystore or a restored file: start clean rather than crash. The user just signs in again.
            Log.w(TAG, "Secure prefs unreadable, resetting", e)
            try {
                appContext.deleteSharedPreferences(SECURE_PREFS)
                create()
            } catch (e2: Exception) {
                Log.e(TAG, "Secure prefs unavailable; session will not persist", e2)
                null
            }
        }
    }

    private fun loadSession(): Session? = synchronized(sessionLock) {
        if (!sessionLoaded) {
            val prefs = securePrefs
            val access = prefs?.getString(KEY_ACCESS, null)
            val refresh = prefs?.getString(KEY_REFRESH, null)
            cachedSession = if (!access.isNullOrEmpty() && !refresh.isNullOrEmpty()) {
                Session(access, refresh, prefs?.getString(KEY_AVATAR, "").orEmpty())
            } else null
            sessionLoaded = true
        }
        cachedSession
    }

    private fun saveSession(access: String, refresh: String, avatar: String) = synchronized(sessionLock) {
        securePrefs?.edit()?.putString(KEY_ACCESS, access)?.putString(KEY_REFRESH, refresh)
            ?.putString(KEY_AVATAR, avatar)?.commit()
        cachedSession = Session(access, refresh, avatar)
        sessionLoaded = true
    }

    private fun clearSession() = synchronized(sessionLock) {
        securePrefs?.edit()?.remove(KEY_ACCESS)?.remove(KEY_REFRESH)?.remove(KEY_AVATAR)?.commit()
        cachedSession = null
        sessionLoaded = true
    }

    // The verifier must survive Android killing the app while the user is in the browser.
    private fun savePending(pending: Pending) {
        pendingInMemory = pending
        securePrefs?.edit()
            ?.putString(KEY_PENDING_VERIFIER, pending.verifier)
            ?.putString(KEY_PENDING_STATE, pending.state)
            ?.putLong(KEY_PENDING_AT, System.currentTimeMillis())
            ?.commit()
    }

    private fun takePending(): Pending? {
        val prefs = securePrefs
        val fromMemory = pendingInMemory
        val savedAt = prefs?.getLong(KEY_PENDING_AT, 0L) ?: 0L
        val verifier = prefs?.getString(KEY_PENDING_VERIFIER, null)
        val savedState = prefs?.getString(KEY_PENDING_STATE, null)
        clearPending()
        if (fromMemory != null) return fromMemory
        val fresh = System.currentTimeMillis() - savedAt < PENDING_TTL_MS
        return if (fresh && !verifier.isNullOrEmpty() && !savedState.isNullOrEmpty()) Pending(verifier, savedState) else null
    }

    private fun clearPending() {
        pendingInMemory = null
        securePrefs?.edit()?.remove(KEY_PENDING_VERIFIER)?.remove(KEY_PENDING_STATE)?.remove(KEY_PENDING_AT)?.commit()
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val API_BASE = AuthJson.API_BASE
        // The backend pairs client_id with platform (netch-android + android), like netch-macos + macos on desktop.
        private const val CLIENT_ID = "netch-android"
        private const val PLATFORM = "android"
        private const val REDIRECT_URI = "netchvpn://auth/callback"

        private const val SECURE_PREFS = "zerotrace_auth_secure"
        private const val FLAGS_PREFS = "zerotrace_auth_flags"
        private const val KEY_OFFLINE = "offline_choice"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_AVATAR = "avatar_url"
        private const val KEY_PENDING_VERIFIER = "pending_verifier"
        private const val KEY_PENDING_STATE = "pending_state"
        private const val KEY_PENDING_AT = "pending_at"
        private const val PENDING_TTL_MS = 15 * 60 * 1000L
    }
}
