package br.ufpb.dcx.apps4society.quizapi.config;

import br.ufpb.dcx.apps4society.quizapi.security.RateLimitFilter;
import br.ufpb.dcx.apps4society.quizapi.security.SecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private SecurityFilter securityFilter;
    private RateLimitFilter rateLimitFilter;
    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigin;

    @Autowired
    public SecurityConfig(SecurityFilter securityFilter, RateLimitFilter rateLimitFilter) {
        this.securityFilter = securityFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request -> {
                    request.requestMatchers("/", "/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**").permitAll();
                    request.requestMatchers("/v1/game/**").permitAll();
                    request.requestMatchers("/v1/tournament/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/v1/theme/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/v1/question/quiz/**").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/v1/user/*/public-profile").permitAll();
                    request.requestMatchers(HttpMethod.POST, "/v1/user/register", "/v1/user/login").permitAll();
                    request.requestMatchers(HttpMethod.POST, "/v1/statistic").permitAll();
                    request.requestMatchers(HttpMethod.GET, "/v1/theme/creator").authenticated()
                            .anyRequest().authenticated();
                })
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, SecurityFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration auth) throws Exception {
        return auth.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
