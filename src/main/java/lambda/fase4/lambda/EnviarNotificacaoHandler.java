package lambda.fase4.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import lambda.fase4.dto.NotificacaoUrgenciaDTO;
import lambda.fase4.service.AvaliacaoService;
import lambda.fase4.service.NotificacaoService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Função Lambda 2: Envio de Notificações
 *
 * Responsabilidade Única: Processar fila de feedbacks críticos e enviar notificações.
 *
 * Trigger: SQS Queue (notificacao-urgencia-queue)
 *
 * Funcionalidades:
 * - Consome mensagens da fila SQS
 * - Envia notificação via SNS (push)
 * - Envia e-mail detalhado via SES
 * - Marca avaliação como notificada
 * - Registra métricas no CloudWatch
 *
 * Segurança:
 * - IAM Role com permissões: SQS:ReceiveMessage, SQS:DeleteMessage,
 *   SNS:Publish, SES:SendEmail, DynamoDB:UpdateItem, CloudWatch:PutMetricData
 * - DLQ (Dead Letter Queue) configurada para mensagens com falha
 * - Retry automático com backoff exponencial
 *
 * Configurações:
 * - Batch size: 10 mensagens
 * - Timeout: 5 minutos
 * - Memory: 512 MB
 */
@Component
public class EnviarNotificacaoHandler implements RequestHandler<SQSEvent, Void> {

    private final Gson gson = new Gson();
    private NotificacaoService notificacaoService;
    private AvaliacaoService avaliacaoService;

    public EnviarNotificacaoHandler() {
        initializeSpringContext();
    }

    private void initializeSpringContext() {
        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.scan("lambda.fase4");
            context.refresh();
            this.notificacaoService = context.getBean(NotificacaoService.class);
            this.avaliacaoService = context.getBean(AvaliacaoService.class);
        } catch (Exception e) {
            System.err.println("Erro ao inicializar contexto Spring: " + e.getMessage());
        }
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        context.getLogger().log("Processando " + event.getRecords().size() + " mensagens da fila");

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                context.getLogger().log("Processando mensagem: " + message.getMessageId());

                // Parse da mensagem
                NotificacaoUrgenciaDTO notificacao = gson.fromJson(
                        message.getBody(),
                        NotificacaoUrgenciaDTO.class
                );

                context.getLogger().log(
                        "Enviando notificação para avaliação crítica: " +
                        notificacao.getAvaliacaoId() +
                        " (Nota: " + notificacao.getNota() + ")"
                );

                // Envia notificação
                notificacaoService.enviarNotificacaoUrgencia(notificacao);

                // Marca como notificada no banco
                avaliacaoService.marcarComoNotificada(notificacao.getAvaliacaoId());

                context.getLogger().log("Notificação enviada com sucesso: " + message.getMessageId());

            } catch (Exception e) {
                context.getLogger().log(
                        "ERRO ao processar mensagem " + message.getMessageId() +
                        ": " + e.getMessage()
                );
                // A mensagem será automaticamente enviada para DLQ após max retries
                throw new RuntimeException("Falha no processamento da mensagem", e);
            }
        }

        context.getLogger().log("Processamento de notificações concluído");
        return null;
    }
}

