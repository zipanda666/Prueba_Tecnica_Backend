package com.telco.ventas.service;

import com.telco.ventas.dto.request.ActualizarUsuarioRequest;
import com.telco.ventas.dto.request.CrearUsuarioRequest;
import com.telco.ventas.dto.response.AgenteResponse;
import com.telco.ventas.dto.response.UsuarioResponse;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.enums.RolUsuario;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger AUDIT_USUARIOS = LoggerFactory.getLogger("AUDIT.USUARIOS");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AgenteResponse> listarAgentesDeSupervisor(Long supervisorId) {
        return usuarioRepository.findBySupervisorIdAndRolOrderByUsername(supervisorId, RolUsuario.AGENTE)
                .stream()
                .map(AgenteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll(org.springframework.data.domain.Sort.by("username"))
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest req, Usuario admin) {
        if (usuarioRepository.existsByUsername(req.username())) {
            throw new BusinessException("Ya existe un usuario con username '" + req.username() + "'");
        }

        Usuario supervisor = resolverSupervisor(req.supervisorId(), req.rol());

        Usuario nuevo = Usuario.builder()
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .rol(req.rol())
                .supervisor(supervisor)
                .activo(true)
                .build();

        Usuario guardado = usuarioRepository.save(nuevo);

        AUDIT_USUARIOS.info("usuario={} rol={} accion=CREAR_USUARIO nuevoUsuario={} nuevoRol={} supervisorAsignado={}",
                admin.getUsername(), admin.getRol(), guardado.getUsername(), guardado.getRol(),
                supervisor != null ? supervisor.getUsername() : "ninguno");

        return UsuarioResponse.from(guardado);
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest req, Usuario admin) {
        Usuario usuario = obtenerOrFallar(id);

        // Si el username cambio, verificar que el nuevo no choque con otro usuario existente
        if (!usuario.getUsername().equals(req.username()) && usuarioRepository.existsByUsername(req.username())) {
            throw new BusinessException("Ya existe un usuario con username '" + req.username() + "'");
        }

        Usuario supervisor = resolverSupervisor(req.supervisorId(), req.rol());

        String usernameAnterior = usuario.getUsername();
        usuario.setUsername(req.username());
        usuario.setRol(req.rol());
        usuario.setSupervisor(supervisor);
        Usuario actualizado = usuarioRepository.save(usuario);

        AUDIT_USUARIOS.info("usuario={} rol={} accion=ACTUALIZAR_USUARIO usuarioEditado={} (antes: {}) nuevoRol={}",
                admin.getUsername(), admin.getRol(), actualizado.getUsername(), usernameAnterior, actualizado.getRol());

        return UsuarioResponse.from(actualizado);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, Usuario admin) {
        if (id.equals(admin.getId()) && !activo) {
            throw new BusinessException("No puedes desactivar tu propio usuario");
        }

        Usuario usuario = obtenerOrFallar(id);
        usuario.setActivo(activo);
        Usuario actualizado = usuarioRepository.save(usuario);

        AUDIT_USUARIOS.info("usuario={} rol={} accion={} usuarioAfectado={}",
                admin.getUsername(), admin.getRol(), activo ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", actualizado.getUsername());

        return UsuarioResponse.from(actualizado);
    }

    @Transactional
    public void cambiarPassword(Long id, String nuevaPassword, Usuario admin) {
        Usuario usuario = obtenerOrFallar(id);
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        AUDIT_USUARIOS.info("usuario={} rol={} accion=CAMBIAR_PASSWORD usuarioAfectado={}",
                admin.getUsername(), admin.getRol(), usuario.getUsername());
    }

    private Usuario obtenerOrFallar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + id));
    }

    /** Valida y resuelve el supervisor a asignar. Un AGENTE siempre debe tener supervisor (regla de negocio). */
    private Usuario resolverSupervisor(Long supervisorId, RolUsuario rol) {
        if (supervisorId == null) {
            if (rol == RolUsuario.AGENTE) {
                throw new BusinessException("Un usuario con rol AGENTE debe tener un supervisor asignado");
            }
            return null;
        }
        Usuario supervisor = usuarioRepository.findById(supervisorId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + supervisorId + " para asignar como supervisor"));

        if (supervisor.getRol() != RolUsuario.SUPERVISOR) {
            throw new BusinessException(
                    "El usuario '" + supervisor.getUsername() + "' no tiene rol SUPERVISOR, no puede asignarse como supervisor_id");
        }
        return supervisor;
    }
}