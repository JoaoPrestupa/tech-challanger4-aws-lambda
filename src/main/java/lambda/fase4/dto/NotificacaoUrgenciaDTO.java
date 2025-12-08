package lambda.fase4.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para mensagens de notificação de urgência.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacaoUrgenciaDTO {
    private String avaliacaoId;
    private String descricao;
    private String urgencia;
    private String dataEnvio;
    private Integer nota;
}

