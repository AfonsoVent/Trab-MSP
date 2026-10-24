package pt.unl.fct.msp.elderlyhealth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;

/**
 * Protege /api/**. Exceções:
 *  - /api/auth/login e /api/auth/registar (públicos);
 *  - sensores e tensiómetros podem enviar dados com o cabeçalho X-Device-Key.
 * Os restantes pedidos precisam de "Authorization: Bearer {token}".
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_UTILIZADOR = "utilizador";

    private final AuthService auth;
    private final String chaveDispositivos;

    public AuthInterceptor(AuthService auth,
                           @Value("${elderlyhealth.dispositivos.chave:dispositivo-demo-123}") String chaveDispositivos) {
        this.auth = auth;
        this.chaveDispositivos = chaveDispositivos;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        String metodo = request.getMethod();
        if ("OPTIONS".equals(metodo)) {
            return true;
        }
        if (caminho.equals("/api/auth/login") || caminho.equals("/api/auth/registar")) {
            return true;
        }
        boolean endpointDispositivo = "POST".equals(metodo)
                && (caminho.equals("/api/quedas") || caminho.equals("/api/tensao-arterial/medicoes"));
        String chave = request.getHeader("X-Device-Key");
        if (endpointDispositivo && chave != null && chave.equals(chaveDispositivos)) {
            return true;
        }
        request.setAttribute(ATRIBUTO_UTILIZADOR, auth.autenticar(token(request)));
        return true;
    }

    public static String token(HttpServletRequest request) {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            return cabecalho.substring(7).trim();
        }
        return null;
    }
}
