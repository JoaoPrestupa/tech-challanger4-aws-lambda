package lambda.fase4.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa uma avaliação de feedback do sistema.
 * Armazenada em RDS PostgreSQL para garantir consistência transacional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "avaliacoes")
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

