package pt.unl.fct.msp.elderlyhealth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.dto.CriarContaRequest;
import pt.unl.fct.msp.elderlyhealth.dto.LoginRequest;
import pt.unl.fct.msp.elderlyhealth.dto.SessaoIniciada;
import pt.unl.fct.msp.elderlyhealth.dto.UtilizadorResposta;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;

/** main.bpmn — "O user já tem conta?" / "Realizar autenticação" / "Sessão encerrada". */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Criar conta (familiar ou o próprio idoso). Depois segue-se a inscrição do idoso (/api/registos). */
    @PostMapping("/registar")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoIniciada registar(@Valid @RequestBody CriarContaRequest pedido) {
        auth.registar(pedido.nome(), pedido.email(), pedido.password(), pedido.papel());
        return auth.login(pedido.email(), pedido.password());
    }

    @PostMapping("/login")
    public SessaoIniciada login(@Valid @RequestBody LoginRequest pedido) {
        return auth.login(pedido.email(), pedido.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        auth.logout(AuthInterceptor.token(request));
    }

    @GetMapping("/eu")
    public UtilizadorResposta eu(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u) {
        return UtilizadorResposta.de(u);
    }
}
