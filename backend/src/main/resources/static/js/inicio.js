/** Main panel: the day's figures, memberships about to expire and income by plan (RF10, RF15). */
import {
  api, money, date, today, firstDayOfMonth, escapeHtml, badge, showMessage, membershipsByClient,
} from './api.js';
import { initPage } from './layout.js';

const message = document.getElementById('message');
const PLAN_ORDER = ['Básico', 'Estándar', 'Premium'];
const PLAN_COLORS = { Básico: '#FB923C', Estándar: '#C2410C', Premium: '#FDBA74' };
const monthName = new Date().toLocaleDateString('es-CO', { month: 'long' });
let byClient = new Map();

function byPlanOrder([a], [b]) {
  return PLAN_ORDER.indexOf(a) - PLAN_ORDER.indexOf(b);
}

async function loadSummary() {
  const [summary, entries] = await Promise.all([
    api.get('/api/reports/dashboard'),
    api.get('/api/attendance?date=' + today()),
  ]);
  document.getElementById('active-clients').textContent = summary.activeClients;
  document.getElementById('today-entries').textContent = summary.todayEntries;
  document.getElementById('expiring-count').textContent = summary.expiringMemberships;
  document.getElementById('income-label').textContent = 'Recaudado en ' + monthName;
  document.getElementById('monthly-income').textContent = money(summary.monthlyIncome);
  const rejected = entries.filter((entry) => !entry.allowed).length;
  document.getElementById('rejected-entries').textContent = rejected === 1
    ? '1 ingreso rechazado' : rejected + ' ingresos rechazados';
}

async function loadMemberships() {
  byClient = membershipsByClient(await api.get('/api/memberships'));
  const counts = {};
  byClient.forEach(({ main }) => {
    if (main.status === 'ACTIVE') counts[main.planName] = (counts[main.planName] || 0) + 1;
  });
  document.getElementById('clients-by-plan').textContent = Object.entries(counts).sort(byPlanOrder)
    .map(([plan, count]) => count + ' ' + plan).join(' · ');
}

function renewalCells(clientId) {
  const renewal = (byClient.get(clientId) || {}).renewal;
  if (!renewal) {
    return '<td>' + badge('Activa', 'green') + '</td><td><a class="link-plain" href="membresias.html?clientId='
      + clientId + '&renew=1">Renovar</a></td>';
  }
  if (renewal.status === 'PENDING_PAYMENT') {
    return '<td>' + badge('Renovada', 'blue') + '</td><td><a class="link-plain" href="pagos.html?membershipId='
      + renewal.id + '">Pagar renovación</a></td>';
  }
  return '<td>' + badge('Renovada', 'blue') + '</td><td class="muted">Programada</td>';
}

async function loadExpiring() {
  const memberships = await api.get('/api/reports/expiring?days=7');
  document.getElementById('expiring').innerHTML = memberships.length === 0
    ? '<tr><td colspan="5" class="empty">No hay membresías por vencer en los próximos 7 días.</td></tr>'
    : memberships.map((m) => '<tr><td>' + escapeHtml(m.clientName) + '</td><td>' + escapeHtml(m.planName)
      + '</td><td>' + date(m.endDate) + '</td>' + renewalCells(m.clientId) + '</tr>').join('');
}

async function loadIncomeByPlan() {
  const income = await api.get('/api/reports/income-by-plan?from=' + firstDayOfMonth() + '&to=' + today());
  const entries = Object.entries(income).sort(byPlanOrder);
  const max = Math.max(1, ...entries.map(([, value]) => value));
  document.getElementById('income-title').textContent = 'Recaudo de ' + monthName + ' por plan';
  document.getElementById('income-by-plan').innerHTML = entries.length === 0
    ? '<p class="hint">Todavía no hay pagos este mes.</p>'
    : entries.map(([plan, value]) => '<div class="bar-row"><div><span>' + escapeHtml(plan) + '</span><strong>'
      + money(value) + '</strong></div><div class="bar" aria-hidden="true"><span style="width:'
      + Math.round(value / max * 100) + '%;background:' + (PLAN_COLORS[plan] || '#C2410C') + '"></span></div></div>')
      .join('');
  document.getElementById('income-card').hidden = false;
}

async function start() {
  const user = await initPage('inicio');
  document.getElementById('today').textContent = 'Resumen del gimnasio · '
    + new Date().toLocaleDateString('es-CO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  try {
    await Promise.all([loadSummary(), loadMemberships()]);
    await loadExpiring();
    if (user.role === 'ADMIN') {
      document.getElementById('see-all').hidden = false;
      await loadIncomeByPlan();
    }
  } catch (error) {
    showMessage(message, error.message);
  }
}

start();
