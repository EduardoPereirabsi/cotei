package br.cotei.msalerta.controller;

import br.cotei.msalerta.client.ProdutoIndisponivelException;
import br.cotei.msalerta.domain.Alerta;
import br.cotei.msalerta.domain.Notificacao;
import br.cotei.msalerta.service.AlertaService;
import br.cotei.msalerta.service.ProdutoInexistenteException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping("/alertas")
    public List<Alerta> listar(@RequestParam(required = false) String email,
                               @RequestParam(required = false) Long produtoId) {
        return service.listar(email, produtoId);
    }

    @GetMapping("/alertas/{id}")
    public ResponseEntity<Alerta> buscar(@PathVariable Long id) {
        return service.buscar(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/alertas")
    public ResponseEntity<Alerta> criar(@Valid @RequestBody AlertaRequest req) {
        Alerta criado = service.criar(req.produtoId(), req.email(), req.precoAlvo());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @DeleteMapping("/alertas/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return service.remover(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Notificacoes ja disparadas: a "caixa de entrada" do usuario. */
    @GetMapping("/notificacoes")
    public List<Notificacao> notificacoes(@RequestParam(required = false) String email) {
        return service.notificacoes(email);
    }

    @ExceptionHandler(ProdutoInexistenteException.class)
    public ProblemDetail produtoInexistente(ProdutoInexistenteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(ProdutoIndisponivelException.class)
    public ProblemDetail produtoIndisponivel(ProdutoIndisponivelException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }
}
