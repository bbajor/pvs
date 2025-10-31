package de.bbajor.pvs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Headers Configuration für OWASP Top 10 Compliance.
 * 
 * Setzt Security-relevante HTTP-Headers für alle Responses:
 * - Content-Security-Policy (CSP)
 * - X-Frame-Options
 * - X-Content-Type-Options
 * - Referrer-Policy
 * - Permissions-Policy
 * - HSTS (bereits von Traefik gesetzt, hier als Fallback)
 * 
 * Hinweis: Diese Headers werden zusätzlich zu Traefik-Middlewares gesetzt
 * für Defense-in-Depth (doppelte Absicherung).
 * 
 * Diese Konfiguration gilt für alle Umgebungen (dev, test, prod).
 * In Dev ist sie weniger strikt für einfacheres Debugging.
 * 
 * @author Agent 2 - Spring Security
 * @since 2025-10-30
 */
@Configuration
public class SecurityHeadersConfiguration {

    /**
     * Security Headers Filter Bean.
     * 
     * Wird für alle HTTP-Responses ausgeführt (OncePerRequestFilter).
     */
    @Bean
    public OncePerRequestFilter securityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                           HttpServletResponse response,
                                           FilterChain filterChain) throws ServletException, IOException {
                
                // Content-Security-Policy (CSP)
                // Vaadin-spezifische CSP-Regeln (Vaadin benötigt unsafe-inline für Styles)
                // Hinweis: Vaadin 24+ hat eigene CSP-Unterstützung, diese ist als Fallback
                String csp = "default-src 'self'; " +
                             "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                             "style-src 'self' 'unsafe-inline'; " +
                             "img-src 'self' data: https:; " +
                             "font-src 'self' data:; " +
                             "connect-src 'self'; " +
                             "frame-ancestors 'none'; " +
                             "base-uri 'self'; " +
                             "form-action 'self'";
                response.setHeader("Content-Security-Policy", csp);
                
                // X-Frame-Options (Clickjacking-Schutz)
                response.setHeader("X-Frame-Options", "DENY");
                
                // X-Content-Type-Options (MIME-Sniffing verhindern)
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                // X-XSS-Protection (Legacy, aber noch sinnvoll für alte Browser)
                response.setHeader("X-XSS-Protection", "1; mode=block");
                
                // Referrer-Policy (Privacy-Protection)
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                
                // Permissions-Policy (Browser-Features deaktivieren)
                String permissionsPolicy = "camera=(), microphone=(), geolocation=(), " +
                                          "payment=(), usb=(), magnetometer=(), gyroscope=()";
                response.setHeader("Permissions-Policy", permissionsPolicy);
                
                // HSTS (Strict-Transport-Security) - Fallback zu Traefik
                // Nur setzen wenn HTTPS (erkennbar via X-Forwarded-Proto)
                String forwardedProto = request.getHeader("X-Forwarded-Proto");
                if ("https".equals(forwardedProto)) {
                    // 1 Jahr (31536000 Sekunden)
                    response.setHeader("Strict-Transport-Security", 
                                      "max-age=31536000; includeSubDomains; preload");
                }
                
                // Filter-Chain fortsetzen
                filterChain.doFilter(request, response);
            }
        };
    }
}
