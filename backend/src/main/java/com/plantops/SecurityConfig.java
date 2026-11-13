package com.plantops;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Configuration @EnableMethodSecurity
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins){
        CorsConfiguration c=new CorsConfiguration(); c.setAllowedOrigins(Arrays.asList(origins.split(","))); c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); c.setAllowedHeaders(List.of("Authorization","Content-Type"));
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s;
    }
    @Bean JwtTokens jwtTokens(@Value("${jwt.secret}") String secret,@Value("${jwt.expiration-ms}") long expiry){return new JwtTokens(secret,expiry);}
    @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtFilter jwtFilter) throws Exception {
        return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a->a.requestMatchers("/api/auth/login","/api/health","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html","/actuator/health").permitAll().requestMatchers(HttpMethod.OPTIONS,"/**").permitAll().anyRequest().authenticated())
          .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
class JwtTokens {
    private final Key key; private final long expiry;
    JwtTokens(String secret,long expiry){this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.expiry=expiry;}
    String issue(Domain.AppUser user){Date now=new Date();return Jwts.builder().subject(user.getEmail()).claim("role",user.getRole().name()).issuedAt(now).expiration(new Date(now.getTime()+expiry)).signWith(key).compact();}
    Claims parse(String token){return Jwts.parser().verifyWith((javax.crypto.SecretKey)key).build().parseSignedClaims(token).getPayload();}
}
@Component
class JwtFilter extends OncePerRequestFilter {
    private final JwtTokens tokens;
    JwtFilter(JwtTokens tokens){this.tokens=tokens;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,java.io.IOException{
        String header=request.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")) try {Claims c=tokens.parse(header.substring(7));String email=c.getSubject(),role=c.get("role",String.class);var auth=new UsernamePasswordAuthenticationToken(email,null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));SecurityContextHolder.getContext().setAuthentication(auth);}catch(JwtException|IllegalArgumentException ignored){SecurityContextHolder.clearContext();}
        chain.doFilter(request,response);
    }
}
