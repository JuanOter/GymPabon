/**
 * Connection between the web pages and the GymPabon REST API, plus the format
 * helpers that every screen shares (money, dates, statuses).
 */

/**
 * Calls the API and returns the JSON answer. If the API answers with an
 * error, it throws an Error whose message comes from the server. A 401 sends
 * the user back to the sign-in screen.
 */
async function request(method, url, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  let response;
  try {
    response = await fetch(url, options);
  } catch (e) {
    throw new Error('No hay conexión con el servidor');
  }
  if (response.status === 401 && !location.pathname.endsWith('login.html')) {
    location.href = 'login.html';
    throw new Error('Debe iniciar sesión');
  }
  if (response.status === 204) {
    return null;
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(data && data.error ? data.error : 'Ocurrió un error (' + response.status + ')');
  }
  return data;
}

export const api = {
  get: (url) => request('GET', url),
  post: (url, body) => request('POST', url, body),
  put: (url, body) => request('PUT', url, body),
  patch: (url, body) => request('PATCH', url, body),
};

/** $70.000 */
export function money(value) {
  return '$' + Math.round(value).toLocaleString('es-CO');
}

/** 2026-10-27 -> 27/10/2026 */
export function date(iso) {
  if (!iso) return '—';
  const [y, m, d] = iso.substring(0, 10).split('-');
  return d + '/' + m + '/' + y;
}

/** 2026-10-27T17:05:00 -> 27/10/2026 5:05 p. m. */
export function dateTime(iso) {
  if (!iso) return '—';
  return date(iso) + ' ' + time(iso);
}

/** 2026-10-27T17:05:00 -> 5:05 p. m. */
export function time(iso) {
  const [h, min] = iso.substring(11, 16).split(':').map(Number);
  const suffix = h < 12 ? 'a. m.' : 'p. m.';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return hour12 + ':' + String(min).padStart(2, '0') + ' ' + suffix;
}

/** Today as yyyy-mm-dd, for date inputs. */
export function today() {
  const now = new Date();
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().substring(0, 10);
}

/** First day of the current month as yyyy-mm-dd. */
export function firstDayOfMonth() {
  return today().substring(0, 8) + '01';
}

const MEMBERSHIP_STATUS = {
  PENDING_PAYMENT: ['Pendiente de pago', 'yellow'],
  SCHEDULED: ['Programada', 'blue'],
  ACTIVE: ['Activa', 'green'],
  EXPIRED: ['Vencida', 'red'],
  CANCELLED: ['Cancelada', 'gray'],
};

/** Colored badge with the membership status in Spanish. */
export function statusBadge(status) {
  const [label, color] = MEMBERSHIP_STATUS[status] || [status, 'gray'];
  return badge(label, color);
}

export function badge(label, color) {
  return '<span class="badge ' + color + '">' + escapeHtml(label) + '</span>';
}

export const METHOD_LABELS = { CASH: 'Efectivo', TRANSFER: 'Transferencia', CARD: 'Tarjeta' };
export const DURATION_LABELS = {
  ONE_MONTH: '1 mes', THREE_MONTHS: '3 meses', SIX_MONTHS: '6 meses', TWELVE_MONTHS: '12 meses',
};
export const ROLE_LABELS = { ADMIN: 'Administrador', RECEPTIONIST: 'Recepcionista' };

/** Escapes text before putting it inside HTML, so data can never inject markup. */
export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

/** Shows a message box (error, ok or info) inside an element. */
export function showMessage(element, text, type = 'error') {
  element.className = 'msg ' + type;
  element.textContent = text;
  element.hidden = false;
}

export function hideMessage(element) {
  element.hidden = true;
}

/**
 * Groups the memberships (GET /api/memberships) by client and picks the one
 * that describes the client today: the active one, else the one pending
 * payment, else the scheduled one, else the last one that ended. It also
 * returns the renewal already sold after the active membership, if any.
 *
 * @param {Array<object>} memberships list of MembershipResponse
 * @returns {Map<number, {main: object, renewal: object|null}>} by client id
 */
export function membershipsByClient(memberships) {
  const groups = new Map();
  memberships.forEach((m) => {
    if (!groups.has(m.clientId)) groups.set(m.clientId, []);
    groups.get(m.clientId).push(m);
  });
  const result = new Map();
  groups.forEach((list, clientId) => {
    const byStatus = (status) => list.filter((m) => m.status === status);
    const latestEnd = (items) => items.sort((a, b) => b.endDate.localeCompare(a.endDate))[0];
    const active = latestEnd(byStatus('ACTIVE'));
    const main = active || byStatus('PENDING_PAYMENT')[0] || byStatus('SCHEDULED')[0] || latestEnd([...list]);
    const renewal = active
      ? list.find((m) => (m.status === 'SCHEDULED' || m.status === 'PENDING_PAYMENT') && m.startDate > active.endDate)
      : null;
    result.set(clientId, { main, renewal: renewal || null });
  });
  return result;
}

const WEEKDAYS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

/** Current day and time: "Martes 27/10/2026 · 5:20 p. m." */
export function nowLabel() {
  const now = new Date();
  const iso = today() + 'T' + String(now.getHours()).padStart(2, '0') + ':'
    + String(now.getMinutes()).padStart(2, '0');
  return WEEKDAYS[now.getDay()] + ' ' + date(iso) + ' · ' + time(iso);
}

/** Reads a query string parameter: ?clientId=3 -> '3'. */
export function param(name) {
  return new URLSearchParams(location.search).get(name);
}
