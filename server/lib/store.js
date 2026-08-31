const https = require('https');

const GIST_ID = process.env.GIST_ID || 'c3b734cf5cad834a732b4dfb1584b449';
const GITHUB_TOKEN = process.env.GITHUB_TOKEN || (function() {
  try { return require('fs').readFileSync(require('path').join(__dirname, '../.github_token'), 'utf8').trim(); } catch(e) { return ''; }
})();

// In-memory runtime cache
global._zt_store = global._zt_store || {
  liveDevices: {}, // clientId -> { clientId, deviceModel, androidVersion, configRemark, serverAddress, version, protocol, durationSeconds, downloadSpeed, uploadSpeed, activeApps, ts }
  dailyUsers: [],
  totalConnections: 0,
  forceUpdateGlobal: false,
  minRequiredVersion: "1.0.8",
  lastDailyReset: new Date().toISOString().slice(0, 10),
  lastGistSync: 0
};

const store = global._zt_store;

function githubRequest(path, method, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const req = https.request({
      hostname: 'api.github.com',
      path: path,
      method: method,
      headers: {
        'User-Agent': 'ZeroTrace-Telemetry-Server',
        'Authorization': `token ${GITHUB_TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
        ...(data ? { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } : {})
      },
      timeout: 5000
    }, (res) => {
      let raw = '';
      res.on('data', chunk => raw += chunk);
      res.on('end', () => {
        try {
          resolve(raw ? JSON.parse(raw) : {});
        } catch (e) {
          resolve({});
        }
      });
    });

    req.on('error', () => resolve({}));
    req.on('timeout', () => { req.destroy(); resolve({}); });
    if (data) req.write(data);
    req.end();
  });
}

async function loadFromGist() {
  try {
    const gist = await githubRequest(`/gists/${GIST_ID}`, 'GET');
    const file = gist?.files?.['telemetry.json'];
    if (file?.content) {
      const data = JSON.parse(file.content);
      store.liveDevices = data.liveDevices || {};
      store.dailyUsers = Array.isArray(data.dailyUsers) ? data.dailyUsers : [];
      store.totalConnections = data.totalConnections || 0;
      store.forceUpdateGlobal = !!data.forceUpdateGlobal;
      store.minRequiredVersion = data.minRequiredVersion || "1.0.8";
      store.lastDailyReset = data.lastDailyReset || new Date().toISOString().slice(0, 10);
      store.lastGistSync = Date.now();
    }
  } catch (e) {}
}

async function saveToGist() {
  try {
    const payload = {
      files: {
        'telemetry.json': {
          content: JSON.stringify({
            liveDevices: store.liveDevices,
            dailyUsers: store.dailyUsers,
            totalConnections: store.totalConnections,
            forceUpdateGlobal: store.forceUpdateGlobal,
            minRequiredVersion: store.minRequiredVersion,
            lastDailyReset: store.lastDailyReset
          })
        }
      }
    };
    await githubRequest(`/gists/${GIST_ID}`, 'PATCH', payload);
  } catch (e) {}
}

function cleanupStale() {
  const now = Date.now();
  const today = new Date().toISOString().slice(0, 10);

  if (store.lastDailyReset !== today) {
    store.dailyUsers = [];
    store.lastDailyReset = today;
  }

  // Active TTL: 3 minutes (180s)
  for (const clientId in store.liveDevices) {
    if (now - (store.liveDevices[clientId].ts || 0) > 180 * 1000) {
      delete store.liveDevices[clientId];
    }
  }
}

async function recordHeartbeat(clientId, version, protocol, activeApps, event, extra = {}) {
  if (Date.now() - store.lastGistSync > 5000) {
    await loadFromGist();
  }

  cleanupStale();
  const now = Date.now();

  if (clientId) {
    if (!store.dailyUsers.includes(clientId)) {
      store.dailyUsers.push(clientId);
    }

    const prev = store.liveDevices[clientId] || {};

    store.liveDevices[clientId] = {
      clientId,
      version: version || prev.version || '1.0.8',
      protocol: protocol || prev.protocol || 'vless',
      configRemark: extra.configRemark || prev.configRemark || 'ZeroTrace Direct',
      serverAddress: extra.serverAddress || prev.serverAddress || '',
      deviceModel: extra.deviceModel || prev.deviceModel || 'Android Device',
      androidVersion: extra.androidVersion || prev.androidVersion || 'Android',
      durationSeconds: extra.durationSeconds || prev.durationSeconds || 0,
      downloadSpeed: extra.downloadSpeed || 0,
      uploadSpeed: extra.uploadSpeed || 0,
      activeApps: Array.isArray(activeApps) ? activeApps : (prev.activeApps || []),
      ts: now,
      firstSeen: prev.firstSeen || now
    };

    if (event === 'vpn_connected') {
      store.totalConnections++;
    }
  }

  await saveToGist();
}

async function setForceUpdate(enabled, minVersion) {
  await loadFromGist();
  store.forceUpdateGlobal = !!enabled;
  if (minVersion) store.minRequiredVersion = minVersion;
  await saveToGist();
  return { forceUpdateGlobal: store.forceUpdateGlobal, minRequiredVersion: store.minRequiredVersion };
}

async function getStats() {
  await loadFromGist();
  cleanupStale();

  const liveAppsMap = {};
  const protocolMap = {};
  const versionMap = {};

  const devicesList = Object.values(store.liveDevices).map(dev => ({
    ...dev,
    onlineAgoSeconds: Math.round((Date.now() - (dev.ts || Date.now())) / 1000)
  }));

  for (const device of devicesList) {
    if (device.protocol) {
      protocolMap[device.protocol] = (protocolMap[device.protocol] || 0) + 1;
    }
    if (device.version) {
      versionMap[device.version] = (versionMap[device.version] || 0) + 1;
    }
    if (Array.isArray(device.activeApps)) {
      for (const app of device.activeApps) {
        liveAppsMap[app] = (liveAppsMap[app] || 0) + 1;
      }
    }
  }

  const liveConnectedApps = Object.entries(liveAppsMap)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count);

  return {
    liveUsers: devicesList.length,
    todayUsers: store.dailyUsers.length,
    totalConnections: store.totalConnections,
    forceUpdateGlobal: store.forceUpdateGlobal,
    minRequiredVersion: store.minRequiredVersion,
    protocols: protocolMap,
    versions: versionMap,
    devices: devicesList,
    liveConnectedApps,
    timestamp: Date.now()
  };
}

module.exports = {
  recordHeartbeat,
  setForceUpdate,
  getStats
};
