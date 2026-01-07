package com.desk.config;

import com.desk.security.CustomUserDetailsService;
import com.desk.security.filter.FaceAuthenticationFilter;
import com.desk.security.filter.FaceAuthenticationProvider;
import com.desk.security.filter.JWTCheckFilter;
import com.desk.security.handler.APILoginFailHandler;
import com.desk.security.handler.APILoginSuccessHandler;
import com.desk.security.handler.CustomAccessDeniedHandler;
import com.desk.service.FaceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@Log4j2
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class CustomSecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final FaceService faceService; // 주입
    private final FaceAuthenticationProvider faceAuthenticationProvider; // 주입

    @Bean // 비밀번호 암호화(BCrypt 해시 방식으로 암호화), (@Bean)스프링 전역에서 사용가능
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        log.info("---------------------security config---------------------------");

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                        })
                );
        // 접근 권한 예외 처리
        http.exceptionHandling(config -> {config.accessDeniedHandler(new CustomAccessDeniedHandler());
        });

        http.authenticationProvider(authenticationProvider(passwordEncoder()));

        // Spring Security의 CORS 필터를 활성화하고 corsConfigurationSource() 설정에 따라 프론트엔드의 API 요청을 허용한다.
        http.cors(httpSecurityCorsConfigurer -> {
            httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());
        });

        // 로그인 설정 (JWT 발급 지점)
        http.formLogin(config ->{
            config.loginPage("/api/member/login");
            config.successHandler(new APILoginSuccessHandler());
            config.failureHandler(new APILoginFailHandler());
        });

        // JWT 검증 필터 등록 (요청마다 실행)
        // UsernamePasswordAuthenticationFilter 실행 시, 내부적으로 loadUserByUsername()이 호출.(로그인 시) - CustomUserDetailsService
        http.addFilterBefore(new JWTCheckFilter(), UsernamePasswordAuthenticationFilter.class); //JWT 체크

        // 매니저 획득
        AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(faceAuthenticationProvider) // 얼굴 프로바이더 등록
                .authenticationProvider(authenticationProvider(passwordEncoder())) // 기존 ID/PW 프로바이더
                .build();

        http.authenticationManager(authenticationManager);

        // 얼굴 인식 필터 설정
        FaceAuthenticationFilter faceFilter = new FaceAuthenticationFilter("/api/member/login/face", faceService);
        faceFilter.setAuthenticationManager(authenticationManager);
        faceFilter.setAuthenticationSuccessHandler(new APILoginSuccessHandler()); // 성공 시 JWT 발급
        faceFilter.setAuthenticationFailureHandler(new APILoginFailHandler());

        // 필터 순서 등록
        http.addFilterBefore(faceFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // 모든 도메인 허용
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // REST API 전용
        configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "PATCH", "DELETE"));
        // JWT 전달을 위한 Authorization 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // 쿠키/인증 정보 허용
        configuration.setAllowCredentials(true);

        // URL별로 CORS 정책 적용 가능 (모든 URL에 위 정책 적용)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

}