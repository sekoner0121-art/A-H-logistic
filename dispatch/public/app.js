'use strict';

(() => {
  const app = document.getElementById('app');
  const modalRoot = document.getElementById('modal-root');
  const toastRoot = document.getElementById('toast-root');
  const AGS_CENTER = [21.8818, -102.2916];
  const STATUS = {
    pending: { label: 'Pendiente', className: 'pending' },
    accepted: { label: 'Aceptado', className: 'accepted' },
    en_route: { label: 'En camino', className: 'en-route' },
    completed: { label: 'Entregado', className: 'completed' },
    cancelled: { label: 'Cancelado', className: 'cancelled' },
  };

  let currentUser = null;
  let snapshot = { drivers: [], orders: [] };
  let pollingTimer = null;
  let refreshInFlight = false;
  let controlMap = null;
  let driverMap = null;
  let controlMarkers = new Map();
  let driverMarker = null;
  let tracking = false;
  let watchId = null;
  let trackingTimer = null;
  let lastPosition = null;
  let lastToastTimer = null;

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>"']/g, char => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
    })[char]);
  }

  function shortTime(value) {
    if (!value) return 'Sin registro';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Sin registro';
    return new Intl.DateTimeFormat('es-MX', { hour: '2-digit', minute: '2-digit' }).format(date);
  }

  function relativeTime(value) {
    if (!value) return 'Aún sin ubicación';
    const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
    if (seconds < 10) return 'Actualizado ahora';
    if (seconds < 60) return 'Hace ' + seconds + ' s';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return 'Hace ' + minutes + ' min';
    return 'Hace ' + Math.floor(minutes / 60) + ' h';
  }

  function toast(message, kind = 'info') {
    toastRoot.innerHTML = '<div class="toast toast-' + escapeHtml(kind) + '">' + escapeHtml(message) + '</div>';
    clearTimeout(lastToastTimer);
    lastToastTimer = setTimeout(() => { toastRoot.innerHTML = ''; }, 3800);
  }

  async function api(url, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'same-origin',
      cache: 'no-store',
    });
    let result = {};
    try { result = await response.json(); } catch {}
    if (!response.ok) {
      const error = new Error(result.error || 'No se pudo completar la solicitud.');
      error.status = response.status;
      error.mustChangePassword = Boolean(result.mustChangePassword);
      throw error;
    }
    return result;
  }

  function roleFromPath() {
    if (location.pathname === '/control') return 'control';
    if (location.pathname === '/conductor') return 'driver';
    return 'choose';
  }

  function goToRole(role) {
    location.assign(role === 'control' ? '/control' : '/conductor');
  }

  function renderChooser() {
    app.innerHTML = [
      '<main class="landing">',
      '  <div class="landing-brand"><span class="brand-mark">A<span>&</span>H</span><span>A&H <b>LOGÍSTICA</b></span></div>',
      '  <div class="landing-copy"><div class="eyebrow">CENTRO DE OPERACIONES · AGUASCALIENTES</div>',
      '    <h1>La operación, <em>en movimiento.</em></h1>',
      '    <p>Elige tu espacio de trabajo para iniciar sesión.</p></div>',
      '  <div class="role-cards">',
      '    <a class="role-card" href="/control"><span class="role-icon">⌖</span><span class="role-meta"><b>Torre de control</b><small>Despacha servicios y sigue a tus conductores.</small></span><span class="role-arrow">↗</span></a>',
      '    <a class="role-card" href="/conductor"><span class="role-icon driver-icon">↗</span><span class="role-meta"><b>Conductor</b><small>Recibe órdenes y comparte tu ubicación.</small></span><span class="role-arrow">↗</span></a>',
      '  </div>',
      '  <div class="landing-foot">A&H Logística <span>·</span> Aguascalientes, Ags.</div>',
      '</main>',
    ].join('');
  }

  function renderLogin(role) {
    const isControl = role === 'control';
    app.innerHTML = [
      '<main class="login-shell">',
      '  <a class="back-link" href="/">← <span>Inicio</span></a>',
      '  <section class="login-card">',
      '    <div class="brand-mark login-mark">A<span>&</span>H</div>',
      '    <div class="eyebrow">' + (isControl ? 'CENTRO DE OPERACIONES' : 'APP DEL CONDUCTOR') + '</div>',
      '    <h1>' + (isControl ? 'Bienvenido al turno.' : 'Listo para salir.') + '</h1>',
      '    <p class="login-description">' + (isControl ? 'Accede para asignar y dar seguimiento a los servicios.' : 'Accede para recibir tu ruta y compartir tu ubicación.') + '</p>',
      '    <form id="login-form" class="form-stack">',
      '      <label>Usuario<input name="username" autocomplete="username" required maxlength="40" placeholder="' + (isControl ? 'control1' : 'conductor1') + '"></label>',
      '      <label>Contraseña<input name="password" type="password" autocomplete="current-password" required></label>',
      '      <div id="login-error" class="form-error" role="alert"></div>',
      '      <button class="button button-primary button-wide" type="submit">Entrar <span>→</span></button>',
      '    </form>',
      '    <div class="login-note"><span class="secure-dot"></span> Acceso privado para el equipo de A&H</div>',
      '  </section>',
      '  <div class="login-bottom">A&H Logística <span>·</span> Aguascalientes, Ags.</div>',
      '</main>',
    ].join('');
    const username = app.querySelector('[name="username"]');
    if (username && role === 'driver') username.value = '';
  }

  function renderSetup() {
    app.innerHTML = [
      '<main class="login-shell">',
      '  <a class="back-link" href="/">← <span>Inicio</span></a>',
      '  <section class="login-card setup-card">',
      '    <div class="brand-mark login-mark">A<span>&</span>H</div>',
      '    <div class="eyebrow">CONFIGURACIÓN INICIAL · UNA SOLA VEZ</div>',
      '    <h1>Activa el equipo.</h1>',
      '    <p class="login-description">Usa el código de instalación que configuraste en el hosting. Crea las tres cuentas privadas para logística y los conductores.</p>',
      '    <form id="setup-form" class="form-stack">',
      '      <label>Código de instalación<input name="setupCode" type="password" autocomplete="off" required maxlength="128"></label>',
      '      <label>Contraseña de logística <small>Usuario: control1</small><input name="controlPassword" type="password" autocomplete="new-password" minlength="12" required></label>',
      '      <label>Contraseña de conductor 1 <small>Usuario: conductor1</small><input name="driver1Password" type="password" autocomplete="new-password" minlength="12" required></label>',
      '      <label>Contraseña de conductor 2 <small>Usuario: conductor2</small><input name="driver2Password" type="password" autocomplete="new-password" minlength="12" required></label>',
      '      <div class="form-error" id="setup-error" role="alert"></div>',
      '      <button class="button button-primary button-wide" type="submit">Crear las cuentas <span>→</span></button>',
      '    </form>',
      '    <div class="login-note"><span class="secure-dot"></span> El código deja de servir después de crear las cuentas.</div>',
      '  </section>',
      '  <div class="login-bottom">A&H Logística <span>·</span> Aguascalientes, Ags.</div>',
      '</main>',
    ].join('');
  }

  function renderUnavailable() {
    app.innerHTML = [
      '<main class="login-shell"><section class="login-card">',
      '<div class="brand-mark login-mark">A<span>&</span>H</div>',
      '<div class="eyebrow">A&H LOGÍSTICA · AGUASCALIENTES</div>',
      '<h1>No se pudo conectar.</h1>',
      '<p class="login-description">El servicio todavía no está configurado o no responde. Revisa la configuración del hosting y vuelve a cargar esta página.</p>',
      '<button class="button button-primary button-wide" type="button" data-action="reload">Volver a intentar <span>↻</span></button>',
      '</section></main>',
    ].join('');
  }

  const CONTROL_SHELL = [
    '<div class="shell">',
    ' <aside class="sidebar">',
    '  <a class="brand" href="/"><span class="brand-mark">A<span>&</span>H</span><span><b>A&H</b><small>LOGÍSTICA</small></span></a>',
    '  <div class="side-section-label">OPERACIÓN</div>',
    '  <div class="side-link active"><span class="side-icon">⌖</span>Centro de control</div>',
    '  <div class="side-section-label side-section-spaced">EQUIPO</div>',
    '  <div class="team-mini"><span class="team-mini-dot"></span><span><b>2 conductores</b><small>Seguimiento en vivo</small></span></div>',
    '  <div class="sidebar-bottom">',
    '   <div class="account-card"><span class="avatar">' + '<span id="account-initial">L</span>' + '</span><span><b id="account-name">Logística</b><small>Torre de control</small></span><button class="icon-button" data-action="password" title="Cambiar contraseña">•••</button></div>',
    '   <button class="logout-button" data-action="logout"><span>↪</span> Cerrar sesión</button>',
    '  </div>',
    ' </aside>',
    ' <main class="workspace">',
    '  <header class="topbar"><div><div class="eyebrow" id="control-date">AGUASCALIENTES, AGS.</div><h1>Centro de control</h1></div>',
    '   <div class="topbar-right"><span id="connection-badge" class="live-badge"><i></i> Conectando</span><span class="top-avatar" id="top-initial">L</span><button class="top-logout" data-action="password">Clave</button><button class="top-logout" data-action="logout">Salir</button></div>',
    '  </header>',
    '  <section class="stats-grid">',
    '   <article class="stat-card"><span class="stat-label">EN SERVICIO</span><strong id="stat-active">0</strong><small>servicios en curso</small><span class="stat-mark green">↗</span></article>',
    '   <article class="stat-card"><span class="stat-label">POR ASIGNAR</span><strong id="stat-pending">0</strong><small>esperando conductor</small><span class="stat-mark amber">⌁</span></article>',
    '   <article class="stat-card"><span class="stat-label">CONDUCTORES EN LÍNEA</span><strong id="stat-online">0<span class="stat-total"> / 2</span></strong><small>ubicación reciente</small><span class="stat-mark blue">◎</span></article>',
    '   <article class="stat-card stat-date"><span class="stat-label">HOY</span><strong id="stat-date">—</strong><small>turno local</small><span class="stat-mark pale">◷</span></article>',
    '  </section>',
    '  <section class="control-layout">',
    '   <article class="panel map-panel">',
    '    <div class="panel-heading"><div><h2>Mapa de operación</h2><p>Ubicación en tiempo real de los conductores</p></div><span class="map-live"><i></i> EN VIVO</span></div>',
    '    <div id="control-map" class="map-canvas"><div class="map-loading">Cargando mapa de Aguascalientes…</div></div>',
    '    <div class="map-legend"><span><i class="legend-dot moving"></i> Con ubicación reciente</span><span><i class="legend-dot stale"></i> Sin señal reciente</span></div>',
    '   </article>',
    '   <aside class="dispatch-column">',
    '    <article class="panel dispatch-panel">',
    '     <div class="panel-heading"><div><h2>Nueva orden</h2><p>Asigna un servicio a un conductor</p></div><span class="panel-plus">+</span></div>',
    '     <form id="dispatch-form" class="dispatch-form">',
    '      <label>Nombre del servicio<input name="title" required maxlength="90" placeholder="Ej. Entrega de mercancía"></label>',
    '      <div class="form-row"><label>Origen<input name="origin" required maxlength="160" placeholder="Colonia o dirección"></label><label>Destino<input name="destination" required maxlength="160" placeholder="Colonia o dirección"></label></div>',
    '      <label>Conductor<select name="driverId" id="driver-select" required><option value="">Selecciona conductor</option></select></label>',
    '      <label>Instrucciones <span class="optional">OPCIONAL</span><textarea name="notes" maxlength="600" rows="2" placeholder="Detalles para el conductor"></textarea></label>',
    '      <div class="form-error" id="dispatch-error" role="alert"></div>',
    '      <button class="button button-dark button-wide" type="submit">Enviar orden <span>→</span></button>',
    '     </form>',
    '    </article>',
    '    <article class="panel drivers-panel"><div class="panel-heading compact"><div><h2>Conductores</h2><p>Equipo de campo</p></div><span class="driver-count" id="driver-count">02</span></div><div id="drivers-grid" class="drivers-list"></div></article>',
    '   </aside>',
    '  </section>',
    '  <section class="panel orders-panel"><div class="panel-heading"><div><h2>Servicios</h2><p>Órdenes asignadas y su avance</p></div><button class="text-button" data-action="refresh">Actualizar ↻</button></div><div id="orders-list" class="orders-list"></div></section>',
    '  <footer class="workspace-footer">A&H Logística <span>·</span> Panel privado de operación <span class="footer-right">Los cambios se sincronizan en tiempo real</span></footer>',
    ' </main>',
    '</div>',
  ].join('');

  const DRIVER_SHELL = [
    '<div class="shell driver-shell">',
    ' <aside class="sidebar driver-sidebar">',
    '  <a class="brand" href="/"><span class="brand-mark">A<span>&</span>H</span><span><b>A&H</b><small>LOGÍSTICA</small></span></a>',
    '  <div class="driver-welcome"><span class="eyebrow">TU JORNADA</span><h2>En ruta,<br><em>en equipo.</em></h2><p>Las órdenes que te asigne logística aparecerán aquí.</p></div>',
    '  <div class="driver-sidebar-status"><span class="secure-dot"></span><span><b>Tu ubicación es privada</b><small>Solo visible para logística</small></span></div>',
    '  <div class="sidebar-bottom">',
    '   <div class="account-card"><span class="avatar driver-avatar" id="driver-avatar">C</span><span><b id="driver-account-name">Conductor</b><small>Conductor</small></span><button class="icon-button" data-action="password" title="Cambiar contraseña">•••</button></div>',
    '   <button class="logout-button" data-action="logout"><span>↪</span> Cerrar sesión</button>',
    '  </div>',
    ' </aside>',
    ' <main class="workspace driver-workspace">',
    '  <header class="topbar"><div><div class="eyebrow">A&H LOGÍSTICA · AGUASCALIENTES</div><h1>Mi jornada</h1></div>',
    '   <div class="topbar-right"><span id="connection-badge" class="live-badge"><i></i> Conectando</span><span class="top-avatar" id="driver-top-initial">C</span><button class="top-logout" data-action="password">Clave</button><button class="top-logout" data-action="logout">Salir</button></div>',
    '  </header>',
    '  <section class="driver-hero panel"><div class="hero-copy"><div class="eyebrow">ESTADO DE UBICACIÓN</div><h2 id="tracking-heading">Comparte tu ubicación</h2><p id="tracking-copy">Activa el seguimiento para que logística vea dónde estás durante tu jornada.</p><div id="tracking-message" class="tracking-message"></div></div>',
    '   <button id="tracking-button" class="button button-track" data-action="tracking-start"><span class="track-pulse"></span> Compartir ubicación</button>',
    '  </section>',
    '  <section class="driver-layout">',
    '   <article class="panel driver-map-panel"><div class="panel-heading"><div><h2>Tu ubicación</h2><p>Mapa de Aguascalientes y alrededores</p></div><span id="driver-map-status" class="map-live muted"><i></i> ESPERANDO GPS</span></div><div id="driver-map" class="map-canvas driver-map"><div class="map-loading">Activa la ubicación para mostrar el mapa</div></div><div class="map-legend"><span><i class="legend-dot moving"></i> Tu posición</span><span class="map-update" id="driver-location-time">Ubicación apagada</span></div></article>',
    '   <aside class="panel current-order-panel"><div class="panel-heading"><div><h2>Servicio asignado</h2><p>Instrucciones de logística</p></div><span class="order-icon">↗</span></div><div id="driver-current-order"></div></aside>',
    '  </section>',
    '  <section class="panel driver-history-panel"><div class="panel-heading"><div><h2>Mis servicios</h2><p>Órdenes activas y recientes</p></div><button class="text-button" data-action="refresh">Actualizar ↻</button></div><div id="driver-orders-list" class="driver-orders-list"></div></section>',
    '  <footer class="workspace-footer">A&H Logística <span>·</span> Mantén esta pantalla abierta mientras compartes ubicación</footer>',
    ' </main>',
    '</div>',
  ].join('');

  async function refreshData(quiet = true) {
    if (refreshInFlight || !currentUser) return;
    refreshInFlight = true;
    try {
      const result = await api('/api/bootstrap');
      snapshot = { drivers: result.drivers || [], orders: result.orders || [] };
      if (currentUser.role === 'control') {
        if (!document.getElementById('control-map')) renderControl();
        else {
          renderControlStats();
          renderDriverCards();
          renderDispatchOptions();
          renderControlOrders();
          syncControlMarkers();
        }
      } else {
        if (!document.getElementById('driver-map')) renderDriver();
        else {
          plotDriverPosition();
          renderDriverOrders();
          updateTrackingUi();
        }
      }
      setConnectionState(true);
      if (!quiet) toast('Información actualizada.', 'success');
    } finally {
      refreshInFlight = false;
    }
  }

  function setConnectionState(connected) {
    const badge = document.getElementById('connection-badge');
    if (!badge) return;
    badge.classList.toggle('offline', !connected);
    badge.innerHTML = '<i></i> ' + (connected ? 'Conectado' : 'Sin conexión');
  }

  function handleConnectionError(error) {
    setConnectionState(false);
    if (error.status === 401 && currentUser) {
      const role = currentUser.role;
      closeLive();
      if (role === 'driver') stopTracking();
      currentUser = null;
      goToRole(role);
    }
  }

  function fillAccountNames() {
    const name = currentUser ? currentUser.displayName : '';
    ['account-name', 'driver-account-name'].forEach(id => {
      const node = document.getElementById(id);
      if (node) node.textContent = name;
    });
    const initial = (name || 'A').trim().charAt(0).toUpperCase();
    ['account-initial', 'top-initial', 'driver-avatar', 'driver-top-initial'].forEach(id => {
      const node = document.getElementById(id);
      if (node) node.textContent = initial;
    });
  }

  function initControlMap() {
    const node = document.getElementById('control-map');
    if (!node) return;
    if (!window.L) {
      node.innerHTML = '<div class="map-fallback">No se pudo cargar el mapa. Revisa la conexión a internet.</div>';
      return;
    }
    controlMap = L.map(node, { zoomControl: true }).setView(AGS_CENTER, 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(controlMap);
    requestAnimationFrame(() => controlMap && controlMap.invalidateSize());
    drawAllDrivers();
  }

  function initDriverMap() {
    const node = document.getElementById('driver-map');
    if (!node) return;
    if (!window.L) {
      node.innerHTML = '<div class="map-fallback">No se pudo cargar el mapa. Revisa la conexión a internet.</div>';
      return;
    }
    driverMap = L.map(node, { zoomControl: true }).setView(AGS_CENTER, 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(driverMap);
    requestAnimationFrame(() => driverMap && driverMap.invalidateSize());
  }

  function makeDriverIcon(online) {
    return L.divIcon({
      className: 'driver-map-icon ' + (online ? 'is-online' : 'is-offline'),
      html: '<span>↗</span>',
      iconSize: [34, 34],
      iconAnchor: [17, 17],
    });
  }

  function drawAllDrivers() {
    if (!controlMap || !window.L) return;
    controlMarkers.forEach(marker => controlMap.removeLayer(marker));
    controlMarkers = new Map();
    snapshot.drivers.forEach(driver => updateControlMarker(driver));
  }

  function syncControlMarkers() {
    if (!controlMap || !window.L) return;
    const located = new Set(snapshot.drivers.filter(driver => driver.lat != null && driver.lng != null).map(driver => driver.id));
    controlMarkers.forEach((marker, id) => {
      if (!located.has(id)) {
        controlMap.removeLayer(marker);
        controlMarkers.delete(id);
      }
    });
    snapshot.drivers.forEach(updateControlMarker);
  }

  function updateControlMarker(driver) {
    if (!controlMap || !window.L || driver.lat == null || driver.lng == null) return;
    const isOnline = driver.updatedAt && Date.now() - new Date(driver.updatedAt).getTime() < 60_000;
    const marker = controlMarkers.get(driver.id);
    if (marker) {
      marker.setLatLng([driver.lat, driver.lng]);
      marker.setIcon(makeDriverIcon(isOnline));
      marker.setPopupContent('<b>' + escapeHtml(driver.displayName) + '</b><br>' + escapeHtml(relativeTime(driver.updatedAt)));
      return;
    }
    const next = L.marker([driver.lat, driver.lng], { icon: makeDriverIcon(isOnline) })
      .bindPopup('<b>' + escapeHtml(driver.displayName) + '</b><br>' + escapeHtml(relativeTime(driver.updatedAt)))
      .addTo(controlMap);
    next.bindTooltip(escapeHtml(driver.displayName), { direction: 'top', offset: [0, -16] });
    controlMarkers.set(driver.id, next);
  }

  function renderControl() {
    if (controlMap) {
      controlMap.remove();
      controlMap = null;
      controlMarkers = new Map();
    }
    app.innerHTML = CONTROL_SHELL;
    fillAccountNames();
    const dateNode = document.getElementById('stat-date');
    if (dateNode) dateNode.textContent = new Intl.DateTimeFormat('es-MX', { day: '2-digit', month: 'short' }).format(new Date());
    const dateHeader = document.getElementById('control-date');
    if (dateHeader) dateHeader.textContent = new Intl.DateTimeFormat('es-MX', { weekday: 'long' }).format(new Date()).toUpperCase() + ' · AGUASCALIENTES, AGS.';
    renderControlStats();
    renderDriverCards();
    renderDispatchOptions();
    renderControlOrders();
    initControlMap();
  }

  function renderControlStats() {
    const active = snapshot.orders.filter(order => ['accepted', 'en_route'].includes(order.status)).length;
    const pending = snapshot.orders.filter(order => order.status === 'pending').length;
    const online = snapshot.drivers.filter(driver => driver.updatedAt && Date.now() - new Date(driver.updatedAt).getTime() < 60_000).length;
    setText('stat-active', active);
    setText('stat-pending', pending);
    setText('stat-online', online);
  }

  function setText(id, value) {
    const node = document.getElementById(id);
    if (node) node.textContent = value;
  }

  function renderDriverCards() {
    const node = document.getElementById('drivers-grid');
    if (!node) return;
    setText('driver-count', String(snapshot.drivers.length).padStart(2, '0'));
    node.innerHTML = snapshot.drivers.map(driver => {
      const online = driver.updatedAt && Date.now() - new Date(driver.updatedAt).getTime() < 60_000;
      const position = driver.lat == null ? 'Esperando ubicación' : Number(driver.lat).toFixed(5) + ', ' + Number(driver.lng).toFixed(5);
      const accuracy = driver.accuracy ? ' ±' + Math.round(driver.accuracy) + ' m' : '';
      return [
        '<div class="driver-row">',
        ' <span class="driver-status-dot ' + (online ? 'online' : '') + '"></span>',
        ' <span class="driver-row-main"><b>' + escapeHtml(driver.displayName) + '</b><small>' + escapeHtml(online ? 'En línea · ' + relativeTime(driver.updatedAt) : (driver.updatedAt ? relativeTime(driver.updatedAt) : 'Sin conexión')) + '</small></span>',
        ' <span class="driver-row-meta"><small>' + escapeHtml(position + accuracy) + '</small><small>' + Number(driver.activeOrders || 0) + ' servicios activos</small></span>',
        '</div>',
      ].join('');
    }).join('') || '<div class="empty-inline">No hay conductores activos.</div>';
  }

  function renderDispatchOptions() {
    const select = document.getElementById('driver-select');
    if (!select) return;
    const previous = select.value;
    select.innerHTML = '<option value="">Selecciona conductor</option>' + snapshot.drivers.map(driver =>
      '<option value="' + escapeHtml(driver.id) + '">' + escapeHtml(driver.displayName) + '</option>'
    ).join('');
    if (snapshot.drivers.some(driver => driver.id === previous)) select.value = previous;
  }

  function orderStatusBadge(status) {
    const item = STATUS[status] || STATUS.pending;
    return '<span class="status-badge status-' + item.className + '"><i></i>' + item.label + '</span>';
  }

  function renderControlOrders() {
    const node = document.getElementById('orders-list');
    if (!node) return;
    if (!snapshot.orders.length) {
      node.innerHTML = '<div class="empty-state"><span class="empty-symbol">⌁</span><b>Aún no hay servicios</b><p>Cuando envíes una orden, su avance aparecerá aquí.</p></div>';
      return;
    }
    node.innerHTML = snapshot.orders.map(order => [
      '<article class="order-row">',
      ' <div class="order-row-title"><span class="order-number">#' + escapeHtml(order.id.slice(0, 6).toUpperCase()) + '</span><b>' + escapeHtml(order.title) + '</b></div>',
      ' <div class="order-route"><span>' + escapeHtml(order.origin) + '</span><i>→</i><span>' + escapeHtml(order.destination) + '</span></div>',
      ' <div class="order-driver"><span class="mini-avatar">' + escapeHtml((order.driverName || 'C').charAt(0)) + '</span>' + escapeHtml(order.driverName) + '</div>',
      ' <div class="order-state">' + orderStatusBadge(order.status) + '<small>' + shortTime(order.updatedAt) + '</small></div>',
      ['completed', 'cancelled'].includes(order.status) ? '' : '<button class="cancel-order" data-action="cancel-order" data-id="' + escapeHtml(order.id) + '" title="Cancelar servicio">×</button>',
      '</article>',
    ].join('')).join('');
  }

  function renderDriver() {
    if (driverMap) {
      driverMap.remove();
      driverMap = null;
      driverMarker = null;
    }
    app.innerHTML = DRIVER_SHELL;
    fillAccountNames();
    initDriverMap();
    plotDriverPosition();
    renderDriverOrders();
    updateTrackingUi();
    setConnectionState(true);
  }

  function currentOrders() {
    return snapshot.orders.filter(order => ['pending', 'accepted', 'en_route'].includes(order.status));
  }

  function orderActionLabel(status) {
    if (status === 'pending') return ['accepted', 'Aceptar servicio'];
    if (status === 'accepted') return ['en_route', 'Iniciar recorrido'];
    if (status === 'en_route') return ['completed', 'Marcar entregado'];
    return ['', 'Completado'];
  }

  function renderDriverOrders() {
    const currentNode = document.getElementById('driver-current-order');
    const listNode = document.getElementById('driver-orders-list');
    if (!currentNode || !listNode) return;
    const active = currentOrders();
    const primary = active[0];
    if (!primary) {
      currentNode.innerHTML = '<div class="no-current-order"><span class="empty-symbol">⌁</span><b>No tienes servicios asignados</b><p>Cuando logística te envíe una orden, podrás verla aquí.</p></div>';
    } else {
      const action = orderActionLabel(primary.status);
      currentNode.innerHTML = [
        '<div class="current-order-card">',
        ' <div class="current-order-top"><span class="order-number">#' + escapeHtml(primary.id.slice(0, 6).toUpperCase()) + '</span>' + orderStatusBadge(primary.status) + '</div>',
        ' <h3>' + escapeHtml(primary.title) + '</h3>',
        ' <div class="route-points"><div><i class="route-point origin-point"></i><span><small>ORIGEN</small><b>' + escapeHtml(primary.origin) + '</b></span></div><div class="route-line"></div><div><i class="route-point destination-point"></i><span><small>DESTINO</small><b>' + escapeHtml(primary.destination) + '</b></span></div></div>',
        primary.notes ? '<div class="order-notes"><small>INSTRUCCIONES</small><p>' + escapeHtml(primary.notes) + '</p></div>' : '',
        '<button class="button button-dark button-wide" data-action="advance-order" data-id="' + escapeHtml(primary.id) + '" data-status="' + escapeHtml(action[0]) + '">' + escapeHtml(action[1]) + ' <span>→</span></button>',
        '</div>',
      ].join('');
    }
    if (!snapshot.orders.length) {
      listNode.innerHTML = '<div class="empty-inline">Todavía no tienes órdenes. La torre de control te asignará una.</div>';
      return;
    }
    listNode.innerHTML = snapshot.orders.map(order => {
      const action = orderActionLabel(order.status);
      const canAdvance = Boolean(action[0]);
      return [
        '<article class="driver-order-row"><div class="driver-order-info"><span class="order-number">#' + escapeHtml(order.id.slice(0, 6).toUpperCase()) + '</span><b>' + escapeHtml(order.title) + '</b><small>' + escapeHtml(order.origin) + ' <i>→</i> ' + escapeHtml(order.destination) + '</small></div>',
        '<div class="driver-order-meta">' + orderStatusBadge(order.status) + '<small>Actualizado ' + shortTime(order.updatedAt) + '</small></div>',
        canAdvance ? '<button class="small-action" data-action="advance-order" data-id="' + escapeHtml(order.id) + '" data-status="' + escapeHtml(action[0]) + '">' + escapeHtml(action[1]) + '</button>' : '',
        '</article>',
      ].join('');
    }).join('');
  }

  function updateTrackingUi(errorText = '') {
    const button = document.getElementById('tracking-button');
    const heading = document.getElementById('tracking-heading');
    const copy = document.getElementById('tracking-copy');
    const message = document.getElementById('tracking-message');
    const badge = document.getElementById('driver-map-status');
    if (!button) return;
    button.dataset.action = tracking ? 'tracking-stop' : 'tracking-start';
    button.classList.toggle('is-tracking', tracking);
    button.innerHTML = '<span class="track-pulse"></span>' + (tracking ? 'Dejar de compartir' : 'Compartir ubicación');
    if (heading) heading.textContent = tracking ? 'Ubicación compartida' : 'Comparte tu ubicación';
    if (copy) copy.textContent = tracking
      ? 'La torre de control puede ver tu posición mientras este seguimiento esté activo.'
      : 'Activa el seguimiento cuando comiences tu jornada. Puedes detenerlo en cualquier momento.';
    if (message) message.textContent = errorText || (tracking ? 'Seguimiento activo en este dispositivo.' : '');
    if (badge) {
      const gpsActive = tracking && Boolean(lastPosition);
      badge.classList.toggle('muted', !gpsActive);
      badge.innerHTML = '<i></i>' + (gpsActive ? ' GPS ACTIVO' : tracking ? ' BUSCANDO GPS' : ' GPS APAGADO');
    }
    const timeNode = document.getElementById('driver-location-time');
    if (timeNode) timeNode.textContent = tracking && lastPosition ? 'Último punto: ' + new Date(lastPosition.timestamp || Date.now()).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' }) : 'Ubicación apagada';
  }

  function plotDriverPosition() {
    if (!driverMap || !lastPosition || !window.L) return;
    if (!driverMarker) {
      driverMarker = L.circleMarker([lastPosition.lat, lastPosition.lng], {
        radius: 9, color: '#fff', weight: 3, fillColor: '#a3d951', fillOpacity: 1,
      }).addTo(driverMap).bindPopup('Tu ubicación actual');
    } else {
      driverMarker.setLatLng([lastPosition.lat, lastPosition.lng]);
    }
    driverMap.setView([lastPosition.lat, lastPosition.lng], Math.max(driverMap.getZoom(), 14));
  }

  function updateDriverPosition(position) {
    lastPosition = {
      lat: position.coords.latitude,
      lng: position.coords.longitude,
      accuracy: position.coords.accuracy,
      speed: position.coords.speed,
      heading: position.coords.heading,
      timestamp: position.timestamp,
    };
    plotDriverPosition();
    updateTrackingUi();
  }

  function startTracking() {
    if (!navigator.geolocation) {
      updateTrackingUi('Este dispositivo no ofrece geolocalización.');
      return;
    }
    if (!window.isSecureContext) {
      updateTrackingUi('La ubicación requiere HTTPS cuando abres la app desde otro dispositivo.');
      toast('Abre esta app en una dirección HTTPS para activar el GPS.', 'error');
      return;
    }
    if (driverMarker && driverMap) {
      driverMap.removeLayer(driverMarker);
      driverMarker = null;
    }
    lastPosition = null;
    tracking = true;
    updateTrackingUi();
    watchId = navigator.geolocation.watchPosition(
      updateDriverPosition,
      error => {
        const message = error.code === 1 ? 'Permite el acceso a ubicación en los ajustes del navegador.' :
          error.code === 2 ? 'No pudimos obtener una ubicación. Revisa el GPS del dispositivo.' :
          'La ubicación tardó demasiado. Intenta de nuevo.';
        stopTracking();
        updateTrackingUi(message);
        toast(message, 'error');
      },
      { enableHighAccuracy: true, maximumAge: 4000, timeout: 20000 }
    );
    trackingTimer = setInterval(() => {
      if (!tracking || !lastPosition) return;
      api('/api/location', {
        method: 'POST',
        body: JSON.stringify({
          lat: lastPosition.lat, lng: lastPosition.lng, accuracy: lastPosition.accuracy,
          speed: lastPosition.speed, heading: lastPosition.heading,
        }),
      }).catch(handleConnectionError);
    }, 8000);
  }

  function stopTracking() {
    const wasTracking = tracking;
    tracking = false;
    if (watchId !== null) navigator.geolocation.clearWatch(watchId);
    watchId = null;
    clearInterval(trackingTimer);
    trackingTimer = null;
    if (wasTracking && currentUser && currentUser.role === 'driver') {
      api('/api/location', { method: 'DELETE' }).catch(() => {});
    }
    lastPosition = null;
    if (driverMarker && driverMap) driverMap.removeLayer(driverMarker);
    driverMarker = null;
    updateTrackingUi();
  }

  function connectLive() {
    clearInterval(pollingTimer);
    if (!currentUser || currentUser.mustChangePassword) return;
    pollingTimer = setInterval(() => {
      if (!document.hidden) refreshData(true).catch(handleConnectionError);
    }, 7000);
  }

  function closeLive() {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }

  function openPasswordModal(required = false) {
    modalRoot.innerHTML = [
      '<div class="modal-backdrop"><section class="password-modal" role="dialog" aria-modal="true" aria-labelledby="password-title">',
      '<div class="modal-mark">⌁</div><div class="eyebrow">' + (required ? 'PASO NECESARIO' : 'SEGURIDAD DE CUENTA') + '</div>',
      '<h2 id="password-title">' + (required ? 'Crea tu contraseña' : 'Cambiar contraseña') + '</h2>',
      '<p>' + (required ? 'Tu clave inicial es temporal. Cámbiala para entrar a tu espacio de trabajo.' : 'Usa una contraseña de al menos 12 caracteres.') + '</p>',
      '<form id="password-form" class="form-stack">',
      '<label>Contraseña actual<input name="currentPassword" type="password" autocomplete="current-password" required></label>',
      '<label>Nueva contraseña<input name="newPassword" type="password" autocomplete="new-password" minlength="12" required></label>',
      '<label>Confirma la nueva contraseña<input name="confirmPassword" type="password" autocomplete="new-password" minlength="12" required></label>',
      '<div class="form-error" id="password-error" role="alert"></div>',
      '<button class="button button-dark button-wide" type="submit">Guardar contraseña <span>→</span></button>',
      required ? '' : '<button class="modal-cancel" type="button" data-action="close-password">Ahora no</button>',
      '</form></section></div>',
    ].join('');
    const input = modalRoot.querySelector('[name="currentPassword"]');
    if (input) input.focus();
  }

  async function handleSubmit(event) {
    const form = event.target;
    if (form.id === 'setup-form') {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]');
      const errorNode = document.getElementById('setup-error');
      submit.disabled = true;
      errorNode.textContent = '';
      const formData = new FormData(form);
      try {
        await api('/api/setup', {
          method: 'POST',
          body: JSON.stringify(Object.fromEntries(formData.entries())),
        });
        renderChooser();
        toast('Cuentas creadas. Elige tu espacio para iniciar sesión.', 'success');
      } catch (error) {
        errorNode.textContent = error.message;
        submit.disabled = false;
      }
      return;
    }

    if (form.id === 'login-form') {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]');
      const errorNode = document.getElementById('login-error');
      submit.disabled = true;
      errorNode.textContent = '';
      try {
        const formData = new FormData(form);
        const role = roleFromPath();
        const result = await api('/api/login', {
          method: 'POST',
          body: JSON.stringify({ username: formData.get('username'), password: formData.get('password'), role }),
        });
        currentUser = result.user;
        if (currentUser.mustChangePassword) {
          if (role === 'control') renderControl(); else renderDriver();
          openPasswordModal(true);
          return;
        }
        await refreshData();
        connectLive();
      } catch (error) {
        errorNode.textContent = error.message;
      } finally {
        submit.disabled = false;
      }
    }

    if (form.id === 'dispatch-form') {
      event.preventDefault();
      const submit = form.querySelector('button[type="submit"]');
      const errorNode = document.getElementById('dispatch-error');
      submit.disabled = true;
      errorNode.textContent = '';
      const formData = new FormData(form);
      try {
        await api('/api/orders', {
          method: 'POST',
          body: JSON.stringify({
            title: formData.get('title'),
            origin: formData.get('origin'),
            destination: formData.get('destination'),
            driverId: formData.get('driverId'),
            notes: formData.get('notes'),
          }),
        });
        form.reset();
        await refreshData();
        toast('Orden enviada al conductor.', 'success');
      } catch (error) {
        errorNode.textContent = error.message;
      } finally {
        submit.disabled = false;
      }
    }

    if (form.id === 'password-form') {
      event.preventDefault();
      const errorNode = document.getElementById('password-error');
      const formData = new FormData(form);
      const next = String(formData.get('newPassword') || '');
      if (next !== String(formData.get('confirmPassword') || '')) {
        errorNode.textContent = 'Las contraseñas nuevas no coinciden.';
        return;
      }
      const submit = form.querySelector('button[type="submit"]');
      submit.disabled = true;
      errorNode.textContent = '';
      try {
        await api('/api/password', {
          method: 'POST',
          body: JSON.stringify({ currentPassword: formData.get('currentPassword'), newPassword: next }),
        });
        currentUser.mustChangePassword = false;
        modalRoot.innerHTML = '';
        await refreshData();
        connectLive();
        toast('Contraseña actualizada.', 'success');
      } catch (error) {
        errorNode.textContent = error.message;
      } finally {
        if (submit.isConnected) submit.disabled = false;
      }
    }
  }

  async function handleClick(event) {
    const target = event.target.closest('[data-action]');
    if (!target) return;
    const action = target.dataset.action;

    if (action === 'reload') location.reload();
    if (action === 'password') openPasswordModal(false);
    if (action === 'close-password') modalRoot.innerHTML = '';
    if (action === 'refresh') refreshData(false).catch(error => toast(error.message, 'error'));
    if (action === 'tracking-start') startTracking();
    if (action === 'tracking-stop') stopTracking();

    if (action === 'logout') {
      stopTracking();
      closeLive();
      await api('/api/logout', { method: 'POST' }).catch(() => {});
      currentUser = null;
      snapshot = { drivers: [], orders: [] };
      goToRole(roleFromPath() === 'choose' ? 'control' : roleFromPath());
    }

    if (action === 'advance-order') {
      target.disabled = true;
      try {
        await api('/api/orders/' + encodeURIComponent(target.dataset.id), {
          method: 'PATCH',
          body: JSON.stringify({ status: target.dataset.status }),
        });
        await refreshData();
        toast('Estado del servicio actualizado.', 'success');
      } catch (error) {
        toast(error.message, 'error');
        await refreshData().catch(() => {});
      }
    }

    if (action === 'cancel-order') {
      if (!confirm('¿Cancelar este servicio?')) return;
      target.disabled = true;
      try {
        await api('/api/orders/' + encodeURIComponent(target.dataset.id), {
          method: 'PATCH',
          body: JSON.stringify({ status: 'cancelled' }),
        });
        await refreshData();
        toast('Servicio cancelado.', 'success');
      } catch (error) {
        toast(error.message, 'error');
        await refreshData().catch(() => {});
      }
    }
  }

  document.addEventListener('submit', event => { handleSubmit(event).catch(error => toast(error.message, 'error')); });
  document.addEventListener('click', event => { handleClick(event).catch(error => toast(error.message, 'error')); });

  setInterval(() => {
    if (currentUser && currentUser.role === 'control') {
      snapshot.drivers.forEach(driver => updateControlMarker(driver));
      renderDriverCards();
      renderControlStats();
    }
  }, 15_000);

  async function start() {
    const roleInPath = roleFromPath();
    try {
      const result = await api('/api/me');
      if (result.setupRequired) return renderSetup();
      if (result.user) {
        currentUser = result.user;
        const role = currentUser.role;
        if (roleInPath === 'choose' || roleInPath !== role) return goToRole(role);
        if (currentUser.mustChangePassword) {
          if (role === 'control') renderControl(); else renderDriver();
          return openPasswordModal(true);
        }
        await refreshData();
        connectLive();
        return;
      }
    } catch {
      return renderUnavailable();
    }
    if (roleInPath === 'choose') return renderChooser();
    renderLogin(roleInPath);
  }

  start();
})();
