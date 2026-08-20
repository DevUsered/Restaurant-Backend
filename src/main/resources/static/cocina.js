// Detecta la IP del servidor Spring Boot automáticamente
const SERVIDOR_IP = window.location.hostname;
const PUERTO = window.location.port || "8080";
const URL_BASE = `http://${SERVIDOR_IP}:${PUERTO}`;
const WS_URL = `ws://${SERVIDOR_IP}:${PUERTO}/ws-sync`;

let socket = null;
let usuarioActual = null;

async function iniciarSesion() {
    const user = document.getElementById("txtUsuario").value;
    const pass = document.getElementById("txtPassword").value;
    const lblError = document.getElementById("lblError");

    if (!user || !pass) {
        lblError.innerText = "Llene todos los campos.";
        return;
    }

    try {
        lblError.innerText = "Conectando...";
        const response = await fetch(`${URL_BASE}/api/web/cocina/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });

        const data = await response.json();

        if (response.ok && data.exito) {
            // Login correcto
            usuarioActual = data.usuario;
            document.getElementById("lblNombreCocinero").innerText = usuarioActual.nombre;

            // Cambiar pantalla
            document.getElementById("login-container").classList.replace("visible", "hidden");
            document.getElementById("kds-container").classList.replace("hidden", "visible");

            // Iniciar conexión y cargar datos
            iniciarWebSocket();
            cargarPedidos();
        } else {
            lblError.innerText = data.mensaje || "Error al iniciar sesión.";
        }
    } catch (error) {
        lblError.innerText = "Error de conexión con el servidor.";
    }
}

function cerrarSesion() {
    usuarioActual = null;
    if (socket) socket.close();
    document.getElementById("txtPassword").value = "";
    document.getElementById("kds-container").classList.replace("visible", "hidden");
    document.getElementById("login-container").classList.replace("hidden", "visible");
}

// ==========================================
// 2. WEBSOCKET (TIEMPO REAL)
// ==========================================
function iniciarWebSocket() {
    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
        console.log("✅ WebSocket conectado para la cocina.");
    };

    socket.onmessage = (event) => {
        const mensaje = event.data;
        console.log("📩 Evento recibido:", mensaje);

        // Si desde JavaFX mandan el evento SYNC_PEDIDOS
        if (mensaje.includes("SYNC_PEDIDOS")) {
            reproducirCampana();
            cargarPedidos(); // Refrescar pantalla
        }
    };

    socket.onclose = () => {
        console.warn("⚠️ WebSocket cerrado. Reintentando en 5 segundos...");
        setTimeout(iniciarWebSocket, 5000);
    };
}

function reproducirCampana() {
   try{
       const audio = new Audio('sounds/cocina.mp3');
       audio.play().catch(e => {
           console.warn("El navegador bloqeó el sonido automático.", e);
       });
   }catch (error){
       console.error("Error al reproducir audio en la web:", error);
   }
}
async function cargarPedidos() {
    try {
        const response = await fetch(`${URL_BASE}/api/web/pedidos/pendientes`);
        if (response.ok) {
            const pedidos = await response.json();
            dibujarPedidos(pedidos);
        }
    } catch (error) {
        console.error("Error obteniendo pedidos:", error);
    }
}

function dibujarPedidos(pedidos) {
    const contenedor = document.getElementById("contenedor-pedidos");
    contenedor.innerHTML = ""; // Limpiar grid

    if(pedidos.length === 0) {
        contenedor.innerHTML = "<h2 style='grid-column: 1/-1; text-align:center; margin-top:50px; color:#777;'>No hay pedidos pendientes. ¡Buen trabajo!</h2>";
        return;
    }

    pedidos.forEach(pedido => {
        const tarjeta = document.createElement("div");
        tarjeta.className = "tarjeta-pedido";

        // Convertir la lista de detalles (strings) en <li> de HTML
        // Ajusta esto según cómo devuelva tu API los detalles
        let listaHTML = "";
        if(pedido.detalles) {
            pedido.detalles.forEach(det => {
                listaHTML += `<li>${det}</li>`;
            });
        } else {
            listaHTML = "<li>Detalles no disponibles</li>";
        }

        tarjeta.innerHTML = `
            <div class="tarjeta-header">
                <h3>Ticket #${pedido.numeroTicket}</h3>
                <span>${pedido.cliente || 'Cliente'}</span>
            </div>
            <div class="tarjeta-body">
                <ul>${listaHTML}</ul>
            </div>
            <button class="btn-despachar" onclick="despacharPedido(${pedido.idPedido})">✅ DESPACHAR</button>
        `;
        contenedor.appendChild(tarjeta);
    });
}

async function despacharPedido(idPedido) {
    try {
        const response = await fetch(`${URL_BASE}/api/web/pedidos/despachar/${idPedido}`, {
            method: 'POST'
        });

        if(response.ok) {
            cargarPedidos(); // Recargamos para que desaparezca la tarjeta
        }
    } catch (error) {
        console.error("Error despachando:", error);
    }
}