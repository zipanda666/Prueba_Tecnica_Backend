package com.telco.ventas.service;

import com.telco.ventas.dto.request.VentaCreateRequest;
import com.telco.ventas.dto.response.ResumenResponse;
import com.telco.ventas.dto.response.VentaResponse;
import com.telco.ventas.entity.Usuario;
import com.telco.ventas.entity.Venta;
import com.telco.ventas.enums.EstadoVenta;
import com.telco.ventas.exception.BusinessException;
import com.telco.ventas.exception.ResourceNotFoundException;
import com.telco.ventas.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IMPORTANTE sobre @Transactional aqui:
 * Venta.agente y Venta.validadoPor son @ManyToOne(LAZY). Con `open-in-view: false` (application.yml),
 * la sesion de Hibernate se cierra apenas termina el metodo del Service - asi que la conversion a
 * VentaResponse (que llama venta.getAgente().getUsername()) TIENE que pasar DENTRO de este Service,
 * mientras la transaccion sigue abierta. Si esa conversion se hiciera en el Controller (como se hacia
 * antes), Hibernate ya no tendria sesion activa para resolver el proxy lazy -> LazyInitializationException.
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private static final Logger AUDIT_VENTAS = LoggerFactory.getLogger("AUDIT.VENTAS");

    private final VentaRepository ventaRepository;

    @Transactional
    public VentaResponse crear(VentaCreateRequest req, Usuario agente) {
        if (ventaRepository.existsByCodigoLlamada(req.codigoLlamada())) {
            throw new BusinessException("Ya existe una venta con codigo_llamada '" + req.codigoLlamada() + "'");
        }

        Venta venta = Venta.builder()
                .agente(agente)
                .dniCliente(req.dniCliente())
                .nombreCliente(req.nombreCliente())
                .telefonoCliente(req.telefonoCliente())
                .direccionCliente(req.direccionCliente())
                .planActual(req.planActual())
                .planNuevo(req.planNuevo())
                .codigoLlamada(req.codigoLlamada())
                .producto(req.producto() != null ? req.producto() : "FIJA_HOGAR")
                .monto(req.monto())
                .estado(EstadoVenta.PENDIENTE)
                .fechaRegistro(LocalDateTime.now())
                .build();

        Venta guardada = ventaRepository.save(venta);

        AUDIT_VENTAS.info("usuario={} rol={} accion=CREAR_VENTA ventaId={} codigoLlamada={} monto={}",
                agente.getUsername(), agente.getRol(), guardada.getId(), guardada.getCodigoLlamada(), guardada.getMonto());

        return VentaResponse.from(guardada);
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> misVentas(Usuario agente, EstadoVenta estado, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        Specification<Venta> spec = VentaSpecifications.combinar(
                VentaSpecifications.conAgenteId(agente.getId()),
                VentaSpecifications.conEstado(estado),
                VentaSpecifications.desdeFecha(desde),
                VentaSpecifications.hastaFecha(hasta)
        );
        return ventaRepository.findAll(spec, pageable).map(VentaResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> pendientes(Pageable pageable) {
        Specification<Venta> spec = VentaSpecifications.conEstado(EstadoVenta.PENDIENTE);
        return ventaRepository.findAll(spec, pageable).map(VentaResponse::from);
    }

    @Transactional
    public VentaResponse aprobar(Long ventaId, Usuario backoffice) {
        Venta venta = obtenerOrFallar(ventaId);
        validarTransicionDesdePendiente(venta);

        venta.setEstado(EstadoVenta.APROBADA);
        venta.setValidadoPor(backoffice);
        venta.setFechaValidacion(LocalDateTime.now());
        Venta actualizada = ventaRepository.save(venta);

        AUDIT_VENTAS.info("usuario={} rol={} accion=APROBAR_VENTA ventaId={} codigoLlamada={}",
                backoffice.getUsername(), backoffice.getRol(), venta.getId(), venta.getCodigoLlamada());

        return VentaResponse.from(actualizada);
    }

    @Transactional
    public VentaResponse rechazar(Long ventaId, String motivoRechazo, Usuario backoffice) {
        Venta venta = obtenerOrFallar(ventaId);
        validarTransicionDesdePendiente(venta);

        venta.setEstado(EstadoVenta.RECHAZADA);
        venta.setMotivoRechazo(motivoRechazo);
        venta.setValidadoPor(backoffice);
        venta.setFechaValidacion(LocalDateTime.now());
        Venta actualizada = ventaRepository.save(venta);

        AUDIT_VENTAS.info("usuario={} rol={} accion=RECHAZAR_VENTA ventaId={} codigoLlamada={} motivo=\"{}\"",
                backoffice.getUsername(), backoffice.getRol(), venta.getId(), venta.getCodigoLlamada(), motivoRechazo);

        return VentaResponse.from(actualizada);
    }

    @Transactional(readOnly = true)
    public Page<VentaResponse> equipo(Usuario supervisor, EstadoVenta estado, Long agenteId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        Specification<Venta> spec = VentaSpecifications.combinar(
                VentaSpecifications.conSupervisorId(supervisor.getId()),
                VentaSpecifications.conAgenteId(agenteId),
                VentaSpecifications.conEstado(estado),
                VentaSpecifications.desdeFecha(desde),
                VentaSpecifications.hastaFecha(hasta)
        );

        Page<VentaResponse> resultado = ventaRepository.findAll(spec, pageable).map(VentaResponse::from);

        AUDIT_VENTAS.info("usuario={} rol={} accion=CONSULTAR_EQUIPO filtros=[estado={}, agenteId={}, desde={}, hasta={}]",
                supervisor.getUsername(), supervisor.getRol(), estado, agenteId, desde, hasta);

        return resultado;
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumen(Usuario supervisor, LocalDateTime desde, LocalDateTime hasta) {
        List<VentaRepository.ResumenEstado> conteos = ventaRepository.contarPorEstado(desde, hasta, supervisor.getId());
        Map<String, Long> conteosPorEstado = new LinkedHashMap<>();
        for (EstadoVenta e : EstadoVenta.values()) {
            conteosPorEstado.put(e.name(), 0L);
        }
        for (VentaRepository.ResumenEstado c : conteos) {
            conteosPorEstado.put(c.getEstado().name(), c.getCantidad());
        }

        BigDecimal montoTotalAprobadas = ventaRepository.sumarMontoAprobadas(desde, hasta, supervisor.getId());

        List<ResumenResponse.VentasPorDiaItem> serie = ventaRepository.ventasPorDia(desde, hasta, supervisor.getId())
                .stream()
                .map(v -> new ResumenResponse.VentasPorDiaItem(v.getDia().toLocalDate(), v.getCantidad(), v.getMonto()))
                .toList();

        AUDIT_VENTAS.info("usuario={} rol={} accion=CONSULTAR_RESUMEN desde={} hasta={}",
                supervisor.getUsername(), supervisor.getRol(), desde, hasta);

        return new ResumenResponse(conteosPorEstado, montoTotalAprobadas, serie);
    }

    private Venta obtenerOrFallar(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una venta con id " + id));
    }

    private void validarTransicionDesdePendiente(Venta venta) {
        if (venta.getEstado() != EstadoVenta.PENDIENTE) {
            throw new BusinessException(
                    "Solo se pueden aprobar/rechazar ventas en estado PENDIENTE (venta actual: " + venta.getEstado() + ")");
        }
    }
}