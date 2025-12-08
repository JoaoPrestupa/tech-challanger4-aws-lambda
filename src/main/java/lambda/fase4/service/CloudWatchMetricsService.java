package lambda.fase4.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;

/**
 * Serviço responsável por enviar métricas customizadas para o CloudWatch.
 * Permite monitoramento em tempo real da aplicação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudWatchMetricsService {

    private final CloudWatchClient cloudWatchClient;
    private static final String NAMESPACE = "FeedbackSystem";

    /**
     * Registra métrica de avaliação recebida.
     */
    public void registrarAvaliacaoRecebida(String urgencia) {
        try {
            Dimension dimension = Dimension.builder()
                    .name("Urgencia")
                    .value(urgencia)
                    .build();

            MetricDatum datum = MetricDatum.builder()
                    .metricName("AvaliacoesRecebidas")
                    .unit(StandardUnit.COUNT)
                    .value(1.0)
                    .timestamp(Instant.now())
                    .dimensions(dimension)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
            log.debug("Métrica de avaliação registrada: urgencia={}", urgencia);
        } catch (Exception e) {
            log.error("Erro ao registrar métrica no CloudWatch", e);
        }
    }

    /**
     * Registra métrica de mensagem enviada para fila.
     */
    public void registrarMensagemEnviadaFila(String tipoFila) {
        try {
            Dimension dimension = Dimension.builder()
                    .name("TipoFila")
                    .value(tipoFila)
                    .build();

            MetricDatum datum = MetricDatum.builder()
                    .metricName("MensagensEnviadasFila")
                    .unit(StandardUnit.COUNT)
                    .value(1.0)
                    .timestamp(Instant.now())
                    .dimensions(dimension)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception e) {
            log.error("Erro ao registrar métrica de fila no CloudWatch", e);
        }
    }

    /**
     * Registra métrica de notificação enviada.
     */
    public void registrarNotificacaoEnviada(String tipo) {
        try {
            Dimension dimension = Dimension.builder()
                    .name("TipoNotificacao")
                    .value(tipo)
                    .build();

            MetricDatum datum = MetricDatum.builder()
                    .metricName("NotificacoesEnviadas")
                    .unit(StandardUnit.COUNT)
                    .value(1.0)
                    .timestamp(Instant.now())
                    .dimensions(dimension)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception e) {
            log.error("Erro ao registrar métrica de notificação no CloudWatch", e);
        }
    }

    /**
     * Registra métrica de erro.
     */
    public void registrarErro(String tipoErro) {
        try {
            Dimension dimension = Dimension.builder()
                    .name("TipoErro")
                    .value(tipoErro)
                    .build();

            MetricDatum datum = MetricDatum.builder()
                    .metricName("Erros")
                    .unit(StandardUnit.COUNT)
                    .value(1.0)
                    .timestamp(Instant.now())
                    .dimensions(dimension)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception e) {
            log.error("Erro ao registrar métrica de erro no CloudWatch", e);
        }
    }

    /**
     * Registra métrica de relatório gerado.
     */
    public void registrarRelatorioGerado() {
        try {
            MetricDatum datum = MetricDatum.builder()
                    .metricName("RelatoriosGerados")
                    .unit(StandardUnit.COUNT)
                    .value(1.0)
                    .timestamp(Instant.now())
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception e) {
            log.error("Erro ao registrar métrica de relatório no CloudWatch", e);
        }
    }
}

