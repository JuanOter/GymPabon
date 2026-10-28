/** Plans screen (RF06): the three plans with their rules; the administrator updates price and description. */
import {
  api, money, escapeHtml, DURATION_LABELS, showMessage, hideMessage, membershipsByClient,
} from './api.js';
import { initPage } from './layout.js';

const message = document.getElementById('message');
/** Rules of each kind of plan (business rules RN of the Phase 1 document). */
const RULES = {
  BASIC: { access: 'L–V, 5:00 a. m. a 3:00 p. m.', zones: 'Máquinas y pesas', assessment: 'No incluye' },
  STANDARD: { access: 'L–S, todo el horario', zones: 'Máquinas, pesas y clases', assessment: 'Una al inscribirse' },
  PREMIUM: { access: 'Todos los días, todo el horario', zones: 'Todas', assessment: 'Una cada mes' },
};
let plans = [];
let editingId = null;
let isAdmin = false;

function item(label, value) {
  return '<div><dt>' + label + '</dt><dd>' + escapeHtml(value) + '</dd></div>';
}

/** Name of the plan with more clients with an active membership, shown as "Más vendido". */
async function bestSeller(planList) {
  const counts = {};
  membershipsByClient(await api.get('/api/memberships')).forEach(({ main }) => {
    if (main.status === 'ACTIVE') counts[main.planName] = (counts[main.planName] || 0) + 1;
  });
  const byPrice = [...planList].sort((a, b) => a.monthlyPrice - b.monthlyPrice);
  const best = byPrice.reduce((top, plan) => ((counts[plan.name] || 0) > (counts[top.name] || 0) ? plan : top), byPrice[0]);
  return best && counts[best.name] ? best.name : null;
}

function cardOf(plan, best) {
  const rules = RULES[plan.type];
  const isBest = plan.name === best;
  return '<article class="card form' + (isBest ? ' highlight' : '') + '" style="gap:12px">'
    + '<div style="display:flex;justify-content:space-between;align-items:center;gap:8px">'
    + '<h2 style="font-size:24px;font-weight:800">' + escapeHtml(plan.name) + '</h2>'
    + (isBest ? '<span class="tag">Más vendido</span>' : '') + '</div>'
    + '<p class="price">' + money(plan.monthlyPrice) + ' <span>/ mes</span></p>'
    + '<p class="hint" style="margin:0">' + escapeHtml(plan.description) + '</p>'
    + '<dl class="kv">'
    + item('Matrícula', plan.enrollmentFee > 0 ? money(plan.enrollmentFee) : 'Exento')
    + item('Acceso', rules.access) + item('Zonas', rules.zones) + item('Valoración física', rules.assessment)
    + item('Entrenador', plan.includesTrainer ? 'Sí, asignado' : 'No') + '</dl>'
    + (isAdmin ? '<button class="btn2" type="button" style="margin-top:auto" data-edit="' + plan.id
      + '">Editar precio</button>' : '')
    + '</article>';
}

async function loadPlans() {
  plans = await api.get('/api/plans');
  const best = await bestSeller(plans);
  document.getElementById('plans').innerHTML = plans.map((plan) => cardOf(plan, best)).join('');
}

async function loadDurations() {
  const durations = await api.get('/api/memberships/durations');
  const standard = plans.find((p) => p.type === 'STANDARD');
  document.getElementById('durations').innerHTML = durations.map((d) => '<tr><td>' + DURATION_LABELS[d.code]
    + '</td><td>' + Math.round(d.discountRate * 100) + ' %</td><td>'
    + money(standard.monthlyPrice * d.months * (1 - d.discountRate)) + '</td></tr>').join('');
}

function closeEditor() {
  editingId = null;
  document.getElementById('edit-card').hidden = true;
}

async function start() {
  const user = await initPage('planes');
  isAdmin = user.role === 'ADMIN';
  await loadPlans();
  await loadDurations();

  document.getElementById('plans').addEventListener('click', (event) => {
    const id = event.target.dataset.edit;
    if (!id) return;
    const plan = plans.find((p) => p.id === Number(id));
    editingId = plan.id;
    document.getElementById('edit-title').textContent = 'Editar plan ' + plan.name;
    document.getElementById('monthlyPrice').value = plan.monthlyPrice;
    document.getElementById('description').value = plan.description;
    document.getElementById('edit-card').hidden = false;
    document.getElementById('monthlyPrice').focus();
  });
  document.getElementById('cancel').addEventListener('click', closeEditor);
  document.getElementById('plan-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    hideMessage(message);
    try {
      const updated = await api.put('/api/plans/' + editingId, {
        monthlyPrice: Number(document.getElementById('monthlyPrice').value),
        description: document.getElementById('description').value,
      });
      showMessage(message, 'Plan ' + updated.name + ' actualizado.', 'ok');
      closeEditor();
      await loadPlans();
      await loadDurations();
    } catch (error) {
      showMessage(message, error.message);
    }
  });
}

start().catch((error) => showMessage(message, error.message));
