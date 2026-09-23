package com.ecommerce.auth.security;

import com.ecommerce.auth.entity.Session;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.SessionRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   SessionRepository sessionRepository,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (jwtService.isValid(token)) {
            Long userId = jwtService.extractUserId(token);
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Session> sessionOpt = sessionRepository.findByToken(token);

            if (userOpt.isPresent()
                    && sessionOpt.isPresent()
                    && sessionOpt.get().getExpiryTime().isAfter(LocalDateTime.now())) {

                User user = userOpt.get();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Session expired or invalid. Please log in again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}