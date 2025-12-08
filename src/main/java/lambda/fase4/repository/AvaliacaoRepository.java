package lambda.fase4.repository;

import lambda.fase4.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para operações com Avaliação no banco de dados.
 */
@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, String> {

    /**
     * Busca avaliações por urgência.
     */
    List<Avaliacao> findByUrgencia(Avaliacao.Urgencia urgencia);

    /**
     * Busca avaliações críticas que ainda não tiveram notificação enviada.
     */
    List<Avaliacao> findByUrgenciaAndNotificacaoEnviadaFalse(Avaliacao.Urgencia urgencia);

    /**
     * Busca avaliações em um período específico.
     */
    List<Avaliacao> findByDataEnvioBetween(LocalDateTime inicio, LocalDateTime fim);

    /**
     * Calcula a média de notas em um período.
     */
    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.dataEnvio BETWEEN :inicio AND :fim")
    Double calcularMediaNotasPorPeriodo(LocalDateTime inicio, LocalDateTime fim);

    /**
     * Conta avaliações por urgência em um período.
     */
    @Query("SELECT COUNT(a) FROM Avaliacao a WHERE a.urgencia = :urgencia AND a.dataEnvio BETWEEN :inicio AND :fim")
    Long contarPorUrgenciaEPeriodo(Avaliacao.Urgencia urgencia, LocalDateTime inicio, LocalDateTime fim);
}

