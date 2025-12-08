package lambda.fase4.service;

import lambda.fase4.dto.NotificacaoUrgenciaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Serviço responsável pelo envio de notificações.
 * Utiliza SNS para notificações simples e SES para e-mails formatados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final SnsClient snsClient;
    private final SesClient sesClient;
    private final CloudWatchMetricsService metricsService;

    @Value("${aws.sns.topic.urgencia.arn}")
    private String snsTopicArn;

    @Value("${aws.ses.from.email}")
    private String fromEmail;

    @Value("${aws.ses.admin.emails}")
    private String adminEmails;

    /**
     * Envia notificação de urgência para administradores.
     * Utiliza SNS para notificação instantânea e SES para e-mail detalhado.
     */
    public void enviarNotificacaoUrgencia(NotificacaoUrgenciaDTO notificacao) {
        log.info("Enviando notificação de urgência para avaliação: {}", notificacao.getAvaliacaoId());

        try {
            // Envia via SNS (notificação push)
            enviarViaSns(notificacao);

            // Envia via SES (e-mail)
            enviarViaEmail(notificacao);

            metricsService.registrarNotificacaoEnviada("urgencia");
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de urgência", e);
            metricsService.registrarErro("notificacao_urgencia");
            throw new RuntimeException("Falha no envio de notificação", e);
        }
    }

    /**
     * Envia notificação via SNS Topic.
     */
    private void enviarViaSns(NotificacaoUrgenciaDTO notificacao) {
        try {
            String message = formatarMensagemSns(notificacao);

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .subject("ALERTA: Avaliação Crítica Recebida")
                    .message(message)
                    .build();

            snsClient.publish(publishRequest);
            log.info("Notificação SNS enviada com sucesso");
        } catch (Exception e) {
            log.error("Erro ao enviar via SNS", e);
            throw e;
        }
    }

    /**
     * Envia e-mail via Amazon SES.
     */
    private void enviarViaEmail(NotificacaoUrgenciaDTO notificacao) {
        try {
            String htmlBody = formatarEmailHtml(notificacao);
            String textBody = formatarEmailTexto(notificacao);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder()
                            .toAddresses(adminEmails.split(","))
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("URGENTE: Feedback Crítico Recebido - Nota " + notificacao.getNota())
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).build())
                                    .text(Content.builder().data(textBody).build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();

            sesClient.sendEmail(emailRequest);
            log.info("E-mail SES enviado com sucesso para: {}", adminEmails);
        } catch (Exception e) {
            log.error("Erro ao enviar via SES", e);
            throw e;
        }
    }

    /**
     * Formata mensagem para SNS.
     */
    private String formatarMensagemSns(NotificacaoUrgenciaDTO notificacao) {
        return String.format("""
                ⚠️ AVALIAÇÃO CRÍTICA RECEBIDA
                
                ID: %s
                Nota: %d/10
                Urgência: %s
                Data: %s
                
                Descrição:
                %s
                
                Ação necessária: Verificar imediatamente esta avaliação e tomar medidas corretivas.
                """,
                notificacao.getAvaliacaoId(),
                notificacao.getNota(),
                notificacao.getUrgencia(),
                notificacao.getDataEnvio(),
                notificacao.getDescricao()
        );
    }

    /**
     * Formata e-mail em HTML.
     */
    private String formatarEmailHtml(NotificacaoUrgenciaDTO notificacao) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #d32f2f; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f5f5f5; padding: 20px; margin-top: 20px; }
                        .field { margin: 10px 0; }
                        .label { font-weight: bold; }
                        .alert { background-color: #fff3cd; border-left: 4px solid #ff6f00; padding: 15px; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>⚠️ ALERTA DE AVALIAÇÃO CRÍTICA</h1>
                        </div>
                        <div class="content">
                            <div class="alert">
                                <strong>Atenção!</strong> Uma avaliação crítica foi recebida e requer ação imediata.
                            </div>
                            
                            <div class="field">
                                <span class="label">ID da Avaliação:</span> %s
                            </div>
                            <div class="field">
                                <span class="label">Nota:</span> %d/10
                            </div>
                            <div class="field">
                                <span class="label">Urgência:</span> %s
                            </div>
                            <div class="field">
                                <span class="label">Data de Envio:</span> %s
                            </div>
                            <div class="field">
                                <span class="label">Descrição:</span>
                                <p>%s</p>
                            </div>
                            
                            <div class="alert">
                                <strong>Próximos passos:</strong>
                                <ul>
                                    <li>Analisar o feedback detalhadamente</li>
                                    <li>Identificar pontos de melhoria no curso</li>
                                    <li>Entrar em contato com o aluno (se possível)</li>
                                    <li>Implementar ações corretivas</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """,
                notificacao.getAvaliacaoId(),
                notificacao.getNota(),
                notificacao.getUrgencia(),
                notificacao.getDataEnvio(),
                notificacao.getDescricao()
        );
    }

    /**
     * Formata e-mail em texto simples.
     */
    private String formatarEmailTexto(NotificacaoUrgenciaDTO notificacao) {
        return String.format("""
                ========================================
                ALERTA DE AVALIAÇÃO CRÍTICA
                ========================================
                
                ID da Avaliação: %s
                Nota: %d/10
                Urgência: %s
                Data de Envio: %s
                
                Descrição:
                %s
                
                ========================================
                AÇÃO NECESSÁRIA
                ========================================
                Por favor, verifique esta avaliação imediatamente
                e tome as medidas necessárias para melhorar a
                qualidade do curso.
                """,
                notificacao.getAvaliacaoId(),
                notificacao.getNota(),
                notificacao.getUrgencia(),
                notificacao.getDataEnvio(),
                notificacao.getDescricao()
        );
    }
}

