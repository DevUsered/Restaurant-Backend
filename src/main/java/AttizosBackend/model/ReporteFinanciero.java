package AttizosBackend.model;

import java.util.List;
import java.util.Map;

public class ReporteFinanciero {
    private double totalIngresos;
    private double totalEgresos;
    private double totalSueldos;
    private int cantidadVentas;
    private List<Map<String, Object>> facturas;
    private List<Map<String, Object>> egresos;
    private List<Map<String, Object>> empleados;
    private List<Map<String, Object>> auditoria;
    private Map<String, Double> ventasPorDia;
    private double totalEfectivo;
    private double totalQR;

    public double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public double getTotalSueldos() {
        return totalSueldos;
    }

    public void setTotalSueldos(double totalSueldos) {
        this.totalSueldos = totalSueldos;
    }

    public int getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(int cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }

    public List<Map<String, Object>> getFacturas() {
        return facturas;
    }

    public void setFacturas(List<Map<String, Object>> facturas) {
        this.facturas = facturas;
    }

    public List<Map<String, Object>> getEgresos() {
        return egresos;
    }

    public void setEgresos(List<Map<String, Object>> egresos) {
        this.egresos = egresos;
    }

    public List<Map<String, Object>> getEmpleados() {
        return empleados;
    }

    public void setEmpleados(List<Map<String, Object>> empleados) {
        this.empleados = empleados;
    }

    public List<Map<String, Object>> getAuditoria() {
        return auditoria;
    }

    public void setAuditoria(List<Map<String, Object>> auditoria) {
        this.auditoria = auditoria;
    }

    public Map<String, Double> getVentasPorDia() {
        return ventasPorDia;
    }

    public void setVentasPorDia(Map<String, Double> ventasPorDia) {
        this.ventasPorDia = ventasPorDia;
    }

    public double getTotalEfectivo() {
        return totalEfectivo;
    }

    public void setTotalEfectivo(double totalEfectivo) {
        this.totalEfectivo = totalEfectivo;
    }

    public double getTotalQR() {
        return totalQR;
    }

    public void setTotalQR(double totalQR) {
        this.totalQR = totalQR;
    }
}
