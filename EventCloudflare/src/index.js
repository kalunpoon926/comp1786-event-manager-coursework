const cors = { 'access-control-allow-origin': '*', 'access-control-allow-methods': 'GET, PUT, OPTIONS', 'access-control-allow-headers': 'Authorization, Content-Type', 'cache-control': 'no-store' };
const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { ...cors, 'content-type': 'application/json; charset=utf-8' } });
const bad = message => json({ error: message }, 400);
function field(value, name, required = true) {
  if (typeof value !== 'string' || value.length > 2000 || (required && !value.trim())) throw new Error(`Invalid ${name}`);
}
function validDate(value) {
  if (!/^\d{2}\/\d{2}\/\d{4}$/.test(value)) return false;
  const [day, month, year] = value.split('/').map(Number);
  const date = new Date(0); date.setUTCHours(0, 0, 0, 0); date.setUTCFullYear(year, month - 1, day);
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}
function validate(body) {
  if (!body || typeof body !== 'object' || !/^[a-f0-9-]{36}$/i.test(body.deviceId || '') || !Array.isArray(body.events) || !Array.isArray(body.sessions)) throw new Error('Invalid snapshot');
  if (body.events.length > 500 || body.sessions.length > 5000) throw new Error('Snapshot too large');
  const eventIds = new Set(), names = new Set(), sessionIds = new Set();
  for (const e of body.events) {
    if (!Number.isSafeInteger(e.id) || e.id < 1 || eventIds.has(e.id)) throw new Error('Invalid event ID');
    eventIds.add(e.id);
    for (const key of ['name','date','location','organiser','type','status']) field(e[key], key);
    for (const key of ['description','contactEmail','contactPhone']) field(e[key], key, false);
    if (!validDate(e.date)) throw new Error('Invalid event date');
    if (!['Upcoming','Completed','Cancelled'].includes(e.status)) throw new Error('Invalid event status');
    const unique = `${e.name.toLowerCase()}\0${e.date}`;
    if (names.has(unique)) throw new Error('Duplicate event name and date');
    names.add(unique);
  }
  for (const s of body.sessions) {
    if (!Number.isSafeInteger(s.id) || s.id < 1 || sessionIds.has(s.id) || !eventIds.has(s.eventId)) throw new Error('Invalid session relationship');
    sessionIds.add(s.id);
    for (const key of ['name','time','type']) field(s[key], key);
    for (const key of ['speaker','room','description']) field(s[key], key, false);
    if (!['Workshop','Presentation','Panel Discussion','Networking','Keynote','Others'].includes(s.type)) throw new Error('Invalid session type');
  }
}
const escapeLike = s => s.replaceAll('\\','\\\\').replaceAll('%','\\%').replaceAll('_','\\_');
const dateKey = e => e.date.slice(6) + e.date.slice(3,5) + e.date.slice(0,2);
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });
    if (request.method === 'GET' && url.pathname === '/health') {
      try { await env.DB.prepare('SELECT 1').first(); return json({ ok: true }); }
      catch { return json({ ok: false }, 503); }
    }
    if (request.method === 'PUT' && url.pathname === '/api/snapshot') {
      if (!env.ADMIN_TOKEN || request.headers.get('authorization') !== `Bearer ${env.ADMIN_TOKEN}`) return json({ error: 'Unauthorized' }, 401);
      let body;
      try {
        const raw = await request.text();
        if (raw.length > 1_000_000) return bad('Snapshot too large');
        body = JSON.parse(raw); validate(body);
      } catch (e) { return bad(e.message || 'Invalid request'); }
      try {
        await env.DB.batch([
          env.DB.prepare('DELETE FROM events WHERE device_id=?').bind(body.deviceId),
          env.DB.prepare(`INSERT INTO events(device_id,id,name,date,location,organiser,type,status,description,contact_email,contact_phone)
            SELECT ?, CAST(json_extract(value,'$.id') AS INTEGER), json_extract(value,'$.name'), json_extract(value,'$.date'), json_extract(value,'$.location'), json_extract(value,'$.organiser'), json_extract(value,'$.type'), json_extract(value,'$.status'), json_extract(value,'$.description'), json_extract(value,'$.contactEmail'), json_extract(value,'$.contactPhone') FROM json_each(?)`).bind(body.deviceId, JSON.stringify(body.events)),
          env.DB.prepare(`INSERT INTO sessions(device_id,id,event_id,name,time,speaker,room,type,description)
            SELECT ?, CAST(json_extract(value,'$.id') AS INTEGER), CAST(json_extract(value,'$.eventId') AS INTEGER), json_extract(value,'$.name'), json_extract(value,'$.time'), json_extract(value,'$.speaker'), json_extract(value,'$.room'), json_extract(value,'$.type'), json_extract(value,'$.description') FROM json_each(?)`).bind(body.deviceId, JSON.stringify(body.sessions))
        ]);
        return json({ ok: true, events: body.events.length, sessions: body.sessions.length });
      } catch (e) { return json({ error: 'Upload failed', detail: e.message }, 500); }
    }
    if (request.method === 'GET' && url.pathname === '/api/events') {
      const name = (url.searchParams.get('name') || '').trim();
      const date = (url.searchParams.get('date') || '').trim();
      if (name.length > 100 || (date && !validDate(date))) return bad('Invalid search');
      try {
        const where = `e.status='Upcoming' AND e.name LIKE ? ESCAPE '\\' AND (?='' OR e.date=?)`;
        const args = [escapeLike(name) + '%', date, date];
        const eventRows = (await env.DB.prepare(`SELECT e.device_id,e.id,e.name,e.date,e.location,e.organiser,e.type,e.status,e.description FROM events e WHERE ${where} LIMIT 500`).bind(...args).all()).results;
        const sessionRows = (await env.DB.prepare(`SELECT s.device_id,s.id,s.event_id,s.name,s.time,s.speaker,s.room,s.type,s.description FROM sessions s JOIN events e ON e.device_id=s.device_id AND e.id=s.event_id WHERE ${where} ORDER BY s.time,s.id LIMIT 5000`).bind(...args).all()).results;
        const sessions = new Map();
        for (const s of sessionRows) {
          const key = `${s.device_id}:${s.event_id}`;
          if (!sessions.has(key)) sessions.set(key, []);
          sessions.get(key).push({ id: `${s.device_id}:${s.id}`, name: s.name, time: s.time, speaker: s.speaker, room: s.room, type: s.type, description: s.description });
        }
        const events = eventRows.map(e => ({ id: `${e.device_id}:${e.id}`, name: e.name, date: e.date, location: e.location, organiser: e.organiser, type: e.type, status: e.status, description: e.description, sessions: sessions.get(`${e.device_id}:${e.id}`) || [] }));
        events.sort((a,b) => dateKey(a).localeCompare(dateKey(b)) || a.name.localeCompare(b.name));
        return json({ events });
      } catch (e) { return json({ error: 'Read failed', detail: e.message }, 500); }
    }
    return json({ error: 'Not found' }, 404);
  }
};
