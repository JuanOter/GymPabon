/** Attendance screen (RF12, RF13): registers an entry attempt and shows the history of a period. */
import {
  api, date, time, today, escapeHtml, badge, nowLabel, membershipsByClient, showMessage,
} from './api.js';
import { initPage } from './layout.js';

const $ = (id) => document.getElementById(id);
const CHECK = '<svg aria-hidden="true" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#166534" '
  + 'stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>';
const CROSS = '<svg aria-hidden="true" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#991B1B" '
  + 'stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>';
let records = [];
let byClient = new Map();

const planOf = (clientId) => ((byClient.get(clientId) || {}).main || {}).planName || '—';

/** 2026-10-27T17:05:00 -> 27/10 · 5:05 p. m. */
function shortDateTime(iso) {
  return date(iso).substring(0, 5) + ' · ' + time(iso);
}

function renderHistory() {
  const filter = $('result-filter').value;
  const text = $('client-filter').value.trim().toLowerCase();
  const shown = records.filter((r) => (!filter || (filter === 'yes') === r.allowed)
    && (!text || r.clientName.toLowerCase().includes(text) || r.clientDocument.includes(text)));
  $('history').innerHTML = shown.length === 0
    ? '<tr><td colspan="5" class="empty">No hay ingresos para mostrar.</td></tr>'
    : shown.map((r) => '<tr><td>' + shortDateTime(r.dateTime) + '</td><td>' + escapeHtml(r.clientName) + '</td><td>'
      + escapeHtml(planOf(r.clientId)) + '</td><td>'
      + (r.allowed ? badge('Permitido', 'green') : badge('Rechazado', 'red'))
      + '</td><td class="wrap">' + escapeHtml(r.rejectionReason || '—') + '</td></tr>').join('');
}

async function loadHistory() {
  records = await api.get('/api/attendance?from=' + $('from').value + '&to=' + $('to').value);
  renderHistory();
}

/** Today's figures and the last clients that came in, as quick buttons. */
async function loadToday() {
  const entries = await api.get('/api/attendance?date=' + today());
  const allowed = entries.filter((e) => e.allowed).length;
  const rejected = entries.length - allowed;
  $('today-summary').textContent = 'Hoy: ' + allowed + (allowed === 1 ? ' permitido · ' : ' permitidos · ')
    + rejected + (rejected === 1 ? ' rechazado' : ' rechazados');
  const recent = [];
  entries.forEach((e) => {
    if (recent.length < 3 && !recent.some((r) => r.clientDocument === e.clientDocument)) recent.push(e);
  });
  $('recent').innerHTML = recent.map((r) => '<button type="button" class="chip" data-document="'
    + escapeHtml(r.clientDocument) + '">' + escapeHtml(r.clientName) + '</button>').join('');
  $('recent-wrap').hidden = recent.length === 0;
}

async function showResult(entry) {
  const memberships = await api.get('/api/memberships?clientId=' + entry.clientId);
  const main = (membershipsByClient(memberships).get(entry.clientId) || {}).main;
  const plan = main ? ' · Plan ' + escapeHtml(main.planName) : '';
  const detail = entry.allowed
    ? (main ? 'Membresía activa hasta el ' + date(main.endDate) : '')
    : escapeHtml(entry.rejectionReason);
  $('result').className = 'result ' + (entry.allowed ? 'ok' : 'no');
  $('result').setAttribute('role', entry.allowed ? 'status' : 'alert');
  $('result').innerHTML = (entry.allowed ? CHECK : CROSS) + '<div><strong>Ingreso '
    + (entry.allowed ? 'permitido' : 'rechazado') + '</strong><span>' + escapeHtml(entry.clientName) + plan
    + (detail ? ' · ' + detail : '') + '</span></div>';
  $('result').hidden = false;
}

async function register(documentNumber) {
  if (!documentNumber) return;
  $('submit').disabled = true;
  try {
    const entry = await api.post('/api/attendance', { documentNumber });
    await showResult(entry);
    $('document').value = '';
    $('to').value = today();
    await Promise.all([loadHistory(), loadToday()]);
  } catch (error) {
    showMessage($('result'), error.message);
  } finally {
    $('submit').disabled = false;
    $('document').focus();
  }
}

async function start() {
  await initPage('asistencia');
  $('now').textContent = nowLabel();
  setInterval(() => { $('now').textContent = nowLabel(); }, 30000);
  $('from').value = today();
  $('to').value = today();
  byClient = membershipsByClient(await api.get('/api/memberships'));
  await Promise.all([loadHistory(), loadToday()]);

  ['from', 'to'].forEach((id) => $(id).addEventListener('change',
    () => loadHistory().catch((e) => showMessage($('result'), e.message))));
  $('result-filter').addEventListener('change', renderHistory);
  $('client-filter').addEventListener('input', renderHistory);
  $('entry-form').addEventListener('submit', (event) => {
    event.preventDefault();
    register($('document').value.trim());
  });
  $('recent').addEventListener('click', (event) => {
    const documentNumber = event.target.dataset.document;
    if (!documentNumber) return;
    $('document').value = documentNumber;
    register(documentNumber);
  });
}

start().catch((error) => showMessage($('result'), error.message));
