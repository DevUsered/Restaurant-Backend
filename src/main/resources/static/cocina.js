// Detecta la IP del servidor Spring Boot automáticamente
const SERVIDOR_IP = window.location.hostname;
const PUERTO = window.location.port || "8080";
const URL_BASE = `http://${SERVIDOR_IP}:${PUERTO}`;
const WS_URL = `ws://${SERVIDOR_IP}:${PUERTO}/ws-sync`;
const STORAGE_KEY_USUARIO = "kds_usuario_activo";

let socket = null;
let reconnectTimer = null;
let esReconexionWS = false;
let usuarioActual = null;
let wakeLock = null;

const campanaAudio = new Audio("sounds/cocina.mp3");
let audioDesbloqueado = false;
let desbloqueandoAudio = false;
let idsPedidosEnPantalla = [];
let esPrimeraCarga = true;

// =========================================
// MANTENER PANTALLA ENCENDIDA (WAKE LOCK)
// =========================================
async function activarWakeLock() {
    if (!("wakeLock" in navigator)) {
        console.warn("Screen Wake Lock no disponible en este navegador o conexión HTTP.");
        return;
    }

    if (wakeLock !== null || !usuarioActual || document.visibilityState !== "visible") {
        return;
    }

    try {
        wakeLock = await navigator.wakeLock.request("screen");
        console.log("🔆 Pantalla siempre encendida activada (Wake Lock).");

        wakeLock.addEventListener("release", () => {
            console.log("🌙 Wake Lock liberado.");
            wakeLock = null;
        });
    } catch (err) {
        console.warn(`No se pudo activar Wake Lock: ${err.name} - ${err.message}`);
        wakeLock = null;
    }
}

async function liberarWakeLock() {
    if (wakeLock !== null) {
        try {
            await wakeLock.release();
        } catch (err) {
            console.warn("Error al liberar Wake Lock:", err);
        } finally {
            wakeLock = null;
        }
    }
}

document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible" && usuarioActual) {
        activarWakeLock();
        if (!socket || socket.readyState === WebSocket.CLOSED || socket.readyState === WebSocket.CLOSING) {
            iniciarWebSocket();
        } else {
            cargarPedidos();
        }
    }
});

// =========================================
// CONTROL DE AUDIO Y ALERTA VISUAL
// =========================================
function mostrarAlertaAudio() {
    const btn = document.getElementById("btnActivarAudio");
    if (btn) btn.classList.remove("hidden");
}

function ocultarAlertaAudio() {
    const btn = document.getElementById("btnActivarAudio");
    if (btn) btn.classList.add("hidden");
}

function registrarListenersAudio() {
    document.addEventListener("click", manejarInteraccionGlobal);
    document.addEventListener("touchstart", manejarInteraccionGlobal);
}

function removerListenersAudio() {
    document.removeEventListener("click", manejarInteraccionGlobal);
    document.removeEventListener("touchstart", manejarInteraccionGlobal);
}

function manejarInteraccionGlobal(event) {
    if (event.target.closest("#btnActivarAudio")) return;
    desbloquearAudioSilencioso();
    activarWakeLock();
}

function desbloquearAudioSilencioso() {
    if (audioDesbloqueado || desbloqueandoAudio) return;
    desbloqueandoAudio = true;

    campanaAudio.muted = false;
    campanaAudio.volume = 0;
    campanaAudio.play().then(() => {
        campanaAudio.pause();
        campanaAudio.currentTime = 0;
        campanaAudio.volume = 1;
        audioDesbloqueado = true;
        desbloqueandoAudio = false;
        ocultarAlertaAudio();
        removerListenersAudio();
    }).catch(() => {
        campanaAudio.volume = 1;
        desbloqueandoAudio = false;
    });
}

function activarAudioConPrueba() {
    campanaAudio.muted = false;
    campanaAudio.volume = 1;
    campanaAudio.currentTime = 0;

    campanaAudio.play().then(() => {
        audioDesbloqueado = true;
        ocultarAlertaAudio();
        removerListenersAudio();
        activarWakeLock();
    }).catch((e) => {
        console.warn("No se pudo activar el audio:", e);
    });
}

function verificarPermisoAudio() {
    if (audioDesbloqueado) {
        ocultarAlertaAudio();
        return;
    }

    campanaAudio.muted = false;
    campanaAudio.volume = 0;
    campanaAudio.play().then(() => {
        campanaAudio.pause();
        campanaAudio.currentTime = 0;
        campanaAudio.volume = 1;
        audioDesbloqueado = true;
        ocultarAlertaAudio();
    }).catch(() => {
        campanaAudio.volume = 1;
        mostrarAlertaAudio();
        registrarListenersAudio();
    });
}

function reproducirCampana() {
    try {
        campanaAudio.muted = false;
        campanaAudio.volume = 1;
        campanaAudio.currentTime = 0;
        campanaAudio.play().catch(e => {
            console.warn("El navegador bloqueó el sonido automático.", e);
            audioDesbloqueado = false;
            mostrarAlertaAudio();
            registrarListenersAudio();
        });
    } catch (error) {
        console.error("Error al reproducir audio en la web:", error);
    }
}

// =========================================
// SESIÓN Y CAMBIO DE PANTALLAS
// =========================================
function mostrarPantallaLogin() {
    const login = document.getElementById("login-container");
    const kds = document.getElementById("kds-container");

    kds.classList.remove("visible");
    kds.classList.add("hidden");

    login.classList.remove("hidden");
    login.classList.add("visible");
    const txtUsuario = document.getElementById("txtUsuario");
    if(txtUsuario) txtUsuario.focus();
}

function activarPantallaKDS(usuario) {
    usuarioActual = usuario;
    document.getElementById("lblNombreCocinero").innerText = usuarioActual.nombre || "Cocinero";
    document.getElementById("lblError").innerText = "";

    const login = document.getElementById("login-container");
    const kds = document.getElementById("kds-container");

    login.classList.remove("visible");
    login.classList.add("hidden");

    kds.classList.remove("hidden");
    kds.classList.add("visible");

    verificarPermisoAudio();
    activarWakeLock();
    esReconexionWS = false;
    iniciarWebSocket();
    cargarPedidos();
}

function restaurarSesion() {
    const sesionGuardada = localStorage.getItem(STORAGE_KEY_USUARIO);
    if (!sesionGuardada) {
        mostrarPantallaLogin();
        return;
    }

    try {
        const usuario = JSON.parse(sesionGuardada);
        if (usuario && usuario.nombre) {
            activarPantallaKDS(usuario);
        } else {
            localStorage.removeItem(STORAGE_KEY_USUARIO);
            mostrarPantallaLogin();
        }
    } catch (e) {
        localStorage.removeItem(STORAGE_KEY_USUARIO);
        mostrarPantallaLogin();
    }
}

async function iniciarSesion() {
    const user = document.getElementById("txtUsuario").value.trim();
    const pass = document.getElementById("txtPassword").value;
    const lblError = document.getElementById("lblError");

    if (!user || !pass) {
        lblError.innerText = "Llene todos los campos.";
        return;
    }

    desbloquearAudioSilencioso();

    try {
        lblError.innerText = "Conectando...";

        const response = await fetch(`${URL_BASE}/api/web/cocina/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: user, password: pass })
        });

        const data = await response.json();

        if (response.ok && data.exito) {
            localStorage.setItem(STORAGE_KEY_USUARIO, JSON.stringify(data.usuario));
            activarPantallaKDS(data.usuario);
        } else {
            lblError.innerText = data.mensaje || "Error al iniciar sesión.";
        }
    } catch (error) {
        lblError.innerText = "Error de conexión con el servidor.";
    }
}

function cerrarSesion() {
    usuarioActual = null;
    localStorage.removeItem(STORAGE_KEY_USUARIO);
    ocultarAlertaAudio();
    removerListenersAudio();
    liberarWakeLock();

    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }
    if (socket) {
        socket.onclose = null;
        socket.close();
        socket = null;
    }

    document.getElementById("txtPassword").value = "";
    document.getElementById("lblError").innerText = "";
    mostrarPantallaLogin();

    idsPedidosEnPantalla = [];
    esPrimeraCarga = true;
    esReconexionWS = false;
}

function iniciarWebSocket() {
    if (reconnectTimer) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    }

    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        return;
    }

    console.log("🔌 Conectando WebSocket a:", WS_URL);
    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("✅ WebSocket conectado para la cocina.");
        if(esReconexionWS){
            cargarPedidos();
        }
        esReconexionWS = true;
    };

    socket.onmessage = (event) => {
        const mensaje = String(event.data || "");
        console.log("📩 Evento recibido:", mensaje);

        if (mensaje.includes("SYNC_PEDIDOS")) {
            cargarPedidos();
        }
    };

    socket.onerror = (error) => {
        console.error("❌ Error en WebSocket:", error);
    };

    socket.onclose = (event) => {
        socket = null;
        if (!usuarioActual) return;
        console.warn(`⚠️ WebSocket cerrado (código ${event.code}). Reintentando en 3 segundos...`);
        reconnectTimer = setTimeout(iniciarWebSocket, 3000);
    };
}

async function cargarPedidos() {
    try {
        const response = await fetch(`${URL_BASE}/api/web/pedidos/pendientes`, {
            cache: "no-store"
        });
        if (response.ok) {
            const pedidos = await response.json();
            const nuevosIds = pedidos.map(p => p.idPedido);

            if (!esPrimeraCarga) {
                const llegoPedidoNuevo = nuevosIds.some(id => !idsPedidosEnPantalla.includes(id));
                if (llegoPedidoNuevo) {
                    reproducirCampana();
                }
            }

            idsPedidosEnPantalla = nuevosIds;
            esPrimeraCarga = false;
            dibujarPedidos(pedidos);
        }
    } catch (error) {
        console.error("Error obteniendo pedidos:", error);
    }
}

function dibujarPedidos(pedidos) {
    const contenedor = document.getElementById("contenedor-pedidos");
    contenedor.innerHTML = "";

    if (pedidos.length === 0) {
        contenedor.innerHTML = "<h2 style='grid-column: 1/-1; text-align:center; margin-top:50px; color:#777;'>No hay pedidos pendientes. ¡Buen trabajo!</h2>";
        return;
    }

    pedidos.forEach(pedido => {
        const tarjeta = document.createElement("div");
        tarjeta.className = "tarjeta-pedido";

        let listaHTML = "";
        if (pedido.detalles) {
            pedido.detalles.forEach(det => {
                listaHTML += `<li>${det}</li>`;
            });
        } else {
            listaHTML = "<li>Detalles no disponibles</li>";
        }

        tarjeta.innerHTML = `
            <div class="tarjeta-header">
                <h3 class="cliente-nombre">${pedido.cliente || "Cliente"}</h3>
                <span class="ticket-numero">Ticket #${pedido.numeroTicket}</span>
            </div>
            <div class="tarjeta-body">
                <ul>${listaHTML}</ul>
            </div>
            <button class="btn-despachar" ${pedidosEnProceso.has(pedido.idPedido) ? "disabled" : ""} onclick="despacharPedido(${pedido.idPedido}, this)">
                ${pedidosEnProceso.has(pedido.idPedido) ? "⏳ DESPACHANDO..." : "✅ DESPACHAR"}
            </button>
        `;
        contenedor.appendChild(tarjeta);
    });
}

const pedidosEnProceso = new Set();

async function despacharPedido(idPedido, boton) {
    if (pedidosEnProceso.has(idPedido)) return;
    pedidosEnProceso.add(idPedido);

    if (boton) {
        boton.disabled = true;
        boton.innerText = "⏳ DESPACHANDO...";
    }
    try {
        const response = await fetch(`${URL_BASE}/api/web/pedidos/despachar/${idPedido}`, {
            method: "POST"
        });

        if (response.ok) {
             await cargarPedidos();
        }else{
            throw new Error(`HTTP ${response.status}`)
        }
    } catch (error) {
        console.error("Error despachando:", error);
        if (boton) {
            boton.disabled = false;
            boton.innerText = "✅ DESPACHAR";
        }
    }finally{
        pedidosEnProceso.delete(idPedido);
    }
}
function configurarTecladoLogin() {
    const txtUsuario = document.getElementById("txtUsuario");
    const txtPassword = document.getElementById("txtPassword");

    if (txtUsuario && txtPassword) {
        txtUsuario.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                txtPassword.focus();
            }
        });

        txtPassword.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                iniciarSesion();
            }
        });
    }
}

configurarTecladoLogin();
restaurarSesion();