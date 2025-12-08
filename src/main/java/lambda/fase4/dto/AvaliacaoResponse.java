package lambda.fase4.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de avaliações.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvaliacaoResponse {
    private String id;
    private String descricao;
    private Integer nota;
    private String dataEnvio;
    private String urgencia;
    private String mensagem;
}

