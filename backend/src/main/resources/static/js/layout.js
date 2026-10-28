/**
 * Draws the side menu of every screen with the options of the user's role and
 * handles signing out. Each page calls initPage() first.
 */
import { api, escapeHtml, ROLE_LABELS } from './api.js';

const MENU = [
  { page: 'inicio', label: 'Inicio' },
  { page: 'clientes', label: 'Clientes' },
  { page: 'entrenadores', label: 'Entrenadores' },
  { page: 'planes', label: 'Planes' },
  { page: 'membresias', label: 'Membresías' },
  { page: 'pagos', label: 'Pagos' },
  { page: 'asistencia', label: 'Asistencia' },
  { page: 'reportes', label: 'Reportes', adminOnly: true },
  { page: 'usuarios', label: 'Usuarios', adminOnly: true },
];

const LOGO = '<svg aria-hidden="true" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="#FB923C" '
  + 'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 7v10M3 9v6M18 7v10M21 9v6M6 12h12"/></svg>';

/**
 * Loads the signed-in user and draws the menu. If the page is only for the
 * administrator and the user is not one, it shows a notice instead of the content.
 *
 * @param {string} activePage name of the current page, as in MENU
 * @param {boolean} adminOnly whether only the administrator can open the page
 * @returns {Promise<object>} the signed-in user
 */
export async function initPage(activePage, adminOnly = false) {
  const user = await api.get('/api/auth/me');
  const isAdmin = user.role === 'ADMIN';
  const links = MENU
    .filter((item) => isAdmin || !item.adminOnly)
    .map((item) => '<a class="nav' + (item.page === activePage ? ' on" aria-current="page' : '') + '" href="'
      + item.page + '.html">' + item.label + '</a>')
    .join('');

  document.getElementById('sidebar').innerHTML = '<div class="brand">' + LOGO + 'GymPabon</div>' + links
    + '<div class="session"><strong>' + escapeHtml(user.fullName) + '</strong><small>'
    + ROLE_LABELS[user.role] + '</small><button type="button" id="logout">Cerrar sesión</button></div>';

  document.getElementById('logout').addEventListener('click', async () => {
    await api.post('/api/auth/logout');
    location.href = 'login.html';
  });

  if (adminOnly && !isAdmin) {
    document.querySelector('main').innerHTML = '<div class="msg error">Esta pantalla es solo para el administrador.</div>';
    throw new Error('Solo administrador');
  }
  return user;
}
