package lambda.fase4.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import lambda.fase4.dto.RelatorioSemanalDTO;
import lambda.fase4.service.RelatorioService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Função Lambda 3: Geração de Relatórios
 *
 * Responsabilidade Única: Gerar e enviar relatórios semanais de avaliações.
 *
 * Trigger: EventBridge (CloudWatch Events) - Cron semanal
 * Cron: cron(0 9 ? * MON *) - Toda segunda-feira às 9h UTC
 *
 * Funcionalidades:
 * - Busca avaliações dos últimos 7 dias
 * - Calcula estatísticas (média, quantidades por dia/urgência)
 * - Formata relatório em HTML e texto
 * - Envia por e-mail via SES
 * - Registra métricas no CloudWatch
 *
 * Segurança:
 * - IAM Role com permissões: DynamoDB:Query, SES:SendEmail, CloudWatch:PutMetricData
 * - Logs estruturados para auditoria
 *
 * Configurações:
 * - Timeout: 5 minutos
 * - Memory: 512 MB
 * - EventBridge Rule: taxa semanal
 */
@Component
public class GerarRelatorioHandler implements RequestHandler<ScheduledEvent, String> {

    private RelatorioService relatorioService;

    public GerarRelatorioHandler() {
        initializeSpringContext();
    }

    private void initializeSpringContext() {
        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.scan("lambda.fase4");
            context.refresh();
            this.relatorioService = context.getBean(RelatorioService.class);
        } catch (Exception e) {
            System.err.println("Erro ao inicializar contexto Spring: " + e.getMessage());
        }
    }

    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        context.getLogger().log("===== INICIANDO GERAÇÃO DE RELATÓRIO SEMANAL =====");
        context.getLogger().log("Event ID: " + event.getId());
        context.getLogger().log("Event Time: " + event.getTime());

        try {
            // Gera o relatório
            context.getLogger().log("Gerando relatório semanal...");
            RelatorioSemanalDTO relatorio = relatorioService.gerarRelatorioSemanal();

            context.getLogger().log(String.format(
                    "Relatório gerado: %d avaliações, média %.2f",
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaNotas()
            ));

            // Envia por e-mail
            context.getLogger().log("Enviando relatório por e-mail...");
            relatorioService.enviarRelatorioSemanal(relatorio);

            String resultado = String.format(
                    "Relatório semanal gerado e enviado com sucesso! " +
                    "Total: %d avaliações, Média: %.2f/10",
                    relatorio.getTotalAvaliacoes(),
                    relatorio.getMediaNotas()
            );

            context.getLogger().log("===== RELATÓRIO CONCLUÍDO COM SUCESSO =====");
            return resultado;

        } catch (Exception e) {
            context.getLogger().log("ERRO ao gerar relatório: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha na geração do relatório semanal", e);
        }
    }
}

