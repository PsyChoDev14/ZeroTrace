const { setForceUpdate } = require('../lib/store');

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, User-Agent, Authorization, x-admin-key');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const expectedSecret = process.env.ADMIN_SECRET || 'zerotrace_admin_secret_2026';
  const authHeader = req.headers['x-admin-key'] || req.headers['authorization'] || req.query.key;

  if (!authHeader || (authHeader !== expectedSecret && authHeader !== `Bearer ${expectedSecret}`)) {
    return res.status(401).json({
      error: 'Unauthorized',
      message: 'Access Denied'
    });
  }

  if (req.method === 'POST') {
    try {
      const body = typeof req.body === 'string' ? JSON.parse(req.body) : (req.body || {});
      const { enabled, minVersion } = body;
      const result = await setForceUpdate(enabled, minVersion);
      return res.status(200).json({ status: 'ok', ...result });
    } catch (e) {
      return res.status(400).json({ error: e.message });
    }
  }

  return res.status(405).json({ error: 'Method not allowed' });
};
