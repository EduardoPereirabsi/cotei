package br.cotei.msalerta.repository;

import br.cotei.msalerta.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByEmailOrderByCriadaEmDesc(String email);

    List<Notificacao> findAllByOrderByCriadaEmDesc();
}
