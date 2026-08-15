// ============================================================
// Config
// ============================================================
const API_BASE = 'http://localhost:8080/api/v1';

// ============================================================
// Estado de sesion (persistido en localStorage para sobrevivir a un refresh)
// ============================================================
const Session = {
  get token() { return localStorage.getItem('tv_token'); },
  get username() { return localStorage.getItem('tv_username'); },
  get rol() { return localStorage.getItem('tv_rol'); },
  save(token, username, rol) {
    localStorage.setItem('tv_token', token);
    localStorage.setItem('tv_username', username);
    localStorage.setItem('tv_rol', rol);
  },
  clear() {
    localStorage.removeItem('tv_token');
    localStorage.removeItem('tv_username');
    localStorage.removeItem('tv_rol');
  },
  isLoggedIn() { return !!this.token; }
};

// ============================================================
// Cliente API: agrega el Bearer token, parsea el ErrorResponse consistente del backend
// ============================================================
async function api(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (Session.token) headers['Authorization'] = `Bearer ${Session.token}`;

  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined
    });
  } catch (networkErr) {
    throw new Error('No se pudo conectar con el backend. ¿Esta corriendo en http://localhost:8080?');
  }

  if (res.status === 204) return null;

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    // Formato consistente del backend: { timestamp, path, error, message, details? }
    const msg = data?.details?.length ? data.details.join(' · ') : (data?.message || `Error ${res.status}`);
    throw new Error(msg);
  }

  return data;
}

// ============================================================
// UI helpers
// ============================================================
function showBanner(message, type = 'ok') {
  const el = document.getElementById('banner');
  el.textContent = message;
  el.className = `banner ${type}`;
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 4500);
}

function estadoBadge(estado) {
  return `<span class="badge badge-${estado}">${estado}</span>`;
}

// Previene XSS: cualquier dato que venga del backend (nombre de cliente, motivo de rechazo,
// username, etc.) pasa por aqui antes de insertarse con innerHTML. Sin esto, alguien podria
// registrar una venta con nombre_cliente = "<img src=x onerror=alert(1)>" y ese HTML se
// ejecutaria literalmente para cualquiera que vea la tabla.
function esc(str) {
  if (str === null || str === undefined) return '';
  const div = document.createElement('div');
  div.textContent = String(str);
  return div.innerHTML;
}

function fmtFecha(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }) +
      ' ' + d.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' });
}

function fmtMonto(m) {
  return 'S/ ' + Number(m).toFixed(2);
}

function renderPaginacion(containerId, page, onChange) {
  const el = document.getElementById(containerId);
  const { number, totalPages } = page;
  el.innerHTML = `
    <button class="btn btn-ghost btn-sm" ${number <= 0 ? 'disabled' : ''} id="${containerId}-prev">← Anterior</button>
    <span>Página ${totalPages === 0 ? 0 : number + 1} de ${totalPages}</span>
    <button class="btn btn-ghost btn-sm" ${number + 1 >= totalPages ? 'disabled' : ''} id="${containerId}-next">Siguiente →</button>
  `;
  if (number > 0) document.getElementById(`${containerId}-prev`).onclick = () => onChange(number - 1);
  if (number + 1 < totalPages) document.getElementById(`${containerId}-next`).onclick = () => onChange(number + 1);
}

// ============================================================
// Login
// ============================================================
document.getElementById('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const errorEl = document.getElementById('login-error');
  errorEl.classList.add('hidden');

  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;

  try {
    const data = await api('/auth/login', { method: 'POST', body: { username, password } });
    Session.save(data.token, data.username, data.rol);
    bootApp();
  } catch (err) {
    errorEl.textContent = err.message;
    errorEl.classList.remove('hidden');
  }
});

document.getElementById('logout-btn').addEventListener('click', () => {
  Session.clear();
  document.getElementById('app-screen').classList.add('hidden');
  document.getElementById('login-screen').classList.remove('hidden');
  document.getElementById('login-form').reset();
});

// ============================================================
// Boot: decide que ver segun el rol logueado
// ============================================================
const ROLE_VIEWS = {
  AGENTE: [{ id: 'agente', label: 'Mis ventas' }],
  BACKOFFICE: [{ id: 'backoffice', label: 'Pendientes' }],
  SUPERVISOR: [{ id: 'supervisor', label: 'Equipo y resumen' }],
  ADMIN: [{ id: 'admin', label: 'Usuarios' }]
};

function bootApp() {
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('app-screen').classList.remove('hidden');
  document.getElementById('user-label').textContent = `${Session.username} · ${Session.rol}`;

  const tabsEl = document.getElementById('tabs');
  const views = ROLE_VIEWS[Session.rol] || [];
  tabsEl.innerHTML = views.map((v, i) =>
      `<button class="tab-btn ${i === 0 ? 'active' : ''}" data-view="${v.id}">${v.label}</button>`
  ).join('');

  tabsEl.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => switchView(btn.dataset.view));
  });

  if (views.length) {
    switchView(views[0].id);
  } else {
    showBanner('Tu rol no tiene una vista asignada en este panel.', 'err');
  }
}

function switchView(viewId) {
  document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.view === viewId));
  document.getElementById(`view-${viewId}`).classList.remove('hidden');

  if (viewId === 'agente') cargarMisVentas();
  if (viewId === 'backoffice') cargarPendientes();
  if (viewId === 'supervisor') { cargarAgentesEquipo(); cargarEquipo(); cargarResumen(); }
  if (viewId === 'admin') { cargarSupervisoresParaFormulario(); cargarUsuarios(); }
}

// Sub-pestañas dentro de la vista de agente (Registrar venta / Mis ventas)
document.querySelectorAll('.subtab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    const target = btn.dataset.subview;
    document.querySelectorAll('.subtab-btn').forEach(b => b.classList.toggle('active', b === btn));
    document.querySelectorAll('.subview').forEach(v => v.classList.add('hidden'));
    document.getElementById(`subview-${target}`).classList.remove('hidden');
    if (target === 'ag-lista') cargarMisVentas();
  });
});

// ============================================================
// ==================  VISTA AGENTE  =========================
// ============================================================
document.getElementById('venta-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const body = {
    dniCliente: document.getElementById('f-dni').value.trim(),
    nombreCliente: document.getElementById('f-nombre').value.trim(),
    telefonoCliente: document.getElementById('f-telefono').value.trim(),
    direccionCliente: document.getElementById('f-direccion').value.trim(),
    planActual: document.getElementById('f-plan-actual').value.trim() || null,
    planNuevo: document.getElementById('f-plan-nuevo').value.trim(),
    codigoLlamada: document.getElementById('f-codigo').value.trim(),
    producto: 'FIJA_HOGAR',
    monto: parseFloat(document.getElementById('f-monto').value)
  };

  try {
    await api('/ventas', { method: 'POST', body });
    showBanner('Venta registrada correctamente, queda en estado PENDIENTE.', 'ok');
    e.target.reset();
    agentePage = 0;
    cargarMisVentas();
  } catch (err) {
    showBanner(err.message, 'err');
  }
});

document.getElementById('ag-filtrar-btn').addEventListener('click', () => {
  agentePage = 0;
  cargarMisVentas();
});

let agentePage = 0;

async function cargarMisVentas() {
  const estado = document.getElementById('ag-filtro-estado').value;
  const desde = document.getElementById('ag-filtro-desde').value;
  const hasta = document.getElementById('ag-filtro-hasta').value;

  const params = new URLSearchParams({ page: agentePage, size: 8, sort: 'fechaRegistro,desc' });
  if (estado) params.set('estado', estado);
  if (desde) params.set('desde', desde);
  if (hasta) params.set('hasta', hasta);

  try {
    const page = await api(`/ventas/mis-ventas?${params}`);
    renderTablaVentas('ag-tabla-wrap', page.content, { showAgente: false, showAcciones: false });
    renderPaginacion('ag-paginacion', page, (p) => { agentePage = p; cargarMisVentas(); });
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

// ============================================================
// ================  VISTA BACKOFFICE  ========================
// ============================================================
document.getElementById('bo-refrescar-btn').addEventListener('click', () => { boPage = 0; cargarPendientes(); });

let boPage = 0;
let rechazoVentaId = null;

async function cargarPendientes() {
  const params = new URLSearchParams({ page: boPage, size: 8, sort: 'fechaRegistro,asc' });
  try {
    const page = await api(`/ventas/pendientes?${params}`);
    renderTablaVentas('bo-tabla-wrap', page.content, { showAgente: true, showAcciones: true });
    renderPaginacion('bo-paginacion', page, (p) => { boPage = p; cargarPendientes(); });

    document.querySelectorAll('[data-aprobar]').forEach(btn => {
      btn.addEventListener('click', () => aprobarVenta(btn.dataset.aprobar));
    });
    document.querySelectorAll('[data-rechazar]').forEach(btn => {
      btn.addEventListener('click', () => abrirModalRechazo(btn.dataset.rechazar));
    });
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

async function aprobarVenta(id) {
  try {
    await api(`/ventas/${id}/aprobar`, { method: 'POST' });
    showBanner(`Venta #${id} aprobada.`, 'ok');
    cargarPendientes();
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

function abrirModalRechazo(id) {
  rechazoVentaId = id;
  document.getElementById('modal-motivo').value = '';
  document.getElementById('modal-rechazo').classList.remove('hidden');
}

document.getElementById('modal-cancelar').addEventListener('click', () => {
  document.getElementById('modal-rechazo').classList.add('hidden');
  rechazoVentaId = null;
});

document.getElementById('modal-confirmar').addEventListener('click', async () => {
  const motivo = document.getElementById('modal-motivo').value.trim();
  if (!motivo) {
    showBanner('El motivo de rechazo es obligatorio.', 'err');
    return;
  }
  try {
    await api(`/ventas/${rechazoVentaId}/rechazar`, { method: 'POST', body: { motivoRechazo: motivo } });
    showBanner(`Venta #${rechazoVentaId} rechazada.`, 'ok');
    document.getElementById('modal-rechazo').classList.add('hidden');
    rechazoVentaId = null;
    cargarPendientes();
  } catch (err) {
    showBanner(err.message, 'err');
  }
});

// ============================================================
// ================  VISTA SUPERVISOR  ========================
// ============================================================
document.getElementById('sup-filtrar-btn').addEventListener('click', () => { supPage = 0; cargarEquipo(); });
document.getElementById('sup-resumen-btn').addEventListener('click', () => cargarResumen());

let supPage = 0;

async function cargarAgentesEquipo() {
  const select = document.getElementById('sup-filtro-agente');
  const valorPrevio = select.value;

  try {
    const agentes = await api('/usuarios/mi-equipo');
    select.innerHTML = `<option value="">Todos los agentes</option>` +
        agentes.map(a => `<option value="${a.id}">${esc(a.username)}</option>`).join('');
    // Conserva la seleccion si el agente sigue en la lista tras recargar
    if ([...select.options].some(o => o.value === valorPrevio)) {
      select.value = valorPrevio;
    }
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

async function cargarEquipo() {
  const estado = document.getElementById('sup-filtro-estado').value;
  const agenteId = document.getElementById('sup-filtro-agente').value;
  const desde = document.getElementById('sup-filtro-desde').value;
  const hasta = document.getElementById('sup-filtro-hasta').value;

  const params = new URLSearchParams({ page: supPage, size: 8, sort: 'fechaRegistro,desc' });
  if (estado) params.set('estado', estado);
  if (agenteId) params.set('agenteId', agenteId);
  if (desde) params.set('desde', desde);
  if (hasta) params.set('hasta', hasta);

  try {
    const page = await api(`/ventas/equipo?${params}`);
    renderTablaVentas('sup-tabla-wrap', page.content, { showAgente: true, showAcciones: false });
    renderPaginacion('sup-paginacion', page, (p) => { supPage = p; cargarEquipo(); });
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

async function cargarResumen() {
  const mes = document.getElementById('sup-mes').value; // formato YYYY-MM del <input type=month>
  const params = new URLSearchParams();
  if (mes) params.set('mes', mes);

  try {
    const resumen = await api(`/reportes/resumen?${params}`);

    const cardsEl = document.getElementById('sup-resumen-cards');
    const conteos = resumen.conteosPorEstado || {};
    cardsEl.innerHTML = `
      <div class="stat-card"><div class="stat-label">Pendientes</div><div class="stat-value">${conteos.PENDIENTE ?? 0}</div></div>
      <div class="stat-card"><div class="stat-label">Aprobadas</div><div class="stat-value">${conteos.APROBADA ?? 0}</div></div>
      <div class="stat-card"><div class="stat-label">Rechazadas</div><div class="stat-value">${conteos.RECHAZADA ?? 0}</div></div>
      <div class="stat-card"><div class="stat-label">Monto aprobadas</div><div class="stat-value">${fmtMonto(resumen.montoTotalAprobadas || 0)}</div></div>
    `;

    const serie = resumen.ventasPorDia || [];
    const serieEl = document.getElementById('sup-serie-wrap');
    if (!serie.length) {
      serieEl.innerHTML = `<p class="empty-row">Sin ventas registradas en este período.</p>`;
    } else {
      serieEl.innerHTML = `
        <table>
          <thead><tr><th>Fecha</th><th>Cantidad</th><th>Monto</th></tr></thead>
          <tbody>
            ${serie.map(s => `
              <tr>
                <td class="mono">${s.fecha}</td>
                <td>${s.cantidad}</td>
                <td class="mono">${fmtMonto(s.monto)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      `;
    }
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

// ============================================================
// Tabla de ventas compartida entre las 3 vistas
// ============================================================
function renderTablaVentas(containerId, ventas, { showAgente, showAcciones }) {
  const el = document.getElementById(containerId);

  if (!ventas.length) {
    el.innerHTML = `<p class="empty-row">No hay ventas para mostrar con estos filtros.</p>`;
    return;
  }

  el.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>Código</th>
          ${showAgente ? '<th>Agente</th>' : ''}
          <th>Cliente</th>
          <th>Plan nuevo</th>
          <th>Monto</th>
          <th>Estado</th>
          <th>Fecha registro</th>
          ${showAcciones ? '<th>Acciones</th>' : ''}
        </tr>
      </thead>
      <tbody>
        ${ventas.map(v => `
          <tr>
            <td class="mono">${esc(v.codigoLlamada)}</td>
            ${showAgente ? `<td>${esc(v.agenteUsername)}</td>` : ''}
            <td>${esc(v.nombreCliente)}</td>
            <td>${esc(v.planNuevo)}</td>
            <td class="mono">${fmtMonto(v.monto)}</td>
            <td>${estadoBadge(v.estado)}${v.estado === 'RECHAZADA' && v.motivoRechazo ? `<div style="font-size:11px;color:var(--text-500);margin-top:4px;">${esc(v.motivoRechazo)}</div>` : ''}</td>
            <td class="mono" style="font-size:11.5px;">${fmtFecha(v.fechaRegistro)}</td>
            ${showAcciones ? `
              <td class="row-actions">
                <button class="btn btn-secondary btn-sm" data-aprobar="${v.id}">Aprobar</button>
                <button class="btn btn-danger btn-sm" data-rechazar="${v.id}">Rechazar</button>
              </td>
            ` : ''}
          </tr>
        `).join('')}
      </tbody>
    </table>
  `;
}

// ============================================================
// Arranque: si ya hay sesion guardada (refresh de pagina), entra directo
// ============================================================
if (Session.isLoggedIn()) {
  bootApp();
}

// ============================================================
// ==================  VISTA ADMIN  ===========================
// ============================================================

// El campo "supervisor" solo tiene sentido cuando el rol elegido es AGENTE
document.getElementById('u-rol').addEventListener('change', (e) => {
  document.getElementById('u-supervisor-field').style.display = e.target.value === 'AGENTE' ? '' : 'none';
});

document.getElementById('adm-refrescar-btn').addEventListener('click', cargarUsuarios);

document.getElementById('usuario-form').addEventListener('submit', async (e) => {
  e.preventDefault();

  const rol = document.getElementById('u-rol').value;
  const supervisorId = document.getElementById('u-supervisor').value;

  if (rol === 'AGENTE' && !supervisorId) {
    showBanner('Un usuario con rol Agente debe tener un supervisor asignado.', 'err');
    return;
  }

  const body = {
    username: document.getElementById('u-username').value.trim(),
    password: document.getElementById('u-password').value,
    rol,
    supervisorId: (rol === 'AGENTE' && supervisorId) ? Number(supervisorId) : null
  };

  try {
    const creado = await api('/usuarios', { method: 'POST', body });
    showBanner(`Usuario "${creado.username}" (${creado.rol}) creado correctamente.`, 'ok');
    e.target.reset();
    document.getElementById('u-supervisor-field').style.display = '';
    cargarUsuarios();
    cargarSupervisoresParaFormulario();
  } catch (err) {
    showBanner(err.message, 'err');
  }
});

async function cargarSupervisoresParaFormulario() {
  try {
    const usuarios = await api('/usuarios');
    const supervisores = usuarios.filter(u => u.rol === 'SUPERVISOR');
    const select = document.getElementById('u-supervisor');
    select.innerHTML = `<option value="">Sin asignar</option>` +
        supervisores.map(s => `<option value="${s.id}">${esc(s.username)}</option>`).join('');
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

async function cargarUsuarios() {
  try {
    const usuarios = await api('/usuarios');
    const el = document.getElementById('adm-tabla-wrap');

    if (!usuarios.length) {
      el.innerHTML = `<p class="empty-row">No hay usuarios registrados.</p>`;
      return;
    }

    el.innerHTML = `
      <table>
        <thead>
          <tr>
            <th>Username</th>
            <th>Rol</th>
            <th>Supervisor</th>
            <th>Activo</th>
            <th>Creado</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          ${usuarios.map(u => `
            <tr>
              <td class="mono">${esc(u.username)}</td>
              <td>${u.rol}</td>
              <td>${esc(u.supervisorUsername) || '—'}</td>
              <td>${u.activo ? 'Sí' : 'No'}</td>
              <td class="mono" style="font-size:11.5px;">${fmtFecha(u.createdAt)}</td>
              <td class="row-actions">
                <button class="btn btn-secondary btn-sm" data-editar='${JSON.stringify(u)}'>Editar</button>
                <button class="btn btn-ghost btn-sm" data-clave="${u.id}" data-clave-username="${esc(u.username)}">Clave</button>
                <button class="btn ${u.activo ? 'btn-danger' : 'btn-secondary'} btn-sm" data-toggle-estado="${u.id}" data-activo="${u.activo}">${u.activo ? 'Desactivar' : 'Activar'}</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;

    document.querySelectorAll('[data-editar]').forEach(btn => {
      btn.addEventListener('click', () => abrirModalEditar(JSON.parse(btn.dataset.editar)));
    });
    document.querySelectorAll('[data-clave]').forEach(btn => {
      btn.addEventListener('click', () => abrirModalPassword(btn.dataset.clave, btn.dataset.claveUsername));
    });
    document.querySelectorAll('[data-toggle-estado]').forEach(btn => {
      btn.addEventListener('click', () => toggleEstadoUsuario(btn.dataset.toggleEstado, btn.dataset.activo === 'true'));
    });
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

// ---------- Editar usuario ----------
let usuarioEditandoId = null;

function abrirModalEditar(usuario) {
  usuarioEditandoId = usuario.id;
  document.getElementById('edit-username').value = usuario.username;
  document.getElementById('edit-rol').value = usuario.rol;
  document.getElementById('edit-supervisor-field').style.display = usuario.rol === 'AGENTE' ? '' : 'none';
  cargarSupervisoresEnSelect('edit-supervisor', usuario.supervisorId);
  document.getElementById('modal-editar-usuario').classList.remove('hidden');
}

document.getElementById('edit-rol').addEventListener('change', (e) => {
  document.getElementById('edit-supervisor-field').style.display = e.target.value === 'AGENTE' ? '' : 'none';
});

document.getElementById('modal-editar-cancelar').addEventListener('click', () => {
  document.getElementById('modal-editar-usuario').classList.add('hidden');
  usuarioEditandoId = null;
});

document.getElementById('modal-editar-confirmar').addEventListener('click', async () => {
  const rol = document.getElementById('edit-rol').value;
  const supervisorId = document.getElementById('edit-supervisor').value;

  if (rol === 'AGENTE' && !supervisorId) {
    showBanner('Un usuario con rol Agente debe tener un supervisor asignado.', 'err');
    return;
  }

  const body = {
    username: document.getElementById('edit-username').value.trim(),
    rol,
    supervisorId: (rol === 'AGENTE' && supervisorId) ? Number(supervisorId) : null
  };

  try {
    await api(`/usuarios/${usuarioEditandoId}`, { method: 'PUT', body });
    showBanner('Usuario actualizado correctamente.', 'ok');
    document.getElementById('modal-editar-usuario').classList.add('hidden');
    usuarioEditandoId = null;
    cargarUsuarios();
    cargarSupervisoresParaFormulario();
  } catch (err) {
    showBanner(err.message, 'err');
  }
});

async function cargarSupervisoresEnSelect(selectId, valorSeleccionado) {
  try {
    const usuarios = await api('/usuarios');
    const supervisores = usuarios.filter(u => u.rol === 'SUPERVISOR');
    const select = document.getElementById(selectId);
    select.innerHTML = `<option value="">Sin asignar</option>` +
        supervisores.map(s => `<option value="${s.id}">${esc(s.username)}</option>`).join('');
    if (valorSeleccionado) select.value = String(valorSeleccionado);
  } catch (err) {
    showBanner(err.message, 'err');
  }
}

// ---------- Cambiar contraseña ----------
let usuarioCambiandoPasswordId = null;

function abrirModalPassword(id, username) {
  usuarioCambiandoPasswordId = id;
  document.getElementById('password-username-label').textContent = username;
  document.getElementById('modal-password-valor').value = '';
  document.getElementById('modal-password').classList.remove('hidden');
}

document.getElementById('modal-password-cancelar').addEventListener('click', () => {
  document.getElementById('modal-password').classList.add('hidden');
  usuarioCambiandoPasswordId = null;
});

document.getElementById('modal-password-confirmar').addEventListener('click', async () => {
  const nuevaPassword = document.getElementById('modal-password-valor').value;
  if (nuevaPassword.length < 6) {
    showBanner('La contraseña debe tener al menos 6 caracteres.', 'err');
    return;
  }
  try {
    await api(`/usuarios/${usuarioCambiandoPasswordId}/password`, { method: 'PATCH', body: { password: nuevaPassword } });
    showBanner('Contraseña actualizada correctamente.', 'ok');
    document.getElementById('modal-password').classList.add('hidden');
    usuarioCambiandoPasswordId = null;
  } catch (err) {
    showBanner(err.message, 'err');
  }
});

// ---------- Activar / desactivar ----------
async function toggleEstadoUsuario(id, activoActual) {
  const nuevoEstado = !activoActual;
  const verbo = nuevoEstado ? 'activar' : 'desactivar';
  if (!confirm(`¿Seguro que quieres ${verbo} este usuario?`)) return;

  try {
    await api(`/usuarios/${id}/estado`, { method: 'PATCH', body: { activo: nuevoEstado } });
    showBanner(`Usuario ${nuevoEstado ? 'activado' : 'desactivado'} correctamente.`, 'ok');
    cargarUsuarios();
  } catch (err) {
    showBanner(err.message, 'err');
  }
}