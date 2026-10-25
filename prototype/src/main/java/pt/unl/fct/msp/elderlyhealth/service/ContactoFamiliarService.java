package pt.unl.fct.msp.elderlyhealth.service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.exception.EstadoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoExpiradoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.ContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.DadosContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.EstadoPedidoContacto;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.PedidoContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.repository.ContactoFamiliarRepository;
import pt.unl.fct.msp.elderlyhealth.repository.PedidoContactoRepository;

/**
 * Subprocesso BPMN "Adicionar contacto familiar":
 * receber contacto -> validar contacto + pedir confirmação ao utilizador
 * -> event-based gateway (confirmação | 1 minuto) -> guardar contacto -> notificar.
 */
@Service
public class ContactoFamiliarService {

    private final PedidoContactoRepository pedidos;
    private final ContactoFamiliarRepository contactos;
    private final IdosoService idosos;
    private final NotificacaoService notificacoes;
    private final Clock clock;
    private final Duration timeoutConfirmacao;

    public ContactoFamiliarService(PedidoContactoRepository pedidos, ContactoFamiliarRepository contactos,
                                   IdosoService idosos, NotificacaoService notificacoes, Clock clock,
                                   @Value("${elderlyhealth.contacto.timeout-confirmacao:PT1M}") Duration timeoutConfirmacao) {
        this.pedidos = pedidos;
        this.contactos = contactos;
        this.idosos = idosos;
        this.notificacoes = notificacoes;
        this.clock = clock;
        this.timeoutConfirmacao = timeoutConfirmacao;
    }

    /** Receber contacto -> "Validar contacto familiar" -> gateway "É válido?" -> pedir confirmação. */
    public synchronized PedidoContactoFamiliar pedirAdicao(Long idosoId, DadosContactoFamiliar dados) {
        Idoso idoso = idosos.obter(idosoId);
        String telefone = ValidadorDados.normalizarTelefone(dados.telefone());

        String motivo = null;
        if (!ValidadorDados.telefoneValido(dados.telefone())) {
            motivo = "telefone inválido";
        } else if (!contactos.filtrar(c -> c.getIdosoId().equals(idosoId) && c.getTelefone().equals(telefone)).isEmpty()) {
            motivo = "este telefone já está associado ao idoso";
        }
        if (motivo != null) {
            notificacoes.enviar(Destinatario.UTILIZADOR, idosoId, "CONTACTO", "Contacto inválido: " + motivo);
            throw new PedidoInvalidoException("Contacto inválido: " + motivo);
        }

        PedidoContactoFamiliar p = new PedidoContactoFamiliar();
        p.setIdosoId(idosoId);
        p.setDados(new DadosContactoFamiliar(dados.nome().trim(), telefone, dados.email(), dados.parentesco()));
        p.setEstado(EstadoPedidoContacto.AGUARDA_CONFIRMACAO);
        p.setCriadoEm(clock.instant());
        p.setAtualizadoEm(p.getCriadoEm());
        pedidos.guardar(p);

        notificacoes.enviar(Destinatario.UTILIZADOR, idosoId, ref(p),
                "Confirme a adição de " + p.getDados().nome() + " (" + p.getDados().parentesco()
                        + ") como contacto familiar de " + idoso.getNome() + " — tem "
                        + timeoutConfirmacao.toSeconds() + " segundos");
        return p;
    }

    /** Event-based gateway: chegou a confirmação antes do timer. */
    public synchronized PedidoContactoFamiliar confirmar(Long idosoId, Long pedidoId, boolean confirmado) {
        PedidoContactoFamiliar p = procurar(idosoId, pedidoId);
        verificarExpiracao(p);
        if (p.getEstado() == EstadoPedidoContacto.EXPIRADO) {
            throw new PedidoExpiradoException("O pedido " + pedidoId + " expirou por falta de confirmação");
        }
        if (p.getEstado() != EstadoPedidoContacto.AGUARDA_CONFIRMACAO) {
            throw new EstadoInvalidoException("O pedido " + pedidoId + " já está em " + p.getEstado());
        }

        if (!confirmado) {
            p.setEstado(EstadoPedidoContacto.REJEITADO);
            tocar(p);
            notificacoes.enviar(Destinatario.UTILIZADOR, idosoId, ref(p), "Adição do contacto cancelada pelo utilizador");
            return p;
        }

        // Parallel gateway de junção: contacto válido + confirmado -> "Guardar contacto familiar"
        DadosContactoFamiliar d = p.getDados();
        ContactoFamiliar c = new ContactoFamiliar();
        c.setIdosoId(idosoId);
        c.setNome(d.nome());
        c.setTelefone(d.telefone());
        c.setEmail(d.email());
        c.setParentesco(d.parentesco());
        c.setAdicionadoEm(clock.instant());
        contactos.guardar(c);

        p.setContactoId(c.getId());
        p.setEstado(EstadoPedidoContacto.CONFIRMADO);
        tocar(p);
        notificacoes.enviar(Destinatario.UTILIZADOR, idosoId, ref(p), "Contacto familiar " + d.nome() + " adicionado");
        notificacoes.enviar(Destinatario.FAMILIA, idosoId, ref(p),
                "Foi adicionado(a) como contacto familiar de " + idosos.obter(idosoId).getNome());
        return p;
    }

    public synchronized PedidoContactoFamiliar obterPedido(Long idosoId, Long pedidoId) {
        PedidoContactoFamiliar p = procurar(idosoId, pedidoId);
        verificarExpiracao(p);
        return p;
    }

    public List<ContactoFamiliar> listar(Long idosoId) {
        idosos.obter(idosoId);
        return contactos.filtrar(c -> c.getIdosoId().equals(idosoId));
    }

    /** Timer de 1 minuto do event-based gateway. */
    public synchronized void expirarPendentes() {
        pedidos.todos().forEach(this::verificarExpiracao);
    }

    private void verificarExpiracao(PedidoContactoFamiliar p) {
        if (p.getEstado() == EstadoPedidoContacto.AGUARDA_CONFIRMACAO
                && clock.instant().isAfter(p.getCriadoEm().plus(timeoutConfirmacao))) {
            p.setEstado(EstadoPedidoContacto.EXPIRADO);
            tocar(p);
            notificacoes.enviar(Destinatario.UTILIZADOR, p.getIdosoId(), ref(p),
                    "O contacto não foi adicionado por falta de confirmação");
        }
    }

    private PedidoContactoFamiliar procurar(Long idosoId, Long pedidoId) {
        return pedidos.procurar(pedidoId)
                .filter(p -> p.getIdosoId().equals(idosoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido " + pedidoId + " não existe para o idoso " + idosoId));
    }

    private void tocar(PedidoContactoFamiliar p) {
        p.setAtualizadoEm(clock.instant());
        pedidos.guardar(p);
    }

    private static String ref(PedidoContactoFamiliar p) {
        return "CONTACTO#" + p.getId();
    }
}
