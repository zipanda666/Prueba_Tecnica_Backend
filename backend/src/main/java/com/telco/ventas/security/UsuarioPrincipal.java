package com.telco.ventas.security;

import com.telco.ventas.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Envuelve la entidad Usuario para que Spring Security la entienda.
 * El Controller la recibe con @AuthenticationPrincipal UsuarioPrincipal actual,
 * y de ahi saca actual.usuario() para pasarselo al Service (id, rol, etc).
 *
 * El rol se expone como authority "ROLE_<ROL>" (convencion de Spring Security),
 * asi @PreAuthorize("hasRole('BACKOFFICE')") funciona directo cuando armemos SecurityConfig.
 */
@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
    }

    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(usuario.getActivo());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}