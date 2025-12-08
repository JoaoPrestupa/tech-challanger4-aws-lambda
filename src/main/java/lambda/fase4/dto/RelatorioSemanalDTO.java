package lambda.fase4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para relatório semanal de avaliações.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatorioSemanalDTO {
    private String periodoInicio;
    private String periodoFim;
    private Integer totalAvaliacoes;
    private Double mediaNotas;
    private Map<String, Long> quantidadePorDia; // Data -> Quantidade
    private Map<String, Long> quantidadePorUrgencia; // Urgencia -> Quantidade
    private String dataGeracao;
}


