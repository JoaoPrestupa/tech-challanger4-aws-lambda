package lambda.fase4.service;

import com.google.gson.Gson;
import lambda.fase4.dto.AvaliacaoRequest;
import lambda.fase4.dto.NotificacaoUrgenciaDTO;
import lambda.fase4.model.Avaliacao;
import lambda.fase4.repository.AvaliacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de avaliações.
 * Implementa a lógica de negócio para receber, processar e armazenar feedbacks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final SqsClient sqsClient;
    private final CloudWatchMetricsService metricsService;
    private final Gson gson;

    @Value("${aws.sqs.queue.notificacao.url}")
    private String notificacaoQueueUrl;

    /**
     * Processa uma nova avaliação recebida.
     * Se a avaliação for crítica (nota <= 3), envia para fila de notificações.
     */
    @Transactional
    public Avaliacao processarAvaliacao(AvaliacaoRequest request) {
        log.info("Processando nova avaliação: nota={}", request.getNota());

        // Cria a entidade
        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setDescricao(request.getDescricao());
        avaliacao.setNota(request.getNota());
        avaliacao.setDataEnvio(LocalDateTime.now());
        avaliacao.calcularUrgencia();
        avaliacao.setNotificacaoEnviada(false);

        // Salva no banco de dados
        avaliacao = avaliacaoRepository.save(avaliacao);
        log.info("Avaliação salva com ID: {} e urgência: {}", avaliacao.getId(), avaliacao.getUrgencia());

        // Envia métrica para CloudWatch
        metricsService.registrarAvaliacaoRecebida(avaliacao.getUrgencia().name());

        // Se for crítica, envia para fila de notificações
        if (avaliacao.getUrgencia() == Avaliacao.Urgencia.CRITICA) {
            enviarParaFilaNotificacao(avaliacao);
        }

        return avaliacao;
    }

    /**
     * Envia avaliação para fila SQS de notificações críticas.
     */
    private void enviarParaFilaNotificacao(Avaliacao avaliacao) {
        try {
            NotificacaoUrgenciaDTO dto = NotificacaoUrgenciaDTO.builder()
                    .avaliacaoId(avaliacao.getId())
                    .descricao(avaliacao.getDescricao())
                    .urgencia(avaliacao.getUrgencia().name())
                    .dataEnvio(avaliacao.getDataEnvio().toString())
                    .nota(avaliacao.getNota())
                    .build();

            String messageBody = gson.toJson(dto);

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(notificacaoQueueUrl)
                    .messageBody(messageBody)
                    .build();

            sqsClient.sendMessage(sendMessageRequest);
            log.info("Avaliação crítica enviada para fila de notificações: {}", avaliacao.getId());

            metricsService.registrarMensagemEnviadaFila("notificacao");
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para fila SQS", e);
            metricsService.registrarErro("envio_fila_notificacao");
        }
    }

    /**
     * Busca avaliações por período (usado para geração de relatórios).
     */
    public List<Avaliacao> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return avaliacaoRepository.findByDataEnvioBetween(inicio, fim);
    }

    /**
     * Marca avaliação como notificada.
     */
    @Transactional
    public void marcarComoNotificada(String avaliacaoId) {
        avaliacaoRepository.findById(avaliacaoId).ifPresent(avaliacao -> {
            avaliacao.setNotificacaoEnviada(true);
            avaliacaoRepository.save(avaliacao);
            log.info("Avaliação {} marcada como notificada", avaliacaoId);
        });
    }
}

