package AttizosBackend.service;

import AttizosBackend.model.Empleado;
import AttizosBackend.repository.EmpleadoRepository;
import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class EmpleadoService {
    @Autowired
    private EmpleadoRepository empleadoRepository;
    @Autowired
    private SyncSocketHandler socketHandler;


    public List<Empleado> obtenerEmpleadosActivos(){
        return empleadoRepository.findByEstado("Activo");
    }

    public Empleado guardarEmpleado(Empleado empleado){
        Empleado guardado =  empleadoRepository.save(empleado);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_EMPLEADOS\"}");
            }
        });
        return guardado;
    }
    public Empleado actualizarEmpleado(Empleado empleado) {
        empleadoRepository.actualizarDatosDirecto(
                empleado.getNombre(),
                empleado.getCargo(),
                empleado.getSueldo(),
                empleado.getUsername(),
                empleado.getPasswordHash(),
                empleado.getIdEmpleado().trim()
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                socketHandler.notificarAClientes("{\"evento\": \"SYNC_EMPLEADOS\"}");
            }
        });
        return empleado;
    }
    public boolean inactivarEmpleado(String id) {
        int filasAfectadas = empleadoRepository.inactivarEmpleadoDirecto(id.trim());
        boolean exito =  filasAfectadas > 0;
        if(exito){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_EMPLEADOS\"}");
                }
            });
        }
        return exito;
    }

    public boolean registrarFechaPago(String id){
        int filasAfectadas = empleadoRepository.actualizarFechaPagoDirecto(id.trim()); 
        boolean exito = filasAfectadas > 0;
        if(exito){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_EMPLEADOS\"}");
                    socketHandler.notificarAClientes("{\"evento\": \"SYNC_REPORTES\"}");
                }
            });
        }
        return exito;
    }
}
