package lambda.fase4.service;

import lambda.fase4.dto.RelatorioSemanalDTO;
import lambda.fase4.model.Avaliacao;
import lambda.fase4.repository.AvaliacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela geração de relatórios periódicos.
 * Gera relatórios semanais com estatísticas de avaliações.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelatorioService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final SesClient sesClient;
    private final CloudWatchMetricsService metricsService;

    @Value("${aws.ses.from.email}")
    private String fromEmail;

    @Value("${aws.ses.admin.emails}")
    private String adminEmails;

    /**
     * Gera relatório semanal de avaliações.
     * Calcula estatísticas dos últimos 7 dias.
     */
    public RelatorioSemanalDTO gerarRelatorioSemanal() {
        log.info("Gerando relatório semanal de avaliações");

        LocalDateTime fim = LocalDateTime.now();
        LocalDateTime inicio = fim.minusDays(7);

        List<Avaliacao> avaliacoes = avaliacaoRepository.findByDataEnvioBetween(inicio, fim);

        // Calcula métricas
        Double mediaNotas = avaliacoes.stream()
                .mapToInt(Avaliacao::getNota)
                .average()
                .orElse(0.0);

        Map<String, Long> quantidadePorDia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDataEnvio().toLocalDate().toString(),
                        Collectors.counting()
                ));

        Map<String, Long> quantidadePorUrgencia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getUrgencia().name(),
                        Collectors.counting()
                ));

        RelatorioSemanalDTO relatorio = RelatorioSemanalDTO.builder()
                .periodoInicio(inicio.format(DateTimeFormatter.ISO_DATE_TIME))
                .periodoFim(fim.format(DateTimeFormatter.ISO_DATE_TIME))
                .totalAvaliacoes(avaliacoes.size())
                .mediaNotas(mediaNotas)
                .quantidadePorDia(quantidadePorDia)
                .quantidadePorUrgencia(quantidadePorUrgencia)
                .dataGeracao(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

        log.info("Relatório gerado: {} avaliações, média {}", relatorio.getTotalAvaliacoes(), relatorio.getMediaNotas());
        metricsService.registrarRelatorioGerado();

        return relatorio;
    }

    /**
     * Envia relatório semanal por e-mail.
     */
    public void enviarRelatorioSemanal(RelatorioSemanalDTO relatorio) {
        log.info("Enviando relatório semanal por e-mail");

        try {
            String htmlBody = formatarRelatorioHtml(relatorio);
            String textBody = formatarRelatorioTexto(relatorio);

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder()
                            .toAddresses(adminEmails.split(","))
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("Relatório Semanal de Feedbacks - " + LocalDate.now())
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).build())
                                    .text(Content.builder().data(textBody).build())
                                    .build())
                            .build())
                    .source(fromEmail)
                    .build();

            sesClient.sendEmail(emailRequest);
            log.info("Relatório enviado com sucesso para: {}", adminEmails);
            metricsService.registrarNotificacaoEnviada("relatorio");
        } catch (Exception e) {
            log.error("Erro ao enviar relatório por e-mail", e);
            metricsService.registrarErro("envio_relatorio");
            throw new RuntimeException("Falha no envio do relatório", e);
        }
    }

    /**
     * Formata relatório em HTML.
     */
    private String formatarRelatorioHtml(RelatorioSemanalDTO relatorio) {
        StringBuilder porDiaHtml = new StringBuilder();
        relatorio.getQuantidadePorDia().forEach((dia, qtd) ->
                porDiaHtml.append(String.format("<tr><td>%s</td><td>%d</td></tr>", dia, qtd))
        );

        StringBuilder porUrgenciaHtml = new StringBuilder();
        relatorio.getQuantidadePorUrgencia().forEach((urgencia, qtd) ->
                porUrgenciaHtml.append(String.format("<tr><td>%s</td><td>%d</td></tr>", urgencia, qtd))
        );

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; }
                        .container { max-width: 800px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #1976d2; color: white; padding: 20px; text-align: center; }
                        .summary { background-color: #e3f2fd; padding: 20px; margin: 20px 0; border-radius: 5px; }
                        .metric { display: inline-block; margin: 10px 20px; text-align: center; }
                        .metric-value { font-size: 32px; font-weight: bold; color: #1976d2; }
                        .metric-label { color: #666; }
                        table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
                        th { background-color: #1976d2; color: white; }
                        .section-title { color: #1976d2; margin-top: 30px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>📊 Relatório Semanal de Feedbacks</h1>
                            <p>Período: %s até %s</p>
                        </div>
                        
                        <div class="summary">
                            <div class="metric">
                                <div class="metric-value">%d</div>
                                <div class="metric-label">Total de Avaliações</div>
                            </div>
                            <div class="metric">
                                <div class="metric-value">%.2f</div>
                                <div class="metric-label">Média das Notas</div>
                            </div>
                        </div>
                        
                        <h2 class="section-title">📅 Avaliações por Dia</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Data</th>
                                    <th>Quantidade</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                        
                        <h2 class="section-title">⚠️ Avaliações por Urgência</h2>
                        <table>
                            <thead>
                                <tr>
                                    <th>Urgência</th>
                                    <th>Quantidade</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                        
                        <div style="margin-top: 40px; padding: 20px; background-color: #fff3cd; border-left: 4px solid #ff6f00;">
                            <strong>📈 Análise Rápida:</strong>
                            <ul>
                                <li>Média geral: %.2f/10 (%s)</li>
                                <li>Total de feedbacks críticos: %d</li>
                                <li>Taxa de resposta diária: %.1f avaliações/dia</li>
                            </ul>
                        </div>
                        
                        <p style="margin-top: 30px; color: #666; font-size: 12px;">
                            Relatório gerado automaticamente em %s
                        </p>
                    </div>
                </body>
                </html>
                """,
                relatorio.getPeriodoInicio().substring(0, 10),
                relatorio.getPeriodoFim().substring(0, 10),
                relatorio.getTotalAvaliacoes(),
                relatorio.getMediaNotas(),
                porDiaHtml.toString(),
                porUrgenciaHtml.toString(),
                relatorio.getMediaNotas(),
                getAvaliacaoQualitativa(relatorio.getMediaNotas()),
                relatorio.getQuantidadePorUrgencia().getOrDefault("CRITICA", 0L),
                relatorio.getTotalAvaliacoes() / 7.0,
                relatorio.getDataGeracao()
        );
    }

    /**
     * Formata relatório em texto simples.
     */
    private String formatarRelatorioTexto(RelatorioSemanalDTO relatorio) {
        StringBuilder porDia = new StringBuilder();
        relatorio.getQuantidadePorDia().forEach((dia, qtd) ->
                porDia.append(String.format("  - %s: %d avaliações\n", dia, qtd))
        );

        StringBuilder porUrgencia = new StringBuilder();
        relatorio.getQuantidadePorUrgencia().forEach((urgencia, qtd) ->
                porUrgencia.append(String.format("  - %s: %d avaliações\n", urgencia, qtd))
        );

        return String.format("""
                ==========================================
                RELATÓRIO SEMANAL DE FEEDBACKS
                ==========================================
                
                Período: %s até %s
                
                RESUMO GERAL
                ==========================================
                Total de Avaliações: %d
                Média das Notas: %.2f/10 (%s)
                
                AVALIAÇÕES POR DIA
                ==========================================
                %s
                
                AVALIAÇÕES POR URGÊNCIA
                ==========================================
                %s
                
                ANÁLISE
                ==========================================
                - Feedbacks críticos: %d
                - Taxa diária: %.1f avaliações/dia
                
                Relatório gerado em: %s
                """,
                relatorio.getPeriodoInicio().substring(0, 10),
                relatorio.getPeriodoFim().substring(0, 10),
                relatorio.getTotalAvaliacoes(),
                relatorio.getMediaNotas(),
                getAvaliacaoQualitativa(relatorio.getMediaNotas()),
                porDia.toString(),
                porUrgencia.toString(),
                relatorio.getQuantidadePorUrgencia().getOrDefault("CRITICA", 0L),
                relatorio.getTotalAvaliacoes() / 7.0,
                relatorio.getDataGeracao()
        );
    }

    /**
     * Retorna avaliação qualitativa da média.
     */
    private String getAvaliacaoQualitativa(Double media) {
        if (media >= 8.0) return "Excelente";
        if (media >= 6.0) return "Bom";
        if (media >= 4.0) return "Regular";
        return "Crítico";
    }
}

