package lambda.fase4.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma avaliação de feedback do sistema.
 * Armazenada em DynamoDB para alta disponibilidade e escalabilidade.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "avaliacoes")
@DynamoDbBean
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Column(nullable = false)
    private Integer nota; // 0 a 10

    @Column(nullable = false)
    private LocalDateTime dataEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgencia urgencia;

    @Column
    private boolean notificacaoEnviada;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSortKey
    public String getDataEnvioString() {
        return dataEnvio != null ? dataEnvio.toString() : null;
    }

    /**
     * Calcula a urgência baseada na nota recebida.
     * Notas 0-3: CRITICA
     * Notas 4-6: MEDIA
     * Notas 7-10: BAIXA
     */
    public void calcularUrgencia() {
        if (nota <= 3) {
            this.urgencia = Urgencia.CRITICA;
        } else if (nota <= 6) {
            this.urgencia = Urgencia.MEDIA;
        } else {
            this.urgencia = Urgencia.BAIXA;
        }
    }

    public enum Urgencia {
        CRITICA,
        MEDIA,
        BAIXA
    }
}

