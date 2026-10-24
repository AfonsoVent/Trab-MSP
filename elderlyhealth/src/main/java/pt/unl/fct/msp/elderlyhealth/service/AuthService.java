package pt.unl.fct.msp.elderlyhealth.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.dto.SessaoIniciada;
import pt.unl.fct.msp.elderlyhealth.dto.UtilizadorResposta;
import pt.unl.fct.msp.elderlyhealth.exception.AcessoNegadoException;
import pt.unl.fct.msp.elderlyhealth.exception.NaoAutenticadoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.model.Papel;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.repository.UtilizadorRepository;

/**
 * Processo principal (main.bpmn): "O user já tem conta?" -> Não: criar conta / inscrever idoso;
 * Sim: "Realizar autenticação" (o CPS valida as credenciais) -> menu principal.
 * Sessões com token aleatório, expiração e bloqueio após 5 tentativas falhadas.
 */
@Service
public class AuthService {

    private static final int MAX_FALHAS = 5;
    private static final Duration BLOQUEIO = Duration.ofMinutes(5);

    private final UtilizadorRepository utilizadores;
    private final Clock clock;
    private final Duration duracaoSessao;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final Map<String, Tentativas> falhas = new ConcurrentHashMap<>();

    public AuthService(UtilizadorRepository utilizadores, Clock clock,
                       @Value("${elderlyhealth.sessao.duracao:PT8H}") Duration duracaoSessao) {
        this.utilizadores = utilizadores;
        this.clock = clock;
        this.duracaoSessao = duracaoSessao;
    }

    /** Criação de conta pública (familiar ou o próprio idoso). Contas da equipa não se criam aqui. */
    public Utilizador registar(String nome, String email, String password, Papel papel) {
        Papel p = papel == null ? Papel.FAMILIAR : papel;
        if (p.isEquipa()) {
            throw new AcessoNegadoException("Contas da equipa só podem ser criadas por um administrador");
        }
        return criarConta(nome, email, password, p);
    }

    public synchronized Utilizador criarConta(String nome, String email, String password, Papel papel) {
        String e = normalizarEmail(email);
        if (nome == null || nome.isBlank()) {
            throw new PedidoInvalidoException("Indique o seu nome");
        }
        if (!e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new PedidoInvalidoException("E-mail inválido");
        }
        validarPassword(password);
        if (utilizadores.procurarPorEmail(e).isPresent()) {
            throw new PedidoInvalidoException("Já existe uma conta com este e-mail");
        }
        Utilizador u = new Utilizador();
        u.setNome(nome.trim());
        u.setEmail(e);
        u.setPasswordHash(PasswordHasher.hash(password));
        u.setPapel(papel);
        u.setCriadoEm(clock.instant());
        return utilizadores.guardar(u);
    }

    public SessaoIniciada login(String email, String password) {
        String e = normalizarEmail(email);
        Instant agora = clock.instant();
        Tentativas t = falhas.get(e);
        if (t != null && t.bloqueadoAte != null && agora.isBefore(t.bloqueadoAte)) {
            throw new NaoAutenticadoException("Demasiadas tentativas falhadas. Tente novamente dentro de alguns minutos");
        }
        Utilizador u = utilizadores.procurarPorEmail(e).orElse(null);
        if (u == null || !PasswordHasher.verificar(password, u.getPasswordHash())) {
            registarFalha(e, agora);
            throw new NaoAutenticadoException("E-mail ou palavra-passe incorretos");
        }
        falhas.remove(e);
        String token = gerarToken();
        Instant expira = agora.plus(duracaoSessao);
        sessoes.put(token, new Sessao(u.getId(), expira));
        return new SessaoIniciada(token, expira, UtilizadorResposta.de(u));
    }

    public Utilizador autenticar(String token) {
        if (token == null || token.isBlank()) {
            throw new NaoAutenticadoException("Inicie sessão para continuar");
        }
        Sessao s = sessoes.get(token);
        if (s == null || clock.instant().isAfter(s.expiraEm)) {
            sessoes.remove(token);
            throw new NaoAutenticadoException("A sessão expirou. Inicie sessão novamente");
        }
        return utilizadores.procurar(s.utilizadorId)
                .orElseThrow(() -> new NaoAutenticadoException("Conta inexistente"));
    }

    public void logout(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }

    public static void exigirAcessoIdoso(Utilizador u, Long idosoId) {
        if (u == null) {
            throw new NaoAutenticadoException("Inicie sessão para continuar");
        }
        if (!u.getPapel().isEquipa() && !u.getIdosoIds().contains(idosoId)) {
            throw new AcessoNegadoException("Não tem acesso aos dados deste idoso");
        }
    }

    public static void exigirEquipa(Utilizador u) {
        if (u == null || !u.getPapel().isEquipa()) {
            throw new AcessoNegadoException("Apenas a equipa de enfermagem ou a administração pode fazer isto");
        }
    }

    public static boolean podeVer(Utilizador u, Long idosoId) {
        return u != null && (u.getPapel().isEquipa() || u.getIdosoIds().contains(idosoId));
    }

    static void validarPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new PedidoInvalidoException("A palavra-passe tem de ter pelo menos 8 caracteres");
        }
        if (!password.matches(".*[0-9].*") || !password.matches(".*[A-Za-z].*")) {
            throw new PedidoInvalidoException("A palavra-passe tem de ter letras e números");
        }
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private synchronized void registarFalha(String email, Instant agora) {
        Tentativas t = falhas.computeIfAbsent(email, k -> new Tentativas());
        t.falhas++;
        if (t.falhas >= MAX_FALHAS) {
            t.bloqueadoAte = agora.plus(BLOQUEIO);
            t.falhas = 0;
        }
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Sessao(Long utilizadorId, Instant expiraEm) {
    }

    private static final class Tentativas {
        private int falhas;
        private Instant bloqueadoAte;
    }
}
