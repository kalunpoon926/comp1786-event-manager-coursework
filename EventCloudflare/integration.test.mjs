import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const base = process.env.CLOUD_TEST_URL;
let token = process.env.TEST_ADMIN_TOKEN;
if (!token) { try { token = (await readFile('.dev.vars', 'utf8')).match(/^ADMIN_TOKEN=(.+)$/m)?.[1]; } catch {} }
const enabled = Boolean(base && token);
const deviceId = '123e4567-e89b-42d3-a456-426614174000';
const event = { id: 1, name: 'Coursework Test Event', date: '25/12/2026', location: 'Hong Kong', organiser: 'COMP1786', type: 'Conference', status: 'Upcoming', description: 'Demo', contactEmail: 'test@example.com', contactPhone: '' };
const session = { id: 1, eventId: 1, name: 'Opening', time: '09:00', speaker: '', room: '', type: 'Keynote', description: '' };
const put = body => fetch(base + '/api/snapshot', { method: 'PUT', headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' }, body: JSON.stringify(body) });
test('cloud snapshot, search, cascade, and privacy', { skip: !enabled }, async () => {
  assert.equal((await fetch(base + '/health')).status, 200);
  assert.equal((await fetch(base + '/api/snapshot', { method: 'PUT', body: '{}' })).status, 401);
  const snapshot = { deviceId, events: [event], sessions: [session] };
  try {
    assert.equal((await put(snapshot)).status, 200);
    const result = await (await fetch(base + '/api/events?name=Course&date=25%2F12%2F2026')).json();
    assert.equal(result.events.length, 1);
    assert.equal(result.events[0].sessions.length, 1);
    assert.equal('contactEmail' in result.events[0], false);
    assert.equal((await put({ ...snapshot, events: [] })).status, 400);
    const still = await (await fetch(base + '/api/events')).json();
    assert.equal(still.events.length, 1);
  } finally {
    assert.equal((await put({ deviceId, events: [], sessions: [] })).status, 200);
    const empty = await (await fetch(base + '/api/events?name=Course')).json();
    assert.equal(empty.events.length, 0);
  }
});
