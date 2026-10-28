/** Reports screen (administrator only): income by plan, clients by plan, attendance and expirations. */
import { api, money, date, today, firstDayOfMonth, escapeHtml, showMessage } from './api.js';
import { initPage } from './layout.js';

const message = document.getElementById('message');
const $ = (id) => document.getElementById(id);

function bars(entries, format) {
  const max = Math.max(1, ...entries.map(([, value]) => value));
  return entries.map(([label, value]) => '<div class="bar-row"><div><span>' + escapeHtml(label) + '</span><strong>'
    + format(value) + '</strong></div><div class="bar" aria-hidden="true"><span style="width:'
    + Math.round(value / max * 100) + '%"></span></div></div>').join('');
}

async function loadPeriod() {
  const period = 'from=' + $('from').value + '&to=' + $('to').value;
  const income = Object.entries(await api.get('/api/reports/income-by-plan?' + period));
  $('income').innerHTML = income.length ? bars(income, money) : '<p class="hint">No hay pagos en el periodo.</p>';
  $('income-total').textContent = 'Total: ' + money(income.reduce((sum, [, value]) => sum + value, 0));

  const attendance = Object.entries(await api.get('/api/reports/attendance-by-day?' + period));
  const max = Math.max(1, ...attendance.map(([, n]) => n));
  $('attendance').innerHTML = attendance.length === 0
    ? '<tr><td colspan="3" class="empty">No hay entradas en el periodo.</td></tr>'
    : attendance.map(([day, n]) => '<tr><td>' + date(day) + '</td><td>' + n + '</td><td><div class="bar" style="margin:0">'
      + '<span style="width:' + Math.round(n / max * 100) + '%"></span></div></td></tr>').join('');
}

async function loadExpiring() {
  const memberships = await api.get('/api/reports/expiring?days=' + $('days').value);
  $('expiring').innerHTML = memberships.length === 0
    ? '<tr><td colspan="3" class="empty">Ninguna membresía vence en ese plazo.</td></tr>'
    : memberships.map((m) => '<tr><td>' + escapeHtml(m.clientName) + '</td><td>' + escapeHtml(m.planName)
      + '</td><td>' + date(m.endDate) + '</td></tr>').join('');
}

async function start() {
  await initPage('reportes', true);
  $('from').value = firstDayOfMonth();
  $('to').value = today();
  const clients = Object.entries(await api.get('/api/reports/clients-by-plan'));
  $('clients-by-plan').innerHTML = clients.length ? bars(clients, (n) => n + ' cliente(s)')
    : '<p class="hint">No hay membresías activas.</p>';
  await loadPeriod();
  await loadExpiring();
  ['from', 'to'].forEach((id) => $(id).addEventListener('change',
    () => loadPeriod().catch((e) => showMessage(message, e.message))));
  $('days').addEventListener('change', () => loadExpiring().catch((e) => showMessage(message, e.message)));
}

start().catch((error) => { if (error.message !== 'Solo administrador') showMessage(message, error.message); });
