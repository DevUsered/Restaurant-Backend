package AttizosBackend.service;

import AttizosBackend.model.Reserva;
import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.util.List;

@Service
public class ReservaService {
    @Autowired
    private JdbcTemplate db;
    @Autowired
    private SyncSocketHandler socketHandler;

    @Transactional
    public List<Reserva> obtenerReservasPendientesYLimpiar(){
        String sqlLimpieza = "UPDATE reservas SET estado = 'Expirada' " +
                "WHERE estado = 'Pendiente' AND fecha_hora < (CURRENT_TIMESTAMP - INTERVAL '15 minutes')";
        int expiradas = db.update(sqlLimpieza);

        if(expiradas > 0){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_RESERVAS\"}");
                }
            });
        }
        String sqlSelect = "SELECT id_reserva, nombre_cliente, telefono, cantidad_personas, fecha_hora, observaciones, estado " +
                "FROM reservas WHERE estado = 'Pendiente' ORDER BY fecha_hora ASC";

        return db.query(sqlSelect, (rs, rowNum) -> {
            Reserva r = new Reserva();
            r.setId(rs.getString("id_reserva"));
            r.setNombreCliente(rs.getString("nombre_cliente"));
            r.setTelefono(rs.getString("telefono"));
            r.setCantidadPersonas(rs.getInt("cantidad_personas"));

            Timestamp ts = rs.getTimestamp("fecha_hora");
            if (ts != null) r.setFecha(ts.toLocalDateTime());

            r.setObservaciones(rs.getString("observaciones"));
            r.setEstado(rs.getString("estado"));
            return r;
        });
    }
    public boolean insertarReserva(Reserva r){
        String sql = "INSERT INTO reservas (id_reserva, nombre_cliente, telefono, cantidad_personas, fecha_hora, observaciones, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'Pendiente')";

        int filas = db.update(sql, r.getId(), r.getNombreCliente(), r.getTelefono(),
                r.getCantidadPersonas(), Timestamp.valueOf(r.getFecha()), r.getObservaciones());
        if(filas > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_RESERVAS\"}");
                }
            });
        }
        return filas > 0;
    }
    public boolean actualizarEstadoReserva(String idReserva, String nuevoEstado){
        String sql = "UPDATE reservas SET estado = ? WHERE id_reserva = ?";
        boolean exito =  db.update(sql, nuevoEstado, idReserva) > 0;
        if(exito) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_RESERVAS\"}");
                }
            });
        }
        return exito;
    }
}
