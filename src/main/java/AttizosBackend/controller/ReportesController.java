package AttizosBackend.controller;

import AttizosBackend.model.ReporteFinanciero;
import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    @Autowired
    private JdbcTemplate db;
    @Autowired
    private SyncSocketHandler socketHandler;

    @GetMapping("/consolidado")
    public ReporteFinanciero obtenerReporteConsolidado(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fin) {

        ReporteFinanciero reporte = new ReporteFinanciero();

        String filtroFecha = "";
        if (inicio != null && fin != null && !inicio.isEmpty() && !fin.isEmpty()) {
            filtroFecha = " AND CAST(fecha_hora AS DATE) BETWEEN '" + inicio + "' AND '" + fin + "' ";
        }

        String sqlFacturas = "SELECT * FROM facturas WHERE estado != 'Anulada' " + filtroFecha + " ORDER BY fecha_hora DESC";
        List<Map<String, Object>> facturas = db.queryForList(sqlFacturas);
        reporte.setFacturas(facturas);

        double ingresos = 0;
        double ingresosEfectivo = 0;
        double ingresosQR = 0;
        Map<String, Double> ventasXDia = new TreeMap<>();
        for(Map<String, Object> f : facturas) {
            double total = ((Number)f.get("total")).doubleValue();
            ingresos += total;

            String metodo = (String) f.get("metodo_pago");
            if("QR".equalsIgnoreCase(metodo)){
                ingresosQR += total;
            }else{
                ingresosEfectivo += total;
            }

            java.sql.Timestamp ts = (java.sql.Timestamp) f.get("fecha_hora");
            String dia = ts.toLocalDateTime().toLocalDate().toString();
            ventasXDia.put(dia, ventasXDia.getOrDefault(dia, 0.0) + total);
        }
        reporte.setTotalIngresos(ingresos);
        reporte.setTotalEfectivo(ingresosEfectivo);
        reporte.setTotalQR(ingresosQR);
        reporte.setCantidadVentas(facturas.size());
        reporte.setVentasPorDia(ventasXDia);

        String filtroEgreso = "";
        if (inicio != null && fin != null && !inicio.isEmpty() && !fin.isEmpty()) {
            filtroEgreso = " WHERE CAST(fecha AS DATE) BETWEEN '" + inicio + "' AND '" + fin + "' ";
        }
        String sqlEgresos = "SELECT * FROM egresos " + filtroEgreso + " ORDER BY fecha DESC";
        List<Map<String, Object>> egresos = db.queryForList(sqlEgresos);
        reporte.setEgresos(egresos);

        double sumaEgresos = 0;
        for(Map<String, Object> e : egresos) sumaEgresos += ((Number)e.get("monto")).doubleValue();
        reporte.setTotalEgresos(sumaEgresos);

        List<Map<String, Object>> empleados = db.queryForList("SELECT id_empleado, nombre, cargo, sueldo, username FROM empleados WHERE estado = 'Activo'");
        reporte.setEmpleados(empleados);
        double sumaSueldos = 0;
        for(Map<String, Object> e : empleados) sumaSueldos += ((Number)e.get("sueldo")).doubleValue();
        reporte.setTotalSueldos(sumaSueldos);

        String filtroAudit = "";
        if (inicio != null && fin != null && !inicio.isEmpty() && !fin.isEmpty()) {
            filtroAudit = " WHERE CAST(fecha_hora AS DATE) BETWEEN '" + inicio + "' AND '" + fin + "' ";
        }
        String sqlAudit = "SELECT * FROM auditoria " + filtroAudit + " ORDER BY fecha_hora DESC";
        reporte.setAuditoria(db.queryForList(sqlAudit));

        return reporte;
    }

    @PostMapping("/egreso")
    public boolean registrarEgreso(@RequestParam String descripcion, @RequestParam double monto) {
        String sql = "INSERT INTO egresos (concepto, monto, fecha) VALUES (?, ?, CURRENT_TIMESTAMP)";
        boolean exito = db.update(sql, descripcion, monto) > 0;
        if(exito){
            socketHandler.notificarAClientes("{\"evento\": \"SYNC_REPORTES\"}");
        }
        return exito;
    }
    @GetMapping("/egreso/existe")
    public boolean existeEgreso(@RequestParam String concepto) {
        String sql = "SELECT COUNT(*) FROM egresos WHERE concepto = ?";
        Integer count = db.queryForObject(sql, Integer.class, concepto);
        return count != null && count > 0;
    }
    @PostMapping("/auditoria")
    public boolean registrarAuditoria(@RequestBody Map<String, Object> datos){
        try{
            String sql = "INSERT INTO auditoria (operador, tipo_area, nombre_item, accion, cantidad, motivo, fecha_hora) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";

            String operador = (String) datos.getOrDefault("operador", "Sistema");
            String tipoArea = (String) datos.getOrDefault("tipoArea", "General");
            String nombreItem = (String) datos.getOrDefault("nombreItem", "Varios");
            String accion = (String) datos.getOrDefault("accion", "Acción");

            double cantidad = 0.0;
            if (datos.get("cantidad") != null) {
                cantidad = ((Number) datos.get("cantidad")).doubleValue();
            }

            String motivo = (String) datos.getOrDefault("motivo", "");

            boolean exito = db.update(sql, operador, tipoArea, nombreItem, accion, cantidad, motivo) > 0;
            if (exito) {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_REPORTES\"}");
            }
            return exito;
        }catch (Exception e){
            return false;
        }
    }
}