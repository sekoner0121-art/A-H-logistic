'use strict';

const encoder = new TextEncoder();
const SESSION_SECONDS = 12 * 60 * 60;
const ACCOUNT_SEED = [
  { id: 'control-1', username: 'control1', displayName: 'Logística', role: 'control' },
  { id: 'driver-1', username: 'conductor1', displayName: 'Conductor 1', role: 'driver' },
  { id: 'driver-2', username: 'conductor2', displayName: 'Conductor 2', role: 'driver' },
];

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'Permissions-Policy': 'geolocation=(self)',
      'Content-Security-Policy': "default-src 'self'; script-src 'self' https://unpkg.com; style-src 'self' 'unsafe-inline' https://unpkg.com; img-src 'self' data: https://*.tile.openstreetmap.org; connect-src 'self'; font-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'",
      ...headers,
    },
  });
}

function safeAsset(response) {
  const headers = new Headers(response.headers);
  headers.set('X-Content-Type-Options', 'nosniff');
  headers.set('X-Frame-Options', 'DENY');
  headers.set('Referrer-Policy', 'strict-origin-when-cross-origin');
  headers.set('Permissions-Policy', 'geolocation=(self)');
  headers.set('Content-Security-Policy', "default-src 'self'; script-src 'self' https://unpkg.com; style-src 'self' 'unsafe-inline' https://unpkg.com; img-src 'self' data: https://*.tile.openstreetmap.org; connect-src 'self'; font-src 'self' data:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'");
  return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
}

async function readJson(request) {
  const raw = await request.text();
  if (raw.length > 16_384) throw Object.assign(new Error('La solicitud es demasiado grande.'), { status: 413 });
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch {
    throw Object.assign(new Error('JSON inválido.'), { status: 400 });
  }
}

function text(value, limit) {
  return typeof value === 'string' ? value.trim().slice(0, limit) : '';
}

function cookie(request, name) {
  const prefix = name + '=';
  for (const part of (request.headers.get('Cookie') || '').split(';')) {
    const item = part.trim();
    if (item.startsWith(prefix)) {
      try { return decodeURIComponent(item.slice(prefix.length)); } catch { return ''; }
    }
  }
  return '';
}

function bytesToBase64Url(bytes) {
  let binary = '';
  for (const byte of new Uint8Array(bytes)) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '');
}

function base64UrlToBytes(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(normalized + '='.repeat((4 - normalized.length % 4) % 4));
  return Uint8Array.from(binary, char => char.charCodeAt(0));
}

async function hmacKey(secret) {
  return crypto.subtle.importKey('raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign', 'verify']);
}

async function signToken(user, env) {
  const payload = bytesToBase64Url(encoder.encode(JSON.stringify({
    sub: user.id,
    exp: Math.floor(Date.now() / 1000) + SESSION_SECONDS,
    v: Number(user.authVersion || 1),
  })));
  const signature = await crypto.subtle.sign('HMAC', await hmacKey(env.SESSION_SECRET), encoder.encode(payload));
  return payload + '.' + bytesToBase64Url(signature);
}

async function currentUser(request, env) {
  const authorization = request.headers.get('Authorization') || '';
  const token = authorization.startsWith('Bearer ') ? authorization.slice(7) : cookie(request, 'ah_session');
  const parts = token.split('.');
  if (parts.length !== 2 || !env.SESSION_SECRET) return null;
  try {
    const valid = await crypto.subtle.verify('HMAC', await hmacKey(env.SESSION_SECRET), base64UrlToBytes(parts[1]), encoder.encode(parts[0]));
    if (!valid) return null;
    const payload = JSON.parse(new TextDecoder().decode(base64UrlToBytes(parts[0])));
    if (!payload.sub || payload.exp <= Math.floor(Date.now() / 1000)) return null;
    const user = await env.DB.prepare(
      'SELECT id, username, display_name AS displayName, role, active, auth_version AS authVersion FROM users WHERE id = ? AND active = 1'
    ).bind(payload.sub).first();
    if (!user || Number(payload.v || 1) !== Number(user.authVersion || 1)) return null;
    return user;
  } catch {
    return null;
  }
}

async function passwordHash(password, salt, env) {
  const key = await hmacKey(env.PASSWORD_PEPPER);
  const result = await crypto.subtle.sign('HMAC', key, encoder.encode(salt + ':' + password));
  return bytesToBase64Url(result);
}

function equalText(a, b) {
  const left = encoder.encode(String(a));
  const right = encoder.encode(String(b));
  let mismatch = left.length ^ right.length;
  const length = Math.max(left.length, right.length);
  for (let i = 0; i < length; i++) mismatch |= (left[i] || 0) ^ (right[i] || 0);
  return mismatch === 0;
}

function randomSalt() {
  return bytesToBase64Url(crypto.getRandomValues(new Uint8Array(18)));
}

function publicUser(user) {
  return {
    id: user.id,
    username: user.username,
    displayName: user.displayName || user.display_name,
    role: user.role,
    mustChangePassword: false,
  };
}

async function getOrders(env, where = '', values = []) {
  const result = await env.DB.prepare(
    'SELECT o.id, o.title, o.origin, o.destination, o.notes, o.driver_id AS driverId, ' +
    'u.display_name AS driverName, o.status, o.created_at AS createdAt, o.updated_at AS updatedAt ' +
    'FROM orders o JOIN users u ON u.id = o.driver_id ' + where +
    ' ORDER BY o.updated_at DESC LIMIT 100'
  ).bind(...values).all();
  return result.results || [];
}

async function recordLoginFailure(request, env) {
  const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
  const now = Math.floor(Date.now() / 1000);
  const existing = await env.DB.prepare('SELECT failures, window_started_at AS windowStartedAt, blocked_until AS blockedUntil FROM login_attempts WHERE ip = ?').bind(ip).first();
  const activeWindow = existing && now - existing.windowStartedAt < 900;
  const failures = activeWindow ? existing.failures + 1 : 1;
  const windowStartedAt = activeWindow ? existing.windowStartedAt : now;
  const blockedUntil = failures >= 8 ? now + 900 : 0;
  await env.DB.prepare(
    'INSERT INTO login_attempts (ip, failures, window_started_at, blocked_until) VALUES (?, ?, ?, ?) ' +
    'ON CONFLICT(ip) DO UPDATE SET failures=excluded.failures, window_started_at=excluded.window_started_at, blocked_until=excluded.blocked_until'
  ).bind(ip, failures, windowStartedAt, blockedUntil).run();
}

async function handleApi(request, env) {
  const url = new URL(request.url);
  const pathname = url.pathname;
  const method = request.method;

  if (pathname === '/api/me' && method === 'GET') {
    const accountCount = await env.DB.prepare('SELECT COUNT(*) AS count FROM users').first();
    if (!accountCount || accountCount.count === 0) return json({ user: null, setupRequired: true });
    const user = await currentUser(request, env);
    return json({ user: user ? publicUser(user) : null, setupRequired: false });
  }

  if (pathname === '/api/setup' && method === 'POST') {
    if (!env.SETUP_CODE || !env.PASSWORD_PEPPER || !env.SESSION_SECRET) {
      return json({ error: 'Faltan secretos de configuración del servidor.' }, 503);
    }
    const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
    const now = Math.floor(Date.now() / 1000);
    const attempts = await env.DB.prepare('SELECT blocked_until AS blockedUntil FROM login_attempts WHERE ip = ?').bind(ip).first();
    if (attempts && attempts.blockedUntil > now) return json({ error: 'Demasiados intentos. Espera 15 minutos.' }, 429);
    const body = await readJson(request);
    if (!equalText(body.setupCode, env.SETUP_CODE)) {
      await recordLoginFailure(request, env);
      return json({ error: 'Código de instalación incorrecto.' }, 401);
    }
    const count = await env.DB.prepare('SELECT COUNT(*) AS count FROM users').first();
    if (count && count.count > 0) return json({ error: 'Las cuentas ya están configuradas.' }, 409);
    const passwords = [
      text(body.controlPassword, 128),
      text(body.driver1Password, 128),
      text(body.driver2Password, 128),
    ];
    if (passwords.some(value => value.length < 12)) return json({ error: 'Cada contraseña debe tener al menos 12 caracteres.' }, 400);
    const statements = [];
    for (let index = 0; index < ACCOUNT_SEED.length; index++) {
      const account = ACCOUNT_SEED[index];
      const salt = randomSalt();
      const hash = await passwordHash(passwords[index], salt, env);
      statements.push(env.DB.prepare(
        'INSERT INTO users (id, username, display_name, role, password_salt, password_hash) VALUES (?, ?, ?, ?, ?, ?)'
      ).bind(account.id, account.username, account.displayName, account.role, salt, hash));
    }
    try {
      await env.DB.batch(statements);
    } catch {
      return json({ error: 'No se pudieron crear las cuentas. Recarga e inténtalo de nuevo.' }, 409);
    }
    await env.DB.prepare('DELETE FROM login_attempts WHERE ip = ?').bind(ip).run();
    return json({ ok: true });
  }

  if (pathname === '/api/login' && method === 'POST') {
    if (!env.PASSWORD_PEPPER || !env.SESSION_SECRET) return json({ error: 'El acceso aún no está configurado.' }, 503);
    const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
    const now = Math.floor(Date.now() / 1000);
    const attempts = await env.DB.prepare('SELECT blocked_until AS blockedUntil FROM login_attempts WHERE ip = ?').bind(ip).first();
    if (attempts && attempts.blockedUntil > now) return json({ error: 'Demasiados intentos. Espera 15 minutos.' }, 429);
    const body = await readJson(request);
    const username = text(body.username, 40).toLowerCase();
    const password = typeof body.password === 'string' ? body.password : '';
    const requestedRole = body.role === 'control' ? 'control' : body.role === 'driver' ? 'driver' : '';
    const user = await env.DB.prepare(
      'SELECT id, username, display_name AS displayName, role, password_salt AS passwordSalt, password_hash AS passwordHash, active, auth_version AS authVersion FROM users WHERE username = ?'
    ).bind(username).first();
    const calculated = user ? await passwordHash(password, user.passwordSalt, env) : '';
    if (!user || !user.active || user.role !== requestedRole || !equalText(calculated, user.passwordHash)) {
      await recordLoginFailure(request, env);
      return json({ error: 'Usuario o contraseña incorrectos.' }, 401);
    }
    await env.DB.prepare('DELETE FROM login_attempts WHERE ip = ?').bind(ip).run();
    const token = await signToken(user, env);
    return json({ user: publicUser(user) }, 200, {
      'Set-Cookie': 'ah_session=' + encodeURIComponent(token) + '; Path=/; HttpOnly; SameSite=Strict; Secure; Max-Age=' + SESSION_SECONDS,
    });
  }

  if (pathname === '/api/recover-initial-drivers' && method === 'POST') {
    if (!env.SETUP_CODE || !env.PASSWORD_PEPPER) return json({ error: 'La recuperación no está configurada.' }, 503);
    const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
    const now = Math.floor(Date.now() / 1000);
    const attempts = await env.DB.prepare('SELECT blocked_until AS blockedUntil FROM login_attempts WHERE ip = ?').bind(ip).first();
    if (attempts && attempts.blockedUntil > now) return json({ error: 'Demasiados intentos. Espera 15 minutos.' }, 429);
    const body = await readJson(request);
    if (!equalText(body.setupCode, env.SETUP_CODE)) {
      await recordLoginFailure(request, env);
      return json({ error: 'Código de recuperación incorrecto.' }, 401);
    }
    const alreadyUsed = await env.DB.prepare('SELECT recovery_id FROM password_recovery_events WHERE recovery_id = ?').bind('initial-driver-reset').first();
    if (alreadyUsed) return json({ error: 'El código de recuperación inicial ya fue utilizado.' }, 409);
    const passwords = [body.driver1Password, body.driver2Password];
    if (passwords.some(value => typeof value !== 'string' || value.length < 12 || value.length > 128)) {
      return json({ error: 'Cada contraseña debe tener entre 12 y 128 caracteres.' }, 400);
    }
    const drivers = await env.DB.prepare("SELECT id FROM users WHERE id IN ('driver-1', 'driver-2') AND role = 'driver' AND active = 1").all();
    if (!drivers.results || drivers.results.length !== 2) return json({ error: 'Las dos cuentas de conductor deben estar configuradas.' }, 409);
    const salts = [randomSalt(), randomSalt()];
    const hashes = await Promise.all(passwords.map((password, index) => passwordHash(password, salts[index], env)));
    try {
      await env.DB.batch([
        env.DB.prepare('INSERT INTO password_recovery_events (recovery_id, used_at) VALUES (?, ?)').bind('initial-driver-reset', new Date().toISOString()),
        env.DB.prepare("UPDATE users SET password_salt = ?, password_hash = ?, auth_version = auth_version + 1 WHERE id = 'driver-1' AND role = 'driver' AND active = 1").bind(salts[0], hashes[0]),
        env.DB.prepare("UPDATE users SET password_salt = ?, password_hash = ?, auth_version = auth_version + 1 WHERE id = 'driver-2' AND role = 'driver' AND active = 1").bind(salts[1], hashes[1]),
      ]);
    } catch {
      return json({ error: 'No se pudieron restablecer las cuentas. Recarga e inténtalo de nuevo.' }, 409);
    }
    await env.DB.prepare('DELETE FROM login_attempts WHERE ip = ?').bind(ip).run();
    return json({ ok: true });
  }

  if (pathname === '/api/logout' && method === 'POST') {
    const signedInUser = await currentUser(request, env);
    if (signedInUser && signedInUser.role === 'driver') {
      await env.DB.prepare('DELETE FROM driver_locations WHERE driver_id = ?').bind(signedInUser.id).run();
    }
    return json({ ok: true }, 200, {
      'Set-Cookie': 'ah_session=; Path=/; HttpOnly; SameSite=Strict; Secure; Max-Age=0',
    });
  }

  const user = await currentUser(request, env);
  if (!user) return json({ error: 'Inicia sesión para continuar.' }, 401);

  if (pathname === '/api/password' && method === 'POST') {
    if (!env.PASSWORD_PEPPER) return json({ error: 'El acceso aún no está configurado.' }, 503);
    const body = await readJson(request);
    const account = await env.DB.prepare('SELECT password_salt AS passwordSalt, password_hash AS passwordHash FROM users WHERE id = ?').bind(user.id).first();
    const oldHash = await passwordHash(String(body.currentPassword || ''), account.passwordSalt, env);
    const nextPassword = typeof body.newPassword === 'string' ? body.newPassword : '';
    if (!equalText(oldHash, account.passwordHash)) return json({ error: 'La contraseña actual no coincide.' }, 400);
    if (nextPassword.length < 12) return json({ error: 'La nueva contraseña debe tener al menos 12 caracteres.' }, 400);
    const salt = randomSalt();
    const hash = await passwordHash(nextPassword, salt, env);
    await env.DB.prepare('UPDATE users SET password_salt = ?, password_hash = ? WHERE id = ?').bind(salt, hash, user.id).run();
    return json({ ok: true });
  }

  const driverPasswordMatch = pathname.match(/^\/api\/drivers\/(driver-1|driver-2)\/password$/);
  if (driverPasswordMatch && method === 'POST') {
    if (user.role !== 'control') return json({ error: 'Solo la torre de control puede restablecer claves.' }, 403);
    if (!env.PASSWORD_PEPPER) return json({ error: 'El acceso aún no está configurado.' }, 503);
    const body = await readJson(request);
    const nextPassword = typeof body.newPassword === 'string' ? body.newPassword : '';
    if (nextPassword.length < 12 || nextPassword.length > 128) return json({ error: 'La nueva contraseña debe tener entre 12 y 128 caracteres.' }, 400);
    const driverId = driverPasswordMatch[1];
    const salt = randomSalt();
    const hash = await passwordHash(nextPassword, salt, env);
    const result = await env.DB.prepare("UPDATE users SET password_salt = ?, password_hash = ?, auth_version = auth_version + 1 WHERE id = ? AND role = 'driver' AND active = 1")
      .bind(salt, hash, driverId).run();
    if (!result.meta || result.meta.changes !== 1) return json({ error: 'No encontramos esa cuenta de conductor.' }, 404);
    return json({ ok: true });
  }

  if (pathname === '/api/bootstrap' && method === 'GET') {
    if (user.role === 'control') {
      await env.DB.prepare("DELETE FROM driver_locations WHERE julianday(updated_at) < julianday('now', '-12 hours')").run();
      const result = await env.DB.prepare(
        'SELECT u.id, u.username, u.display_name AS displayName, l.lat, l.lng, l.accuracy, l.speed, l.heading, l.updated_at AS updatedAt, ' +
        "(SELECT COUNT(*) FROM orders o WHERE o.driver_id = u.id AND o.status IN ('pending', 'accepted', 'en_route')) AS activeOrders " +
        'FROM users u LEFT JOIN driver_locations l ON l.driver_id = u.id WHERE u.role = ? AND u.active = 1 ORDER BY u.display_name'
      ).bind('driver').all();
      return json({ user: publicUser(user), drivers: result.results || [], orders: await getOrders(env) });
    }
    return json({ user: publicUser(user), orders: await getOrders(env, 'WHERE o.driver_id = ?', [user.id]) });
  }

  if (pathname === '/api/orders' && method === 'POST') {
    if (user.role !== 'control') return json({ error: 'No tienes permiso para emitir órdenes.' }, 403);
    const body = await readJson(request);
    const title = text(body.title, 90);
    const origin = text(body.origin, 160);
    const destination = text(body.destination, 160);
    const notes = text(body.notes, 600);
    const driverId = text(body.driverId, 40);
    const driver = await env.DB.prepare("SELECT id FROM users WHERE id = ? AND role = 'driver' AND active = 1").bind(driverId).first();
    if (!title || !origin || !destination || !driver) return json({ error: 'Completa el servicio, origen, destino y conductor.' }, 400);
    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    await env.DB.prepare(
      'INSERT INTO orders (id, title, origin, destination, notes, driver_id, status, created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
    ).bind(id, title, origin, destination, notes, driver.id, 'pending', user.id, now, now).run();
    return json({ order: (await getOrders(env, 'WHERE o.id = ?', [id]))[0] }, 201);
  }

  const orderMatch = pathname.match(/^\/api\/orders\/([a-f0-9-]+)$/i);
  if (orderMatch && method === 'PATCH') {
    const id = orderMatch[1];
    const existing = await env.DB.prepare('SELECT * FROM orders WHERE id = ?').bind(id).first();
    if (!existing) return json({ error: 'No encontramos ese servicio.' }, 404);
    const body = await readJson(request);
    const now = new Date().toISOString();
    if (user.role === 'control') {
      if (body.status === 'cancelled') {
        if (['completed', 'cancelled'].includes(existing.status)) return json({ error: 'El servicio ya terminó.' }, 409);
        await env.DB.prepare("UPDATE orders SET status = 'cancelled', updated_at = ? WHERE id = ?").bind(now, id).run();
      } else if (body.driverId && existing.status === 'pending') {
        const driver = await env.DB.prepare("SELECT id FROM users WHERE id = ? AND role = 'driver' AND active = 1").bind(text(body.driverId, 40)).first();
        if (!driver) return json({ error: 'El conductor seleccionado no existe.' }, 400);
        await env.DB.prepare('UPDATE orders SET driver_id = ?, updated_at = ? WHERE id = ?').bind(driver.id, now, id).run();
      } else {
        return json({ error: 'La torre puede cancelar un servicio activo o reasignar uno pendiente.' }, 400);
      }
    } else {
      if (existing.driver_id !== user.id) return json({ error: 'Este servicio está asignado a otro conductor.' }, 403);
      const allowed = { pending: 'accepted', accepted: 'en_route', en_route: 'completed' };
      if (allowed[existing.status] !== body.status) return json({ error: 'El servicio cambió de estado. Actualiza la pantalla.' }, 409);
      await env.DB.prepare('UPDATE orders SET status = ?, updated_at = ? WHERE id = ?').bind(body.status, now, id).run();
    }
    return json({ order: (await getOrders(env, 'WHERE o.id = ?', [id]))[0] });
  }

  if (pathname === '/api/location' && method === 'POST') {
    if (user.role !== 'driver') return json({ error: 'Solo los conductores pueden compartir ubicación.' }, 403);
    const body = await readJson(request);
    const lat = Number(body.lat);
    const lng = Number(body.lng);
    if (!Number.isFinite(lat) || lat < -90 || lat > 90 || !Number.isFinite(lng) || lng < -180 || lng > 180) {
      return json({ error: 'Coordenadas inválidas.' }, 400);
    }
    const nullableNumber = value => value == null || value === '' || !Number.isFinite(Number(value)) ? null : Number(value);
    const now = new Date().toISOString();
    await env.DB.prepare(
      'INSERT INTO driver_locations (driver_id, lat, lng, accuracy, speed, heading, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?) ' +
      'ON CONFLICT(driver_id) DO UPDATE SET lat=excluded.lat, lng=excluded.lng, accuracy=excluded.accuracy, speed=excluded.speed, heading=excluded.heading, updated_at=excluded.updated_at'
    ).bind(user.id, lat, lng, nullableNumber(body.accuracy), nullableNumber(body.speed), nullableNumber(body.heading), now).run();
    return json({ ok: true, updatedAt: now });
  }

  if (pathname === '/api/location' && method === 'DELETE') {
    if (user.role !== 'driver') return json({ error: 'Solo los conductores pueden borrar su ubicación.' }, 403);
    await env.DB.prepare('DELETE FROM driver_locations WHERE driver_id = ?').bind(user.id).run();
    return json({ ok: true });
  }

  return json({ error: 'Ruta no encontrada.' }, 404);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (!url.pathname.startsWith('/api/')) {
      return safeAsset(await env.ASSETS.fetch(request));
    }
    try {
      return await handleApi(request, env);
    } catch (error) {
      const status = Number(error.status) || 500;
      if (status === 500) console.error('API error', error);
      return json({ error: status === 500 ? 'Ocurrió un error interno.' : error.message }, status);
    }
  },
};
