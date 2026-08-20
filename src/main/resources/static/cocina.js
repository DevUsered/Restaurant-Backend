// Detecta la IP del servidor Spring Boot automáticamente
const SERVIDOR_IP = window.location.hostname;
const PUERTO = window.location.port || "8080";
const URL_BASE = `http://${SERVIDOR_IP}:${PUERTO}`;
const WS_URL = `ws://${SERVIDOR_IP}:${PUERTO}/ws-sync`;

let socket = null;
let usuarioActual = null;

// ==========================================
// NUEVAS VARIABLES PARA AUDIO Y MEMORIA
// ==========================================
const campanaAudio = new Audio('sounds/cocina.mp3');
let idsPedidosEnPantalla = [];
let esPrimeraCarga = true;

async function iniciarSesion() {
    const user = document.getElementById("txtUsuario").value;
    const pass = document.getElementById("txtPassword").value;
    const lblError = document.getElementById("lblError");

    if (!user || !pass) {
        lblError.innerText = "Llene todos los campos.";
        return;
    }

    // 🔥 ¡EL TRUCO ANTI-CHROME VA AQUÍ ARRIBA! 🔥
    // Lo ejecutamos INMEDIATAMENTE después del clic, antes de que Chrome se ponga estricto
    campanaAudio.volume = 0;
    let playPromise = campanaAudio.play();
    if (playPromise !== undefined) {
        playPromise.then(() => {
            campanaAudio.pause();
            campanaAudio.currentTime = 0;
            campanaAudio.volume = 1.0; // Listo para sonar fuerte más adelante
        }).catch(e => console.log("Chrome requiere interacción adicional."));
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

            document.getElementById("login-container").classList.replace("visible", "hidden");
            document.getElementById("kds-container").classList.replace("hidden", "visible");

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

    // Reiniciamos la memoria al cerrar sesión
    idsPedidosEnPantalla = [];
    esPrimeraCarga = true;
}

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
            // AQUÍ YA NO SUENA. Solo mandamos a recargar la pantalla.
            // La inteligencia del sonido está dentro de cargarPedidos()
            cargarPedidos();
        }
    };

    socket.onclose = () => {
        console.warn("⚠️ WebSocket cerrado. Reintentando en 5 segundos...");
        setTimeout(iniciarWebSocket, 5000);
    };
}

function reproducirCampana() {
    try {
        campanaAudio.currentTime = 0; // Reiniciar por si ya estaba sonando
        campanaAudio.play().catch(e => {
            console.warn("El navegador bloqueó el sonido automático.", e);
        });
    } catch (error) {
        console.error("Error al reproducir audio en la web:", error);
    }
}

async function cargarPedidos() {
    try {
        const response = await fetch(`${URL_BASE}/api/web/pedidos/pendientes`);
        if (response.ok) {
            const pedidos = await response.json();

            // 1. Extraemos solo los IDs de los pedidos que acaban de llegar
            const nuevosIds = pedidos.map(p => p.idPedido);

            // 2. Si NO es la primera vez que carga la pantalla, verificamos qué cambió
            if (!esPrimeraCarga) {
                // Buscamos si hay algún ID nuevo que antes no estaba en nuestra pantalla
                const llegoPedidoNuevo = nuevosIds.some(id => !idsPedidosEnPantalla.includes(id));

                if (llegoPedidoNuevo) {
                    reproducirCampana(); // ¡SUENA SOLO SI HAY UN PEDIDO NUEVO!
                }
            }

            // 3. Actualizamos nuestra memoria para la próxima vez
            idsPedidosEnPantalla = nuevosIds;
            esPrimeraCarga = false;

            // 4. Dibujamos
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
            cargarPedidos();
        }
    } catch (error) {
        console.error("Error despachando:", error);
    }
}