package com.pagoflex.auth.service;

import com.pagoflex.auth.entity.Rol;
import com.pagoflex.auth.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    private static final String SECRET = "PagoFlexSecretKeyParaJwtConMinimo256BitsDeLargo1234567890";

    @Test
    void generaTokenConClaimsCorrectos() {
        JwtService jwtService = new JwtService(SECRET, 60000);
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setCorreo("camila@correo.ejemplo");
        usuario.setRol(new Rol(1L, "USUARIO"));
        usuario.setEmpresaId(3L);

        String token = jwtService.generateToken(usuario);
        assertNotNull(token);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        assertEquals("camila@correo.ejemplo", claims.getSubject());
        assertEquals(7, claims.get("id", Integer.class));
        assertEquals("USUARIO", claims.get("rol", String.class));
        assertEquals(3, claims.get("empresaId", Integer.class));
    }

    @Test
    void tokenSinEmpresaTieneClaimNulo() {
        JwtService jwtService = new JwtService(SECRET, 60000);
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setCorreo("agente@pagoflex.ejemplo");
        usuario.setRol(new Rol(2L, "AGENTE"));

        String token = jwtService.generateToken(usuario);
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        assertEquals("AGENTE", claims.get("rol", String.class));
    }
}
