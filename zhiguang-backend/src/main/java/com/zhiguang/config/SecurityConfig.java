package com.zhiguang.config;

import com.zhiguang.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 生产环境通过环境变量 CORS_ALLOWED_ORIGINS 指定允许的域名
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"code\":403,\"message\":\"没有权限访问该资源\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/error", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/sse-ticket").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/hot").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/popular").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/favorites").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/columns/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/collections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/knowledge-graph/data").permitAll()
                        .requestMatchers("/api/tree-hole/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/articles/{id}/like").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/articles/{id}/favorite").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/*/follow").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/follow/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/follows/status/**").permitAll()
                        .requestMatchers("/api/follows/**").authenticated()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/ai/**").authenticated()
                        .requestMatchers("/api/agent/**").authenticated()
                        .requestMatchers("/api/growth/**").authenticated()
                        .requestMatchers("/api/messages/stream").authenticated()
                        .requestMatchers("/api/notifications/stream").authenticated()
                        // 私信全链路需登录（此前 GET 落在默认放行，仅靠 Controller 判空兜底，M-6）
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/upload/**").authenticated()
                        .requestMatchers("/api/avatar-shop/**").authenticated()
                        // 纵深防御：所有写方法（POST/PUT/PATCH/DELETE）默认需要认证，
                        // 匿名可达的写端点（/api/auth/**、/api/tree-hole/**）已在上方显式放行。
                        // 读接口保持默认放行，避免逐个枚举公开 GET 造成回归。
                        .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
