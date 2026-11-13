package br.com.thermocity.baseterritorialapi.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Rastreio de todas as requisições HTTP.
 * <ul>
 *   <li>Cada requisição ganha um ID: o do cabeçalho {@code X-Request-Id}, se o cliente mandar, ou um novo.
 *   O ID vai no MDC ({@code requestId}), aparece em todas as linhas de log daquela requisição e volta no
 *   cabeçalho da resposta, para o front e as outras APIs poderem citar o mesmo ID.</li>
 *   <li>Registra a entrada (método, caminho, parâmetros e IP) e a saída (status e tempo em ms).
 *   Respostas 5xx saem como ERROR, 4xx como WARN e o resto como INFO.</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RastreioRequisicaoFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Request-Id";
    public static final String MDC_CHAVE = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RastreioRequisicaoFilter.class);
    /** Só aceita IDs simples vindos de fora, para ninguém injetar quebras de linha no log. */
    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String id = req.getHeader(CABECALHO);
        if (id == null || !ID_VALIDO.matcher(id).matches()) {
            id = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(MDC_CHAVE, id);
        resp.setHeader(CABECALHO, id);

        String rota = req.getMethod() + " " + req.getRequestURI()
                + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
        long inicio = System.nanoTime();
        log.info("-> {} (ip={})", rota, req.getRemoteAddr());
        try {
            chain.doFilter(req, resp);
        } catch (IOException | ServletException | RuntimeException e) {
            log.error("xx {} falhou após {} ms: {}", rota, ms(inicio), e.toString(), e);
            throw e;
        } finally {
            int status = resp.getStatus();
            long duracao = ms(inicio);
            if (status >= 500) {
                log.error("<- {} {} em {} ms", status, rota, duracao);
            } else if (status >= 400) {
                log.warn("<- {} {} em {} ms", status, rota, duracao);
            } else {
                log.info("<- {} {} em {} ms", status, rota, duracao);
            }
            MDC.remove(MDC_CHAVE);
        }
    }

    /** Não polui o log com arquivos estáticos e favicon. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return uri.equals("/favicon.ico") || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/img/") || uri.startsWith("/assets/");
    }

    private static long ms(long inicioNano) {
        return (System.nanoTime() - inicioNano) / 1_000_000;
    }
}
