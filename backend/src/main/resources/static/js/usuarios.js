/** Users screen (RF02, administrator only): create, edit, activate and deactivate staff accounts. */
import { api, escapeHtml, badge, date, time, ROLE_LABELS, showMessage, hideMessage } from './api.js';
import { initPage } from './layout.js';

const message = document.getElementById('message');
const form = document.getElementById('user-form');
const fields = ['firstName', 'lastName', 'documentNumber', 'username', 'role', 'temporaryPassword'];
let users = [];
let editingId = null;
let me = null;

function lastLogin(iso) {
  return iso ? date(iso) + ' · ' + time(iso) : '—';
}

function actionsFor(user) {
  const edit = '<button class="link" data-edit="' + user.id + '">Editar</button>';
  if (user.id === me.id) return edit;
  return user.active
    ? edit + ' · <button class="link danger" data-deactivate="' + user.id + '">Desactivar</button>'
    : '<button class="link" data-activate="' + user.id + '">Activar</button>';
}

async function loadUsers() {
  users = (await api.get('/api/users'))
    .sort((a, b) => (a.role === b.role ? a.username.localeCompare(b.username) : a.role === 'ADMIN' ? -1 : 1));
  document.getElementById('users').innerHTML = users.map((u) => '<tr>'
    + '<td>' + escapeHtml(u.username) + '</td><td class="wrap">' + escapeHtml(u.fullName) + '</td>'
    + '<td>' + ROLE_LABELS[u.role] + '</td><td class="wrap">' + lastLogin(u.lastLogin) + '</td>'
    + '<td>' + (u.active ? badge('Activo', 'green') : badge('Desactivado', 'gray')) + '</td>'
    + '<td>' + actionsFor(u) + '</td></tr>').join('');
}

function resetForm() {
  editingId = null;
  form.reset();
  document.getElementById('form-title').textContent = 'Crear usuario';
  document.getElementById('submit').textContent = 'Crear usuario';
  document.getElementById('username').disabled = false;
  document.getElementById('password-field').hidden = false;
}

function fillForm(user) {
  editingId = user.id;
  fields.forEach((field) => { document.getElementById(field).value = user[field] || ''; });
  document.getElementById('form-title').textContent = 'Editar usuario';
  document.getElementById('submit').textContent = 'Guardar cambios';
  document.getElementById('username').disabled = true;
  document.getElementById('password-field').hidden = true;
}

async function save(event) {
  event.preventDefault();
  hideMessage(message);
  const data = {};
  fields.forEach((field) => { data[field] = document.getElementById(field).value.trim() || null; });
  try {
    let saved;
    if (editingId) {
      const current = users.find((u) => u.id === editingId);
      saved = await api.put('/api/users/' + editingId, { ...data, phone: current.phone, email: current.email });
    } else {
      saved = await api.post('/api/users', data);
    }
    showMessage(message, 'Usuario ' + saved.username + ' guardado.', 'ok');
    resetForm();
    await loadUsers();
  } catch (error) {
    showMessage(message, error.message);
  }
}

async function start() {
  me = await initPage('usuarios', true);
  form.addEventListener('submit', save);
  document.getElementById('clear').addEventListener('click', () => setTimeout(resetForm));
  document.getElementById('users').addEventListener('click', async (event) => {
    const { edit, deactivate, activate } = event.target.dataset;
    try {
      if (edit) {
        fillForm(users.find((u) => u.id === Number(edit)));
      } else if (deactivate && confirm('¿Desactivar esta cuenta?')) {
        await api.patch('/api/users/' + deactivate + '/deactivate');
        showMessage(message, 'Cuenta desactivada.', 'ok');
        await loadUsers();
      } else if (activate) {
        await api.patch('/api/users/' + activate + '/activate');
        showMessage(message, 'Cuenta activada.', 'ok');
        await loadUsers();
      }
    } catch (error) {
      showMessage(message, error.message);
    }
  });
  await loadUsers();
}

start().catch((error) => { if (error.message !== 'Solo administrador') showMessage(message, error.message); });
