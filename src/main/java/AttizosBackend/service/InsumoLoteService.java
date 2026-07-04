package AttizosBackend.service;

import AttizosBackend.model.InsumoLote;
import AttizosBackend.repository.InsumoLoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InsumoLoteService {

    @Autowired
    private InsumoLoteRepository loteRepository;

    public List<InsumoLote> obtenerLotesPorInsumo(String codigoInsumo) {
        return loteRepository.findByCodigoInsumoAndEstado(codigoInsumo, "Activo");
    }

    public List<InsumoLote> obtenerLotesPorVencer(int diasAviso) {
        LocalDate fechaMaxima = LocalDate.now().plusDays(diasAviso);
        return loteRepository.findByEstadoAndFechaVencimientoBefore("Activo", fechaMaxima);
    }
}