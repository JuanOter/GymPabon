/**
 * Memberships screen (RF07, RF09, RF10, RF11): sell a new membership or a
 * renewal, with the dates and the value calculated as the API does, and
 * cancel a membership with its reason (administrator).
 */
import {
  api, money, date, today, escapeHtml, statusBadge, param, DURATION_LABELS, showMessage, hideMessage,
} from './api.js';
import { initPage } from './layout.js';

const $ = (id) => document.getElementById(id);
const message = $('message');
const CURRENT = ['PENDING_PAYMENT', 'SCHEDULED', 'ACTIVE'];
const state = { mode: 'new', planId: null, duration: 'ONE_MONTH' };
let plans = [];
let durations = [];
let client = null;
let history = [];
let found = [];
let isAdmin = false;
let cancelId = null;

const plan = () => plans.find((p) => p.id === state.planId);
const duration = () => durations.find((d) => d.code === state.duration);
const active = () => history.filter((m) => m.status === 'ACTIVE').sort((a, b) => b.endDate.localeCompare(a.endDate))[0];

/** Adds months like java.time.LocalDate.plusMonths (the day is kept or moved to the month's last day). */
function plusMonths(iso, months) {
  const [y, m, d] = iso.split('-').map(Number);
  const total = m - 1 + months;
  const year = y + Math.floor(total / 12);
  const month = total % 12;
  const lastDay = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  return new Date(Date.UTC(year, month, Math.min(d, lastDay))).toISOString().substring(0, 10);
}

function minusOneDay(iso) {
  const day = new Date(iso + 'T00:00:00Z');
  day.setUTCDate(day.getUTCDate() - 1);
  return day.toISOString().substring(0, 10);
}

function plusOneDay(iso) {
  const day = new Date(iso + 'T00:00:00Z');
  day.setUTCDate(day.getUTCDate() + 1);
  return day.toISOString().substring(0, 10);
}

function startDate() {
  if (state.mode === 'renew') {
    const current = active();
    return current ? plusOneDay(current.endDate) : null;
  }
  return $('startDate').value || today();
}

function drawChoices() {
  $('plans').innerHTML = plans.map((p) => '<button type="button" class="choice" data-plan="' + p.id
    + '" aria-pressed="' + (p.id === state.planId) + '"><strong>' + escapeHtml(p.name) + '</strong><span>'
    + money(p.monthlyPrice) + ' / mes</span></button>').join('');
  $('durations').innerHTML = durations.map((d) => '<button type="button" class="choice" data-duration="' + d.code
    + '" aria-pressed="' + (d.code === state.duration) + '"><strong>' + DURATION_LABELS[d.code] + '</strong><span>'
    + (d.discountRate === 0 ? 'Sin descuento' : Math.round(d.discountRate * 100) + ' % de descuento')
    + '</span></button>').join('');
  document.querySelectorAll('.toggle button').forEach((b) => b.setAttribute('aria-pressed', b.dataset.mode === state.mode));
}

function clientHint() {
  if (!client) return 'Escriba el documento o el nombre y elija el cliente.';
  if (!client.active) return 'El cliente está inactivo: reactívelo en Clientes para venderle una membresía.';
  if (state.mode === 'renew') {
    const current = active();
    return current
      ? 'Su membresía vence el ' + date(current.endDate) + ': la nueva inicia el día siguiente y no se cobra matrícula.'
      : 'El cliente no tiene una membresía activa para renovar; registre una nueva membresía.';
  }
  const hasPaid = history.some((m) => m.paid);
  return hasPaid ? 'Ya tuvo membresías: no se cobra matrícula.'
    : 'Sin membresías anteriores: se cobra matrícula (Premium está exento).';
}

/** Updates dates, trainer field, current membership and the summary. */
async function refresh() {
  drawChoices();
  const p = plan();
  const d = duration();
  const renew = state.mode === 'renew';
  $('trainer-field').hidden = !p || !p.includesTrainer;
  $('client-hint').textContent = clientHint();
  $('startDate').readOnly = renew;
  const start = startDate();
  if (renew) $('startDate').value = start || '';
  $('endDate').value = start && d ? date(minusOneDay(plusMonths(start, d.months))) : '';

  const current = active();
  $('current').hidden = !renew || !current;
  if (current) {
    $('current-text').innerHTML = escapeHtml(current.planName) + ' · ' + DURATION_LABELS[current.duration] + ' · '
      + date(current.startDate) + ' a ' + date(current.endDate) + ' · ' + statusBadge(current.status);
  }

  $('sum-mode').textContent = renew ? 'Renovación' : 'Nueva membresía';
  if (!p || !d) return;
  const subtotal = p.monthlyPrice * d.months;
  const discounted = Math.round(subtotal * (1 - d.discountRate));
  $('sum-plan').textContent = p.name + ' · ' + DURATION_LABELS[d.code];
  $('sum-subtotal').textContent = money(subtotal);
  $('sum-discount-label').textContent = 'Descuento (' + Math.round(d.discountRate * 100) + ' %)';
  $('sum-discount').textContent = subtotal === discounted ? '$0' : '−' + money(subtotal - discounted);

  let total = discounted + (history.some((m) => m.paid) || renew ? 0 : p.enrollmentFee);
  if (client && client.active) {
    try {
      total = (await api.get('/api/memberships/quote?clientId=' + client.id + '&planId=' + p.id
        + '&duration=' + d.code)).totalAmount;
    } catch (error) {
      showMessage(message, error.message);
    }
  }
  const fee = total - discounted;
  if (renew) $('sum-fee').textContent = 'No aplica (renovación)';
  else if (fee > 0) $('sum-fee').textContent = money(fee);
  else $('sum-fee').textContent = p.enrollmentFee === 0 ? 'Exento' : 'No aplica (ya pagó antes)';
  $('total').textContent = money(total);

  const paidStatus = start && start > today() ? 'Programada hasta el ' + date(start) : 'Activa';
  $('state-note').textContent = 'Estado al guardar: Pendiente de pago. Al pagar queda ' + paidStatus + '.';
  $('submit').disabled = !client || !client.active || (renew && !current);
}

async function loadHistory() {
  history = await api.get('/api/memberships?clientId=' + client.id);
  $('history').innerHTML = history.length === 0
    ? '<tr><td colspan="7" class="empty">El cliente todavía no tiene membresías.</td></tr>'
    : history.map((m) => '<tr><td>' + escapeHtml(m.planName) + (m.trainerName ? ' · ' + escapeHtml(m.trainerName) : '')
      + '</td><td>' + DURATION_LABELS[m.duration] + '</td><td>' + date(m.startDate) + '</td><td>' + date(m.endDate)
      + '</td><td>' + money(m.totalAmount) + '</td><td>' + statusBadge(m.status) + '</td><td>' + actionsFor(m)
      + '</td></tr>').join('');
  $('history-card').hidden = false;
}

function actionsFor(membership) {
  const actions = [];
  if (membership.status === 'PENDING_PAYMENT') {
    actions.push('<a class="link-plain" href="pagos.html?membershipId=' + membership.id + '">Pagar</a>');
  }
  if (isAdmin && CURRENT.includes(membership.status)) {
    actions.push('<button type="button" class="link danger" data-cancel="' + membership.id + '">Cancelar</button>');
  }
  return actions.join(' · ');
}

async function selectClient(selected) {
  client = selected;
  $('client').value = client.documentNumber + ' · ' + client.fullName;
  await loadHistory();
  await refresh();
}

/** Fills the suggestions while the user types, and selects the client when one is chosen. */
async function searchClients() {
  const text = $('client').value.trim();
  const chosen = found.find((c) => text === c.documentNumber + ' · ' + c.fullName);
  if (chosen) {
    await selectClient(chosen);
    return;
  }
  if (text.length < 2) return;
  found = await api.get('/api/clients?q=' + encodeURIComponent(text.split(' · ')[0]));
  $('client-options').innerHTML = found.map((c) => '<option value="' + escapeHtml(c.documentNumber + ' · '
    + c.fullName) + '">').join('');
  if (found.length === 1 && found[0].documentNumber === text) await selectClient(found[0]);
}

async function save(event) {
  event.preventDefault();
  hideMessage(message);
  const p = plan();
  const body = {
    clientId: client.id,
    planId: p.id,
    duration: state.duration,
    trainerId: p.includesTrainer ? Number($('trainer').value) || null : null,
  };
  try {
    const saved = state.mode === 'renew'
      ? await api.post('/api/memberships/renew', body)
      : await api.post('/api/memberships', { ...body, startDate: $('startDate').value });
    location.href = 'pagos.html?membershipId=' + saved.id;
  } catch (error) {
    showMessage(message, error.message);
  }
}

async function start() {
  const user = await initPage('membresias');
  isAdmin = user.role === 'ADMIN';
  [plans, durations] = await Promise.all([api.get('/api/plans'), api.get('/api/memberships/durations')]);
  const trainers = await api.get('/api/trainers/available');
  $('trainer').innerHTML = trainers.map((t) => '<option value="' + t.id + '">' + escapeHtml(t.fullName) + ' · '
    + t.premiumClients + ' de 20 clientes</option>').join('');
  state.planId = (plans.find((p) => p.type === 'STANDARD') || plans[0]).id;
  $('startDate').value = today();
  $('startDate').min = today();

  document.querySelector('.toggle').addEventListener('click', (event) => {
    if (!event.target.dataset.mode) return;
    state.mode = event.target.dataset.mode;
    if (state.mode === 'new') $('startDate').value = today();
    refresh();
  });
  $('plans').addEventListener('click', (event) => {
    const button = event.target.closest('[data-plan]');
    if (button) { state.planId = Number(button.dataset.plan); refresh(); }
  });
  $('durations').addEventListener('click', (event) => {
    const button = event.target.closest('[data-duration]');
    if (button) { state.duration = button.dataset.duration; refresh(); }
  });
  $('startDate').addEventListener('change', refresh);
  let timer;
  $('client').addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(() => searchClients().catch((e) => showMessage(message, e.message)), 250);
  });
  $('membership-form').addEventListener('submit', save);

  $('history').addEventListener('click', (event) => {
    if (!event.target.dataset.cancel) return;
    cancelId = event.target.dataset.cancel;
    $('cancel-form').hidden = false;
    $('cancel-reason').focus();
  });
  $('close-cancel').addEventListener('click', () => { $('cancel-form').hidden = true; });
  $('confirm-cancel').addEventListener('click', async () => {
    try {
      await api.patch('/api/memberships/' + cancelId + '/cancel', { reason: $('cancel-reason').value });
      $('cancel-form').hidden = true;
      $('cancel-reason').value = '';
      showMessage(message, 'Membresía cancelada.', 'ok');
      await loadHistory();
      await refresh();
    } catch (error) {
      showMessage(message, error.message);
    }
  });

  const clientId = param('clientId');
  if (clientId) {
    if (param('renew')) state.mode = 'renew';
    await selectClient(await api.get('/api/clients/' + clientId));
  } else {
    await refresh();
  }
}

start().catch((error) => showMessage(message, error.message));
