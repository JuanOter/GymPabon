/** Sign-in screen: checks the credentials against the API and opens the main panel. */
import { api, showMessage } from './api.js';

const form = document.getElementById('login-form');
const message = document.getElementById('message');
const submit = document.getElementById('submit');

// If there is already an open session, go straight to the main panel.
api.get('/api/auth/me').then(() => { location.href = 'inicio.html'; }).catch(() => {});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  if (!username || !password) {
    showMessage(message, 'Escribe el usuario y la contraseña');
    return;
  }
  submit.disabled = true;
  try {
    await api.post('/api/auth/login', { username, password });
    location.href = 'inicio.html';
  } catch (error) {
    showMessage(message, error.message);
  } finally {
    submit.disabled = false;
  }
});
