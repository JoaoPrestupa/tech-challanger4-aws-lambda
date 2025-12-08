package lambda.fase4.controller;

import jakarta.validation.Valid;
import lambda.fase4.dto.AvaliacaoRequest;
import lambda.fase4.dto.AvaliacaoResponse;
import lambda.fase4.model.Avaliacao;
import lambda.fase4.service.AvaliacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gerenciamento de avaliações.
 * Usado para testes locais antes do deploy para Lambda.
 */
@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    /**
     * Endpoint para receber novas avaliações.
     * POST /api/avaliacoes
     */
    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criarAvaliacao(@Valid @RequestBody AvaliacaoRequest request) {
        Avaliacao avaliacao = avaliacaoService.processarAvaliacao(request);

        AvaliacaoResponse response = new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getDescricao(),
                avaliacao.getNota(),
                avaliacao.getDataEnvio().toString(),
                avaliacao.getUrgencia().name(),
                "Avaliação recebida com sucesso!"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para verificar saúde da aplicação.
     * GET /api/avaliacoes/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Sistema de Feedbacks operacional!");
    }
}

