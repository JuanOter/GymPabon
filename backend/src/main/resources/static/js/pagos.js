/** Payments screen (RF08, RF14): collect pending memberships, list payments and void them (administrator). */
import {
  api, money, date, today, firstDayOfMonth, escapeHtml, badge, param, METHOD_LABELS, DURATION_LABELS,
  showMessage, hideMessage,
} from './api.js';
import { initPage } from './layout.js';

const message = document.getElementById('message');
const $ = (id) => document.getElementById(id);
let pending = [];
let payments = [];
let memberships = new Map();
let method = 'CASH';
let isAdmin = false;

async function loadMemberships() {
  memberships = new Map((await api.get('/api/memberships')).map((m) => [m.id, m]));
}

/** "Premium · 3 meses", plus "(renovación)" or "+ matrícula" when it applies. */
function conceptOf(membershipId, planName) {
  const m = memberships.get(membershipId);
  if (!m) return escapeHtml(planName);
  const isRenewal = [...memberships.values()].some((other) => other.clientId === m.clientId && other.id !== m.id
    && other.paid && other.endDate < m.startDate && other.startDate < m.startDate
    && new Date(new Date(other.endDate + 'T00:00:00Z').getTime() + 86400000).toISOString().startsWith(m.startDate));
  let concept = m.planName + ' · ' + DURATION_LABELS[m.duration];
  if (isRenewal) concept += ' (renovación)';
  else if (m.enrollmentFee > 0) concept += ' + matrícula';
  return escapeHtml(concept);
}

async function loadPending(selectedId) {
  pending = await api.get('/api/payments/pending');
  $('membership').innerHTML = pending.length === 0
    ? '<option value="">No hay membresías pendientes de pago</option>'
    : pending.map((p) => '<option value="' + p.membershipId + '">' + escapeHtml(p.clientName) + ' · '
      + conceptOf(p.membershipId, p.planName) + '</option>').join('');
  if (selectedId && pending.some((p) => p.membershipId === Number(selectedId))) $('membership').value = selectedId;
  $('submit').disabled = pending.length === 0;
  showSelected();
}

function showSelected() {
  const selected = pending.find((p) => p.membershipId === Number($('membership').value));
  $('period').textContent = selected ? date(selected.startDate) + ' a ' + date(selected.endDate) : '—';
  $('amount').textContent = selected ? money(selected.totalAmount) : '—';
}

function render() {
  const text = $('client-filter').value.trim().toLowerCase();
  const shown = payments.filter((p) => !text || p.clientName.toLowerCase().includes(text)
    || p.clientDocument.includes(text));
  $('payments').innerHTML = shown.length === 0
    ? '<tr><td colspan="7" class="empty">No hay pagos con estos filtros.</td></tr>'
    : shown.map((p) => '<tr><td>' + date(p.paymentDate) + '</td><td class="wrap">' + escapeHtml(p.clientName)
      + '</td><td class="wrap">' + conceptOf(p.membershipId, p.planName) + '</td><td>' + METHOD_LABELS[p.method] + '</td><td>' + money(p.amount)
      + '</td><td>' + (p.voided ? badge('Anulado', 'red') : badge('Registrado', 'green')) + '</td><td class="wrap">'
      + (p.voided ? '<span class="muted">' + escapeHtml(p.voidReason) + '</span>'
        : (isAdmin ? '<button class="link" data-void="' + p.id + '">Anular</button>' : ''))
      + '</td></tr>').join('');
  const valid = shown.filter((p) => !p.voided).reduce((sum, p) => sum + p.amount, 0);
  $('total').textContent = shown.length + ' pago(s) · total recaudado sin anulados: ' + money(valid);
}

async function loadPayments() {
  const methodFilter = $('method-filter').value;
  payments = await api.get('/api/payments?from=' + $('from').value + '&to=' + $('to').value
    + (methodFilter ? '&method=' + methodFilter : ''));
  render();
}

async function pay(event) {
  event.preventDefault();
  hideMessage(message);
  $('pay-result').hidden = true;
  $('submit').disabled = true;
  try {
    const payment = await api.post('/api/payments', { membershipId: Number($('membership').value), method });
    const membership = await api.get('/api/memberships/' + payment.membershipId);
    const status = membership.status === 'SCHEDULED' ? 'Programada desde el ' + date(membership.startDate)
      : 'Activa hasta el ' + date(membership.endDate);
    $('pay-result').innerHTML = '<strong>Pago registrado (' + METHOD_LABELS[method].toLowerCase() + ')</strong><br>'
      + 'La membresía de ' + escapeHtml(payment.clientName) + ' quedó ' + status + '.';
    $('pay-result').hidden = false;
    await loadMemberships();
    await Promise.all([loadPending(), loadPayments()]);
  } catch (error) {
    showMessage(message, error.message);
    $('submit').disabled = false;
  }
}

async function start() {
  const user = await initPage('pagos');
  isAdmin = user.role === 'ADMIN';
  $('from').value = firstDayOfMonth();
  $('to').value = today();
  $('payment-date').value = date(today());
  await loadMemberships();
  await Promise.all([loadPending(param('membershipId')), loadPayments()]);

  $('membership').addEventListener('change', showSelected);
  $('methods').addEventListener('click', (event) => {
    if (!event.target.dataset.method) return;
    method = event.target.dataset.method;
    document.querySelectorAll('#methods .choice').forEach((b) => b.setAttribute('aria-pressed', b.dataset.method === method));
  });
  $('payment-form').addEventListener('submit', pay);
  $('client-filter').addEventListener('input', render);
  ['from', 'to', 'method-filter'].forEach((id) => $(id).addEventListener('change',
    () => loadPayments().catch((e) => showMessage(message, e.message))));

  $('payments').addEventListener('click', async (event) => {
    const id = event.target.dataset.void;
    if (!id) return;
    const reason = prompt('Motivo de la anulación:');
    if (reason === null) return;
    try {
      await api.patch('/api/payments/' + id + '/void', { reason });
      showMessage(message, 'Pago anulado. La membresía volvió a quedar pendiente de pago.', 'ok');
      await loadMemberships();
      await Promise.all([loadPending(), loadPayments()]);
    } catch (error) {
      showMessage(message, error.message);
    }
  });
}

start().catch((error) => showMessage(message, error.message));
