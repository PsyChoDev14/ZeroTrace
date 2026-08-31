const { recordHeartbeat } = require('../lib/store');

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, User-Agent, Authorization, x-admin-key');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
    const {
      clientId,
      version,
      protocol,
      activeApps,
      event,
      configRemark,
      serverAddress,
      deviceModel,
      androidVersion,
      durationSeconds,
      downloadSpeed,
      uploadSpeed
    } = body;

    await recordHeartbeat(clientId, version, protocol, activeApps, event, {
      configRemark,
      serverAddress,
      deviceModel,
      androidVersion,
      durationSeconds,
      downloadSpeed,
      uploadSpeed
    });

    return res.status(200).json({ status: 'ok', timestamp: Date.now() });
  } catch (e) {
    return res.status(400).json({ status: 'error', message: e.message });
  }
};
