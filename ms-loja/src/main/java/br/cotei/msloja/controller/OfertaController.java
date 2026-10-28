package br.cotei.msloja.controller;

import br.cotei.msloja.client.ProdutoIndisponivelException;
import br.cotei.msloja.domain.Oferta;
import br.cotei.msloja.service.OfertaService;
import br.cotei.msloja.service.ProdutoInexistenteException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ofertas")
public class OfertaController {

    private final OfertaService service;

    public OfertaController(OfertaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Oferta> listar(@RequestParam(required = false) Long produtoId) {
        return service.listar(produtoId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Oferta> buscar(@PathVariable Long id) {
        return service.buscar(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/melhor")
    public ResponseEntity<Oferta> melhor(@RequestParam Long produtoId) {
        return service.melhorOferta(produtoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Oferta> registrar(@Valid @RequestBody OfertaRequest req) {
        Oferta salva = service.registrar(req.produtoId(), req.loja(), req.preco(), req.frete(),
                req.disponivel(), req.url(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        return service.deletar(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
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
