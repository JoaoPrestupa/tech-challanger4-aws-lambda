package lambda.fase4.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import lambda.fase4.dto.AvaliacaoRequest;
import lambda.fase4.dto.AvaliacaoResponse;
import lambda.fase4.model.Avaliacao;
import lambda.fase4.service.AvaliacaoService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Função Lambda 1: Recebimento de Feedbacks
 *
 * Responsabilidade Única: Receber e processar avaliações enviadas pelos alunos.
 *
 * Trigger: API Gateway (POST /avaliacao)
 *
 * Funcionalidades:
 * - Valida dados de entrada
 * - Salva avaliação no banco de dados
 * - Calcula urgência automaticamente
 * - Envia avaliações críticas para fila SQS
 * - Registra métricas no CloudWatch
 *
 * Segurança:
 * - Validação de entrada com Jakarta Validation
 * - IAM Role com permissões mínimas (DynamoDB:PutItem, SQS:SendMessage, CloudWatch:PutMetricData)
 * - Logs estruturados para auditoria
 */
@Component
public class ReceberFeedbackHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final Gson gson = new Gson();
    private AvaliacaoService avaliacaoService;

    public ReceberFeedbackHandler() {
        // Inicializa contexto Spring para injeção de dependências
        initializeSpringContext();
    }

    private void initializeSpringContext() {
        try {
            AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
            context.scan("lambda.fase4");
            context.refresh();
            this.avaliacaoService = context.getBean(AvaliacaoService.class);
        } catch (Exception e) {
            System.err.println("Erro ao inicializar contexto Spring: " + e.getMessage());
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        context.getLogger().log("Processando requisição de feedback");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(getCorsHeaders());

        try {
            // Parse do body
            String body = input.getBody();
            AvaliacaoRequest request = gson.fromJson(body, AvaliacaoRequest.class);

            // Validação básica
            if (request.getDescricao() == null || request.getDescricao().isBlank()) {
                return createErrorResponse(400, "Descrição é obrigatória");
            }
            if (request.getNota() == null || request.getNota() < 0 || request.getNota() > 10) {
                return createErrorResponse(400, "Nota deve estar entre 0 e 10");
            }

            // Processa avaliação
            Avaliacao avaliacao = avaliacaoService.processarAvaliacao(request);

            // Cria resposta
            AvaliacaoResponse avaliacaoResponse = new AvaliacaoResponse(
                    avaliacao.getId(),
                    avaliacao.getDescricao(),
                    avaliacao.getNota(),
                    avaliacao.getDataEnvio().toString(),
                    avaliacao.getUrgencia().name(),
                    "Avaliação recebida com sucesso!"
            );

            response.setStatusCode(201);
            response.setBody(gson.toJson(avaliacaoResponse));
            context.getLogger().log("Avaliação processada com sucesso: " + avaliacao.getId());

        } catch (Exception e) {
            context.getLogger().log("Erro ao processar avaliação: " + e.getMessage());
            return createErrorResponse(500, "Erro interno ao processar avaliação");
        }

        return response;
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(getCorsHeaders());
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);
        response.setBody(gson.toJson(errorBody));
        return response;
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type");
        return headers;
    }
}

