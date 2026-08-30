/**
 * ZeroTrace VPN - Telemetry & Analytics Cloudflare Worker
 * 100% Free Serverless Endpoint with Private Authenticated Admin Stats.
 *
 * Deploy to Cloudflare Workers:
 * 1. Go to dash.cloudflare.com -> Workers & Pages -> Create Worker
 * 2. Paste this code into the editor
 * 3. In Settings -> Variables, add an Environment Variable or Secret:
 *    ADMIN_SECRET = "your_secure_password_or_key_here"
 * 4. (Optional) Bind a KV namespace named 'STATS_KV' for persistent counts.
 */

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, User-Agent, Authorization, x-admin-key',
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // 1. Anonymous Heartbeat Ping Endpoint from ZeroTrace Android App
    if (url.pathname === '/api/heartbeat' && request.method === 'POST') {
      try {
        const body = await request.json();
        const { clientId, version, event, protocol } = body;
        const now = Date.now();
        const today = new Date().toISOString().slice(0, 10);

        if (env.STATS_KV && clientId) {
          // Record live active session (TTL: 5 minutes)
          await env.STATS_KV.put(`live:${clientId}`, JSON.stringify({ version, protocol, ts: now }), { expirationTtl: 300 });
          // Record Daily Active User (TTL: 30 days)
          await env.STATS_KV.put(`dau:${today}:${clientId}`, '1', { expirationTtl: 86400 * 30 });
          // Increment total connections counter
          const totalConns = (parseInt(await env.STATS_KV.get('metric:total_connections')) || 0) + 1;
          await env.STATS_KV.put('metric:total_connections', totalConns.toString());
        }

        return new Response(JSON.stringify({ status: 'ok', timestamp: now }), {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      } catch (e) {
        return new Response(JSON.stringify({ status: 'error', message: e.message }), {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      }
    }

    // 2. Private Authenticated Telemetry Stats API
    if (url.pathname === '/api/stats') {
      const authHeader = request.headers.get('x-admin-key') || request.headers.get('Authorization') || url.searchParams.get('key');
      const expectedSecret = env.ADMIN_SECRET || 'zerotrace_admin_secret_2026';

      // Verify Admin Authentication
      if (!authHeader || (authHeader !== expectedSecret && authHeader !== `Bearer ${expectedSecret}`)) {
        return new Response(JSON.stringify({
          error: 'Unauthorized',
          message: 'Access Denied: Private Admin Analytics'
        }), {
          status: 401,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      }

      let liveUsers = 0;
      let todayUsers = 0;
      let totalConns = 0;

      if (env.STATS_KV) {
        const today = new Date().toISOString().slice(0, 10);
        const liveList = await env.STATS_KV.list({ prefix: 'live:' });
        const dauList = await env.STATS_KV.list({ prefix: `dau:${today}:` });
        liveUsers = liveList.keys.length;
        todayUsers = dauList.keys.length;
        totalConns = parseInt(await env.STATS_KV.get('metric:total_connections')) || 0;
      }

      return new Response(JSON.stringify({
        liveUsers,
        todayUsers,
        totalConnections: totalConns,
        timestamp: Date.now()
      }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // 3. Fallback / Health Check
    return new Response(JSON.stringify({
      app: 'ZeroTrace Telemetry Engine',
      status: 'online',
      version: '1.0.6'
    }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  }
};
