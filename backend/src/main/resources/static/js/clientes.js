/** Clients screen: search, register, edit, deactivate and reactivate clients (RF03, RF04). */
import {
  api, escapeHtml, badge, statusBadge, date, showMessage, hideMessage, membershipsByClient,
} from './api.js';
import { initPage } from './layout.js';

const message = document.getElementById('message');
const form = document.getElementById('client-form');
const fields = ['documentNumber', 'firstName', 'lastName', 'birthDate', 'phone', 'email'];
let clients = [];
let byClient = new Map();
let editingId = null;

/** Status used by the filter: the client's membership, or INACTIVE for inactive clients. */
function statusOf(client) {
  if (!client.active) return 'INACTIVE';
  const info = byClient.get(client.id);
  return info ? info.main.status : 'NONE';
}

function actionsFor(client, main) {
  if (!client.active) {
    return '<button class="link" data-activate="' + client.id + '">Reactivar</button>';
  }
  if (main && main.status === 'PENDING_PAYMENT') {
    return '<a class="link-plain" href="pagos.html?membershipId=' + main.id + '">Pagar</a>';
  }
  return '<button class="link" data-edit="' + client.id + '">Editar</button> · '
    + '<button class="link danger" data-deactivate="' + client.id + '">Inactivar</button>';
}

function rowOf(client) {
  const main = (byClient.get(client.id) || {}).main;
  let membership = badge('Sin membresía', 'gray');
  if (!client.active) membership = badge('Cliente inactivo', 'gray');
  else if (main) membership = statusBadge(main.status);
  const ends = main && main.status !== 'PENDING_PAYMENT' ? date(main.endDate) : '—';
  return '<tr><td>' + escapeHtml(client.documentNumber) + '</td>'
    + '<td class="wrap"><a class="name-link" href="membresias.html?clientId=' + client.id + '">'
    + escapeHtml(client.fullName) + '</a></td><td>' + escapeHtml(client.phone || '—') + '</td>'
    + '<td>' + escapeHtml(main ? main.planName : '—') + '</td><td>' + ends + '</td>'
    + '<td>' + membership + '</td><td>' + actionsFor(client, main) + '</td></tr>';
}

function render() {
  const filter = document.getElementById('status-filter').value;
  const shown = clients.filter((client) => !filter || statusOf(client) === filter);
  document.getElementById('clients').innerHTML = shown.length === 0
    ? '<tr><td colspan="7" class="empty">No hay clientes que coincidan con la búsqueda.</td></tr>'
    : shown.map(rowOf).join('');
  const active = clients.filter((client) => statusOf(client) === 'ACTIVE').length;
  document.getElementById('count').textContent = 'Mostrando ' + shown.length + ' de ' + clients.length
    + ' clientes · ' + active + ' con membresía activa';
}

async function loadClients() {
  const text = document.getElementById('search').value.trim();
  const [found, memberships] = await Promise.all([
    api.get('/api/clients' + (text ? '?q=' + encodeURIComponent(text) : '')),
    api.get('/api/memberships'),
  ]);
  clients = found;
  byClient = membershipsByClient(memberships);
  render();
}

function readForm() {
  const data = {};
  fields.forEach((field) => { data[field] = document.getElementById(field).value.trim() || null; });
  return data;
}

function fillForm(client) {
  editingId = client.id;
  fields.forEach((field) => { document.getElementById(field).value = client[field] || ''; });
  document.getElementById('form-title').textContent = 'Editar cliente';
  document.getElementById('submit').textContent = 'Guardar cambios';
  document.getElementById('documentNumber').focus();
}

function resetForm() {
  editingId = null;
  form.reset();
  document.getElementById('form-title').textContent = 'Registrar cliente';
  document.getElementById('submit').textContent = 'Guardar y asignar membresía';
}

async function save() {
  hideMessage(message);
  try {
    const data = readForm();
    if (editingId) {
      const saved = await api.put('/api/clients/' + editingId, data);
      showMessage(message, 'Cliente ' + saved.fullName + ' actualizado.', 'ok');
      resetForm();
      await loadClients();
    } else {
      const saved = await api.post('/api/clients', data);
      location.href = 'membresias.html?clientId=' + saved.id;
    }
  } catch (error) {
    showMessage(message, error.message);
  }
}

async function changeStatus(id, action, text) {
  try {
    await api.patch('/api/clients/' + id + '/' + action);
    showMessage(message, text, 'ok');
    await loadClients();
  } catch (error) {
    showMessage(message, error.message);
  }
}

async function start() {
  await initPage('clientes');
  let timer;
  document.getElementById('search').addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(() => loadClients().catch((e) => showMessage(message, e.message)), 300);
  });
  document.getElementById('status-filter').addEventListener('change', render);
  form.addEventListener('submit', (event) => { event.preventDefault(); save(); });
  document.getElementById('clear').addEventListener('click', () => setTimeout(resetForm));
  document.getElementById('clients').addEventListener('click', (event) => {
    const { edit, deactivate, activate } = event.target.dataset;
    if (edit) {
      fillForm(clients.find((c) => c.id === Number(edit)));
    } else if (deactivate && confirm('¿Inactivar este cliente? Su historial se conserva.')) {
      changeStatus(deactivate, 'deactivate', 'Cliente inactivado.');
    } else if (activate) {
      changeStatus(activate, 'activate', 'Cliente reactivado.');
    }
  });
  await loadClients();
}

start().catch((error) => showMessage(message, error.message));
