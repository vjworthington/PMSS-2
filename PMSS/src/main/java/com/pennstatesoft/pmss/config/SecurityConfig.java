package com.pennstatesoft.pmss.config;

import com.pennstatesoft.pmss.security.LoginFailureHandler;
import com.pennstatesoft.pmss.security.LoginSuccessHandler;
import com.pennstatesoft.pmss.security.PmssAccessDeniedHandler;
import com.pennstatesoft.pmss.security.SecurityLogger;
import com.pennstatesoft.pmss.service.UserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final UserService userService;
    private final SecurityLogger securityLogger;

    public SecurityConfig(UserService userService, SecurityLogger securityLogger) {
        this.userService = userService;
        this.securityLogger = securityLogger;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/images/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()

                        .requestMatchers("/admin/**")
                        .hasRole("ADMINISTRATOR")

                        .requestMatchers("/client/**")
                        .hasRole("CLIENT")

                        .requestMatchers("/profile/**")
                        .authenticated()

                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(new LoginSuccessHandler(securityLogger))
                        .failureHandler(new LoginFailureHandler(securityLogger))
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {

                                    if (authentication != null) {
                                        securityLogger.logout(authentication.getName());
                                    }

                                    response.sendRedirect("/login");
                                }
                        )
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(new PmssAccessDeniedHandler(securityLogger))
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sessionFixation -> sessionFixation
                                .changeSessionId()
                        )
                )
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();

        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}