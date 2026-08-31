/**
 * ZeroTrace VPN - Telemetry & Live App Analytics Cloudflare Worker
 * 100% Free Serverless Endpoint with Private Authenticated Admin Stats & Live App Monitor.
 *
 * Deploy to Cloudflare Workers:
 * 1. Go to dash.cloudflare.com -> Workers & Pages -> Create Worker
 * 2. Paste this code into the editor
 * 3. In Settings -> Variables, add an Environment Variable or Secret:
 *    ADMIN_SECRET = "zerotrace_admin_secret_2026"
 * 4. Bind a KV namespace named 'STATS_KV' for persistent counts & live app tracking.
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

    const expectedSecret = env.ADMIN_SECRET || 'zerotrace_admin_secret_2026';

    // 1. Heartbeat Ping from ZeroTrace Android App (with Active Connected Apps)
    if (url.pathname === '/api/heartbeat' && request.method === 'POST') {
      try {
        const body = await request.json();
        const { clientId, version, event, protocol, activeApps } = body;
        const now = Date.now();
        const today = new Date().toISOString().slice(0, 10);

        if (env.STATS_KV && clientId) {
          // Record live active device session (TTL: 180 seconds / 3 mins)
          await env.STATS_KV.put(`live:${clientId}`, JSON.stringify({
            version: version || '1.0.7',
            protocol: protocol || 'vless',
            activeApps: Array.isArray(activeApps) ? activeApps : [],
            ts: now
          }), { expirationTtl: 180 });

          // Track Daily Active User (TTL: 30 days)
          await env.STATS_KV.put(`dau:${today}:${clientId}`, '1', { expirationTtl: 86400 * 30 });

          // Increment connection metric
          if (event === 'vpn_connected') {
            const totalConns = (parseInt(await env.STATS_KV.get('metric:total_connections')) || 0) + 1;
            await env.STATS_KV.put('metric:total_connections', totalConns.toString());
          }
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

      // Verify Admin Authentication
      if (!authHeader || (authHeader !== expectedSecret && authHeader !== `Bearer ${expectedSecret}`)) {
        return new Response(JSON.stringify({
          error: 'Unauthorized',
          message: 'Access Denied: Private Admin Telemetry'
        }), {
          status: 401,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        });
      }

      let liveUsers = 0;
      let todayUsers = 0;
      let totalConns = 0;
      const liveAppsMap = {};
      const protocolMap = {};
      const versionMap = {};

      if (env.STATS_KV) {
        const today = new Date().toISOString().slice(0, 10);
        const liveList = await env.STATS_KV.list({ prefix: 'live:' });
        const dauList = await env.STATS_KV.list({ prefix: `dau:${today}:` });

        liveUsers = liveList.keys.length;
        todayUsers = dauList.keys.length;
        totalConns = parseInt(await env.STATS_KV.get('metric:total_connections')) || 0;

        // Fetch each live device's active apps & protocols
        for (const key of liveList.keys) {
          const raw = await env.STATS_KV.get(key.name);
          if (raw) {
            try {
              const data = JSON.parse(raw);
              if (data.protocol) {
                protocolMap[data.protocol] = (protocolMap[data.protocol] || 0) + 1;
              }
              if (data.version) {
                versionMap[data.version] = (versionMap[data.version] || 0) + 1;
              }
              if (Array.isArray(data.activeApps)) {
                for (const app of data.activeApps) {
                  liveAppsMap[app] = (liveAppsMap[app] || 0) + 1;
                }
              }
            } catch (e) {}
          }
        }
      }

      // Sort live connected apps by active devices count descending
      const liveConnectedApps = Object.entries(liveAppsMap)
        .map(([name, count]) => ({ name, count }))
        .sort((a, b) => b.count - a.count);

      return new Response(JSON.stringify({
        liveUsers,
        todayUsers,
        totalConnections: totalConns,
        protocols: protocolMap,
        versions: versionMap,
        liveConnectedApps,
        timestamp: Date.now()
      }, null, 2), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    // 3. Private Web Admin Live Dashboard
    if (url.pathname === '/admin' || url.pathname === '/dashboard') {
      const key = url.searchParams.get('key');
      if (key !== expectedSecret) {
        return new Response(`<!DOCTYPE html>
<html>
<head><title>ZeroTrace Admin - Access Denied</title>
<style>body{background:#0b0f19;color:#ff453a;font-family:sans-serif;display:flex;justify-content:center;align-items:center;height:100vh;margin:0;}
.card{background:#161d2f;border:1px solid #2a3656;padding:32px;border-radius:16px;text-align:center;box-shadow:0 10px 30px rgba(0,0,0,0.5);}
input{background:#0b0f19;border:1px solid #2a3656;color:#fff;padding:10px 16px;border-radius:8px;margin-top:16px;width:80%;outline:none;}
button{background:#00d084;color:#000;border:none;padding:10px 20px;border-radius:8px;font-weight:bold;margin-top:12px;cursor:pointer;}
</style></head>
<body>
<div class="card">
  <h2>🛡️ ZeroTrace Admin Portal</h2>
  <p style="color:#8e9aaf;">Enter Secret Admin Key to view live telemetry & connected apps:</p>
  <form method="GET">
    <input type="password" name="key" placeholder="Enter Admin Secret..." required><br>
    <button type="submit">Unlock Dashboard</button>
  </form>
</div>
</body></html>`, { headers: { 'Content-Type': 'text/html; charset=utf-8' } });
      }

      // Return Live HTML Dashboard
      return new Response(`<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>ZeroTrace VPN • Live Telemetry & Connected Apps</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { background: #07090e; color: #f3f4f6; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 24px; }
    .container { max-width: 900px; margin: 0 auto; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #1f293d; }
    .title { display: flex; align-items: center; gap: 12px; font-size: 20px; font-weight: 700; color: #35c77b; }
    .badge { background: rgba(53, 199, 123, 0.15); color: #35c77b; padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 700; border: 1px solid rgba(53, 199, 123, 0.3); }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .card { background: #0f1422; border: 1px solid #1f293d; border-radius: 16px; padding: 18px; position: relative; overflow: hidden; }
    .card-label { font-size: 11px; font-weight: 700; color: #6b7280; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 8px; }
    .card-value { font-size: 32px; font-weight: 800; font-family: monospace; color: #ffffff; }
    .section-title { font-size: 14px; font-weight: 700; color: #9ca3af; letter-spacing: 0.8px; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
    .app-table { width: 100%; border-collapse: collapse; background: #0f1422; border: 1px solid #1f293d; border-radius: 16px; overflow: hidden; margin-bottom: 24px; }
    .app-table th { background: #151c2e; padding: 12px 16px; text-align: left; font-size: 11px; color: #9ca3af; text-transform: uppercase; }
    .app-table td { padding: 12px 16px; border-top: 1px solid #1a2236; font-size: 13px; }
    .app-bar { height: 6px; background: #35c77b; border-radius: 3px; margin-top: 4px; }
    .live-dot { width: 8px; height: 8px; background: #35c77b; border-radius: 50%; display: inline-block; animation: pulse 1.5s infinite; }
    @keyframes pulse { 0% { opacity: 0.4; } 50% { opacity: 1; } 100% { opacity: 0.4; } }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="title">
        <span class="live-dot"></span>
        <span>ZeroTrace VPN Admin Telemetry</span>
      </div>
      <div class="badge">PRIVATE DEVELOPER VIEW</div>
    </div>

    <div class="grid">
      <div class="card">
        <div class="card-label">🟢 Live Connected Devices</div>
        <div class="card-value" id="val-live" style="color:#35c77b;">--</div>
      </div>
      <div class="card">
        <div class="card-label">📅 Daily Active Users (DAU)</div>
        <div class="card-value" id="val-dau">--</div>
      </div>
      <div class="card">
        <div class="card-label">⚡ Total Tunnel Connections</div>
        <div class="card-value" id="val-total">--</div>
      </div>
    </div>

    <div class="section-title">
      <span>📱 LIVE RUNNING APPS CONNECTED ACROSS DEVICES</span>
    </div>

    <table class="app-table">
      <thead>
        <tr>
          <th>Application Name</th>
          <th>Active Devices Routing Traffic</th>
          <th style="width: 35%;">Bandwidth Share</th>
        </tr>
      </thead>
      <tbody id="live-apps-body">
        <tr><td colspan="3" style="text-align:center; color:#6b7280; padding:24px;">Loading live connected applications...</td></tr>
      </tbody>
    </table>

    <div style="text-align:center; color:#4b5563; font-size:11px; margin-top:20px;">
      ZeroTrace Private Telemetry Engine • Auto-refreshing every 5 seconds
    </div>
  </div>

  <script>
    async function updateStats() {
      try {
        const res = await fetch('/api/stats?key=${expectedSecret}');
        const data = await res.json();
        
        document.getElementById('val-live').innerText = data.liveUsers || 0;
        document.getElementById('val-dau').innerText = data.todayUsers || 0;
        document.getElementById('val-total').innerText = data.totalConnections || 0;

        const tbody = document.getElementById('live-apps-body');
        if (data.liveConnectedApps && data.liveConnectedApps.length > 0) {
          const maxCount = Math.max(...data.liveConnectedApps.map(a => a.count), 1);
          tbody.innerHTML = data.liveConnectedApps.map(app => {
            const pct = Math.round((app.count / maxCount) * 100);
            return '<tr>' +
              '<td style="font-weight:600; color:#fff;">' + app.name + '</td>' +
              '<td><span style="color:#35c77b; font-weight:700; font-family:monospace;">' + app.count + ' active</span></td>' +
              '<td><div style="display:flex; align-items:center; gap:8px;">' +
              '<div style="flex:1; background:#1a2236; height:6px; border-radius:3px; overflow:hidden;">' +
              '<div class="app-bar" style="width:' + pct + '%;"></div></div>' +
              '<span style="font-size:10px; color:#9ca3af; font-family:monospace;">' + pct + '%</span></div></td>' +
              '</tr>';
          }).join('');
        } else {
          tbody.innerHTML = '<tr><td colspan="3" style="text-align:center; color:#6b7280; padding:24px;">No app traffic reported right now. When users connect with apps, they will appear here live!</td></tr>';
        }
      } catch (e) {
        console.error(e);
      }
    }
    updateStats();
    setInterval(updateStats, 5000);
  </script>
</body>
</html>`, { headers: { 'Content-Type': 'text/html; charset=utf-8' } });
    }

    // 4. Fallback / Health Check
    return new Response(JSON.stringify({
      app: 'ZeroTrace Telemetry Engine',
      status: 'online',
      version: '1.0.7'
    }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  }
};
