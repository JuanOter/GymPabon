/**
 * Trainers screen (RF05, RF11): everyone sees the team and its Premium slots;
 * the administrator registers, edits, deactivates and assigns trainers.
 */
import { api, escapeHtml, badge, showMessage, hideMessage } from './api.js';
import { initPage } from './layout.js';

const MAX_CLIENTS = 20;
const CURRENT = ['ACTIVE', 'PENDING_PAYMENT', 'SCHEDULED'];
const message = document.getElementById('message');
const form = document.getElementById('trainer-form');
const fields = ['documentNumber', 'phone', 'firstName', 'lastName', 'specialty', 'workSchedule'];
let trainers = [];
let editingId = null;
let isAdmin = false;

function slotsCell(trainer) {
  const full = trainer.availableSlots === 0;
  const width = Math.round(trainer.premiumClients / MAX_CLIENTS * 100);
  return '<span class="slots' + (full ? ' full' : '') + '" aria-hidden="true"><span style="width:' + width
    + '%"></span></span>' + trainer.premiumClients + ' de ' + MAX_CLIENTS + (full ? ' · cupo lleno' : '');
}

function actionsCell(trainer) {
  if (!isAdmin) return '';
  return '<button class="link" data-edit="' + trainer.id + '">Editar</button> · ' + (trainer.active
    ? '<button class="link danger" data-deactivate="' + trainer.id + '">Inactivar</button>'
    : '<button class="link" data-activate="' + trainer.id + '">Activar</button>');
}

async function loadTrainers() {
  trainers = await api.get('/api/trainers');
  document.getElementById('trainers').innerHTML = trainers.map((t) => '<tr>'
    + '<td>' + escapeHtml(t.fullName) + '</td><td>' + escapeHtml(t.specialty) + '</td>'
    + '<td>' + escapeHtml(t.workSchedule) + '</td><td>' + slotsCell(t) + '</td>'
    + '<td>' + (t.active ? badge('Activo', 'green') : badge('Inactivo', 'gray')) + '</td>'
    + '<td>' + actionsCell(t) + '</td></tr>').join('');
  const active = trainers.filter((t) => t.active);
  const assigned = active.reduce((sum, t) => sum + t.premiumClients, 0);
  document.getElementById('team-summary').textContent = assigned + ' clientes Premium asignados · cupo total '
    + active.length * MAX_CLIENTS;
  document.getElementById('new-trainer').innerHTML = active.map((t) => '<option value="' + t.id + '"'
    + (t.availableSlots === 0 ? ' disabled>' + escapeHtml(t.fullName) + ' · cupo lleno'
      : '>' + escapeHtml(t.fullName) + ' · ' + t.availableSlots + (t.availableSlots === 1 ? ' cupo libre' : ' cupos libres'))
    + '</option>').join('');
}

async function loadPremiumClients() {
  const memberships = (await api.get('/api/memberships'))
    .filter((m) => m.planName === 'Premium' && CURRENT.includes(m.status))
    .sort((a, b) => a.clientName.localeCompare(b.clientName));
  document.getElementById('premium-membership').innerHTML = memberships.length === 0
    ? '<option value="">No hay clientes Premium vigentes</option>'
    : memberships.map((m) => '<option value="' + m.id + '">' + escapeHtml(m.clientName) + ' · '
      + (m.trainerName ? 'actual: ' + escapeHtml(m.trainerName) : 'sin entrenador') + '</option>').join('');
}

/** Selects a value in a select, adding it if it is not one of the options. */
function selectValue(select, value) {
  if (value && ![...select.options].some((option) => option.value === value)) {
    select.add(new Option(value, value));
  }
  select.value = value || select.options[0].value;
}

function fillForm(trainer) {
  editingId = trainer.id;
  ['documentNumber', 'phone', 'firstName', 'lastName'].forEach((field) => {
    document.getElementById(field).value = trainer[field] || '';
  });
  selectValue(document.getElementById('specialty'), trainer.specialty);
  selectValue(document.getElementById('workSchedule'), trainer.workSchedule);
  document.getElementById('form-title').textContent = 'Editar entrenador';
  document.getElementById('submit').textContent = 'Guardar cambios';
}

function resetForm() {
  editingId = null;
  form.reset();
  document.getElementById('form-title').textContent = 'Registrar entrenador';
  document.getElementById('submit').textContent = 'Guardar entrenador';
}

async function run(action, text) {
  hideMessage(message);
  try {
    await action();
    showMessage(message, text, 'ok');
    await Promise.all([loadTrainers(), loadPremiumClients()]);
  } catch (error) {
    showMessage(message, error.message);
  }
}

async function start() {
  const user = await initPage('entrenadores');
  isAdmin = user.role === 'ADMIN';
  document.getElementById('admin-cards').hidden = !isAdmin;

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    const data = {};
    fields.forEach((field) => { data[field] = document.getElementById(field).value.trim() || null; });
    const id = editingId;
    run(() => (id ? api.put('/api/trainers/' + id, data) : api.post('/api/trainers', data)).then(resetForm),
      'Entrenador ' + data.firstName + ' ' + data.lastName + ' guardado.');
  });
  document.getElementById('clear').addEventListener('click', () => setTimeout(resetForm));

  document.getElementById('assign-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const membershipId = document.getElementById('premium-membership').value;
    const trainerId = Number(document.getElementById('new-trainer').value);
    if (!membershipId) return;
    run(() => api.patch('/api/memberships/' + membershipId + '/trainer', { trainerId }), 'Entrenador asignado.');
  });

  document.getElementById('trainers').addEventListener('click', (event) => {
    const { edit, deactivate, activate } = event.target.dataset;
    if (edit) {
      fillForm(trainers.find((t) => t.id === Number(edit)));
    } else if (deactivate && confirm('¿Inactivar este entrenador?')) {
      run(() => api.patch('/api/trainers/' + deactivate + '/deactivate'), 'Entrenador inactivado.');
    } else if (activate) {
      run(() => api.patch('/api/trainers/' + activate + '/activate'), 'Entrenador activado.');
    }
  });
  await loadTrainers();
  if (isAdmin) await loadPremiumClients();
}

start().catch((error) => showMessage(message, error.message));
