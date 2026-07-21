package com.emall.backend.config;

import com.emall.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/user/login",
                                "/api/user/adminLogin",
                                "/api/user/register",
                                "/api/user/sendCode",
                                "/api/user/resetPassword",
                                "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/product/list",
                                "/api/product/detail/**",
                                "/api/product/hotSearches",
                                "/api/product/skus/**",
                                "/api/category/list",
                                "/api/ad/active",
                                "/api/comment/list/*",
                                "/api/coupon/list",
                                "/api/interaction/notice/active").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/product/searchRecord").permitAll()
                        .requestMatchers(
                                "/api/dashboard/**",
                                "/api/product/add",
                                "/api/product/update",
                                "/api/product/delete/**",
                                "/api/product/export",
                                "/api/product/import",
                                "/api/product/sync-inventory",
                                "/api/sku/**",
                                "/api/category/add",
                                "/api/category/update",
                                "/api/category/delete/**",
                                "/api/ad/list",
                                "/api/ad/add",
                                "/api/ad/update",
                                "/api/ad/delete/**",
                                "/api/user/list",
                                "/api/user/add",
                                "/api/order/list",
                                "/api/order/export",
                                "/api/comment/list",
                                "/api/interaction/feedback/list",
                                "/api/interaction/feedback/reply",
                                "/api/interaction/chat/user-list",
                                "/api/interaction/notice/list",
                                "/api/interaction/notice/add",
                                "/api/interaction/notice/update",
                                "/api/interaction/notice/delete/**",
                                "/api/file/uploadCommon").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/user/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录或重新登录"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "没有权限执行此操作")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
