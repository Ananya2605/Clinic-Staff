package com.clinic.staff;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/error", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/style.css", "/app.js", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/audit", "/api/dashboard").hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.POST, "/api/leaves/*/approve", "/api/leaves/*/reject").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "HR")
                .anyRequest().denyAll())
            // Plain 401 without a WWW-Authenticate header, so browsers do not open their own login popup.
            .httpBasic(h -> h.authenticationEntryPoint((req, res, ex) -> res.sendError(401)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return name -> users.findByUsername(name)
                .map(u -> User.withUsername(u.username).password(u.passwordHash).roles(u.role).build())
                .orElseThrow(() -> new UsernameNotFoundException(name));
    }
}
