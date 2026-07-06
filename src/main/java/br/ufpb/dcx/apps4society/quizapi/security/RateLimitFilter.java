package br.ufpb.dcx.apps4society.quizapi.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Rate limit só nos endpoints públicos (permitAll no SecurityConfig) — quem
// já está autenticado passa direto. Limites pensados pra não travar o uso
// normal do front (poucas chamadas por sessão) mas barrar scripts batendo
// no endpoint em loop. Contadores em janela fixa via Caffeine (expiração
// automática cuida da limpeza, sem precisar de scheduler próprio).
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Environment environment;

    @Autowired
    public RateLimitFilter(Environment environment) {
        this.environment = environment;
    }

    private record Rule(String method, String pattern, int limit, Duration window) {
    }

    private final List<Rule> rules = List.of(
            // Registro: baixo volume esperado, alvo comum de bots criando contas em massa.
            new Rule("POST", "/v1/user/register", 5, Duration.ofMinutes(10)),
            // Login: usuário real erra a senha algumas vezes, não centenas — protege contra força bruta.
            new Rule("POST", "/v1/user/login", 10, Duration.ofMinutes(1)),
            // Listagem/leitura de temas e questões do quiz: navegação normal gera várias chamadas rápidas.
            new Rule("GET", "/v1/theme/**", 60, Duration.ofMinutes(1)),
            new Rule("GET", "/v1/question/quiz/**", 60, Duration.ofMinutes(1)),
            // Estatística enviada ao fim de cada quiz single-player (sem login).
            new Rule("POST", "/v1/statistic", 30, Duration.ofMinutes(1)),
            // Torneio: criação é rara; consulta de estado é feita via polling
            // do front (poucos segundos de intervalo) — limite generoso.
            new Rule("POST", "/v1/tournament", 5, Duration.ofMinutes(10)),
            new Rule("GET", "/v1/tournament/*", 60, Duration.ofMinutes(1)),
            // Perfil público: visitável sem login, ex. clique em nome de outro jogador.
            new Rule("GET", "/v1/user/*/public-profile", 30, Duration.ofMinutes(1))
    );

    private final Map<Duration, Cache<String, AtomicInteger>> cachesByWindow = new ConcurrentHashMap<>();

    private Cache<String, AtomicInteger> cacheFor(Duration window) {
        return cachesByWindow.computeIfAbsent(window,
                w -> Caffeine.newBuilder().expireAfterWrite(w).maximumSize(50_000).build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Suíte de integração roda dezenas de chamadas ao mesmo endpoint na
        // mesma JVM/IP — estouraria os limites pensados pro tráfego real.
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("test"))) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule rule = findRule(request);

        if (rule != null) {
            String key = rule.method() + ":" + rule.pattern() + ":" + clientIp(request);
            AtomicInteger counter = cacheFor(rule.window()).get(key, k -> new AtomicInteger(0));

            if (counter.incrementAndGet() > rule.limit()) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Muitas requisições. Tente novamente em instantes.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Rule findRule(HttpServletRequest request) {
        for (Rule rule : rules) {
            if (rule.method().equalsIgnoreCase(request.getMethod())
                    && pathMatcher.match(rule.pattern(), request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
