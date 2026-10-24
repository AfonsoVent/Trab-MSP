package pt.unl.fct.msp.elderlyhealth.service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.exception.EstadoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoExpiradoException;
import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.CanalRegisto;
import pt.unl.fct.msp.elderlyhealth.model.ContactoEmergencia;
import pt.unl.fct.msp.elderlyhealth.model.DadosIdoso;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.EstadoRegisto;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.PedidoRegisto;
import pt.unl.fct.msp.elderlyhealth.model.Preferencias;
import pt.unl.fct.msp.elderlyhealth.repository.IdosoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.PedidoRegistoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.UtilizadorRepository;

/**
 * Subprocesso BPMN "Registar conta do idoso":
 * pedir dados -> validar (NIF) -> [paralelo] pedir preferências + pedir contacto de emergência
 * -> validar contacto -> registar idoso. Timer de 20 minutos sobre todo o subprocesso.
 */
@Service
public class RegistoContaService {

    private final PedidoRegistoRepository pedidos;
    private final IdosoRepository idosos;
    private final UtilizadorRepository utilizadores;
    private final DocumentoService documentos;
    private final NotificacaoService notificacoes;
    private final Clock clock;
    private final Duration timeout;

    public RegistoContaService(PedidoRegistoRepository pedidos, IdosoRepository idosos,
                               UtilizadorRepository utilizadores, DocumentoService documentos,
                               NotificacaoService notificacoes, Clock clock,
                               @Value("${elderlyhealth.registo.timeout:PT20M}") Duration timeout) {
        this.pedidos = pedidos;
        this.idosos = idosos;
        this.utilizadores = utilizadores;
        this.documentos = documentos;
        this.notificacoes = notificacoes;
        this.clock = clock;
        this.timeout = timeout;
    }

    /** Início do subprocesso + send task "Requisitar informação do idoso". */
    public synchronized PedidoRegisto iniciar(CanalRegisto canal) {
        return iniciar(canal, null);
    }

    /** @param utilizadorId conta que está a fazer o registo (fica com acesso ao idoso no fim). */
    public synchronized PedidoRegisto iniciar(CanalRegisto canal, Long utilizadorId) {
        PedidoRegisto p = new PedidoRegisto();
        p.setUtilizadorId(utilizadorId);
        p.setCanal(canal == null ? CanalRegisto.RECEPCIONISTA : canal);
        p.setEstado(EstadoRegisto.AGUARDA_DADOS);
        p.setCriadoEm(clock.instant());
        p.setAtualizadoEm(p.getCriadoEm());
        pedidos.guardar(p);
        notificar(p, "Envie os dados do idoso: nome, NIF, data de nascimento e informação médica");
        return p;
    }

    /** Receber dados -> "Validar dados recebidos" -> gateway OK / Não OK. */
    public synchronized PedidoRegisto submeterDados(Long id, DadosIdoso dados) {
        PedidoRegisto p = obterAtivo(id, EstadoRegisto.AGUARDA_DADOS);
        String nif = ValidadorDados.normalizar(dados.nif());
        if (!ValidadorDados.nifValido(nif)) {
            return negar(p, "NIF inválido");
        }
        if (idosos.existeNif(nif)) {
            return negar(p, "Já existe uma conta registada com este NIF");
        }
        p.setDados(new DadosIdoso(dados.nome().trim(), nif, dados.dataNascimento(),
                dados.informacaoMedica(), dados.quarto()));
        p.setEstado(EstadoRegisto.AGUARDA_PREFERENCIAS_E_CONTACTO);
        tocar(p);
        // Gateway paralelo: os dois pedidos são enviados ao mesmo tempo
        notificar(p, "Envie as preferências do idoso (dieta, alergias, observações)");
        notificar(p, "Envie o contacto de emergência do idoso");
        return p;
    }

    /** Ramo paralelo 1: receber preferências. */
    public synchronized PedidoRegisto submeterPreferencias(Long id, Preferencias preferencias) {
        PedidoRegisto p = obterAtivo(id, EstadoRegisto.AGUARDA_PREFERENCIAS_E_CONTACTO);
        p.setPreferencias(new Preferencias(preferencias.dieta(),
                preferencias.alergias() == null ? List.of() : List.copyOf(preferencias.alergias()),
                preferencias.observacoes()));
        tocar(p);
        return concluirSePossivel(p);
    }

    /** Ramo paralelo 2: receber contacto de emergência -> validar -> gateway OK / Não OK. */
    public synchronized PedidoRegisto submeterContactoEmergencia(Long id, ContactoEmergencia contacto) {
        PedidoRegisto p = obterAtivo(id, EstadoRegisto.AGUARDA_PREFERENCIAS_E_CONTACTO);
        if (!ValidadorDados.telefoneValido(contacto.telefone())) {
            return negar(p, "Contacto de emergência inválido (telefone)");
        }
        p.setContactoEmergencia(new ContactoEmergencia(contacto.nome().trim(),
                ValidadorDados.normalizarTelefone(contacto.telefone()), contacto.parentesco()));
        tocar(p);
        return concluirSePossivel(p);
    }

    public synchronized PedidoRegisto obter(Long id) {
        PedidoRegisto p = procurar(id);
        verificarExpiracao(p);
        return p;
    }

    public synchronized List<PedidoRegisto> listar() {
        pedidos.todos().forEach(this::verificarExpiracao);
        return pedidos.todos();
    }

    /** Boundary timer de 20 minutos (chamado periodicamente pelo TimeoutScheduler). */
    public synchronized void expirarPendentes() {
        pedidos.todos().forEach(this::verificarExpiracao);
    }

    /** Gateway paralelo de junção: só regista quando os dois ramos chegaram. */
    private PedidoRegisto concluirSePossivel(PedidoRegisto p) {
        if (p.getPreferencias() == null || p.getContactoEmergencia() == null) {
            return p;
        }
        DadosIdoso d = p.getDados();
        Idoso idoso = new Idoso();
        idoso.setNome(d.nome());
        idoso.setNif(d.nif());
        idoso.setDataNascimento(d.dataNascimento());
        idoso.setInformacaoMedica(d.informacaoMedica());
        idoso.setQuarto(d.quarto());
        idoso.setPreferencias(p.getPreferencias());
        idoso.setContactoEmergencia(p.getContactoEmergencia());
        idoso.setRegistadoEm(clock.instant());
        idosos.guardar(idoso);
        documentos.criarPastas(idoso);
        if (p.getUtilizadorId() != null) {
            utilizadores.procurar(p.getUtilizadorId()).ifPresent(u -> {
                u.getIdosoIds().add(idoso.getId());
                utilizadores.guardar(u);
            });
        }

        p.setIdosoId(idoso.getId());
        p.setEstado(EstadoRegisto.REGISTADO);
        tocar(p);
        notificar(p, "Conta registada com sucesso (idoso #" + idoso.getId() + ")");
        return p;
    }

    private PedidoRegisto negar(PedidoRegisto p, String motivo) {
        p.setEstado(EstadoRegisto.NEGADO);
        p.setMotivo(motivo);
        tocar(p);
        notificar(p, "Conta negada: " + motivo);
        return p;
    }

    private void verificarExpiracao(PedidoRegisto p) {
        if (!p.getEstado().isFinal() && clock.instant().isAfter(p.getCriadoEm().plus(timeout))) {
            p.setEstado(EstadoRegisto.EXPIRADO);
            p.setMotivo("Tempo limite para responder excedido");
            tocar(p);
            notificar(p, "Tempo limite para responder excedido — o registo foi cancelado");
        }
    }

    private PedidoRegisto obterAtivo(Long id, EstadoRegisto esperado) {
        PedidoRegisto p = procurar(id);
        verificarExpiracao(p);
        if (p.getEstado() == EstadoRegisto.EXPIRADO) {
            throw new PedidoExpiradoException("O pedido de registo " + id + " expirou");
        }
        if (p.getEstado() != esperado) {
            throw new EstadoInvalidoException("O pedido de registo " + id + " está em " + p.getEstado()
                    + " (esperado " + esperado + ")");
        }
        return p;
    }

    private PedidoRegisto procurar(Long id) {
        return pedidos.procurar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido de registo " + id + " não existe"));
    }

    private void tocar(PedidoRegisto p) {
        p.setAtualizadoEm(clock.instant());
        pedidos.guardar(p);
    }

    private void notificar(PedidoRegisto p, String mensagem) {
        notificacoes.enviar(Destinatario.UTILIZADOR, p.getIdosoId(), "REGISTO#" + p.getId(), mensagem);
    }
}
