const fs = require('fs');
const path = require('path');

// In-memory runtime cache for serverless instance
global._zerotrace_store = global._zerotrace_store || {
  liveDevices: new Map(), // clientId -> { version, protocol, activeApps, ts }
  dailyUsers: new Set(),
  totalConnections: 0,
  lastDailyReset: new Date().toISOString().slice(0, 10)
};

const store = global._zerotrace_store;

function cleanupStaleDevices() {
  const now = Date.now();
  const today = new Date().toISOString().slice(0, 10);
  
  if (store.lastDailyReset !== today) {
    store.dailyUsers.clear();
    store.lastDailyReset = today;
  }

  // Remove devices that haven't sent a heartbeat in the last 3 minutes (180s)
  for (const [clientId, data] of store.liveDevices.entries()) {
    if (now - data.ts > 180 * 1000) {
      store.liveDevices.delete(clientId);
    }
  }
}

function recordHeartbeat(clientId, version, protocol, activeApps, event) {
  cleanupStaleDevices();
  const now = Date.now();
  
  if (clientId) {
    store.dailyUsers.add(clientId);
    store.liveDevices.set(clientId, {
      version: version || '1.0.8',
      protocol: protocol || 'vless',
      activeApps: Array.isArray(activeApps) ? activeApps : [],
      ts: now
    });

    if (event === 'vpn_connected') {
      store.totalConnections++;
    }
  }
}

function getStats() {
  cleanupStaleDevices();

  const liveAppsMap = {};
  const protocolMap = {};
  const versionMap = {};

  for (const [_, device] of store.liveDevices.entries()) {
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
    liveUsers: store.liveDevices.size,
    todayUsers: store.dailyUsers.size,
    totalConnections: store.totalConnections,
    protocols: protocolMap,
    versions: versionMap,
    liveConnectedApps,
    timestamp: Date.now()
  };
}

module.exports = {
  recordHeartbeat,
  getStats
};
