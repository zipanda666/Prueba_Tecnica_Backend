package com.telco.ventas.repository;

import com.telco.ventas.entity.Usuario;
import com.telco.ventas.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Usuario> findBySupervisorIdAndRolOrderByUsername(Long supervisorId, RolUsuario rol);
}