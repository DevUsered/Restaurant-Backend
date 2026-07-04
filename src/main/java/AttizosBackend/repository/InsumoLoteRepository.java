package AttizosBackend.repository;

import AttizosBackend.model.InsumoLote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InsumoLoteRepository extends JpaRepository<InsumoLote, Integer> {

    List<InsumoLote> findByCodigoInsumoAndEstado(String codigoInsumo, String estado);

    List<InsumoLote> findByEstadoAndFechaVencimientoBefore(String estado, LocalDate fechaMaxima);
}