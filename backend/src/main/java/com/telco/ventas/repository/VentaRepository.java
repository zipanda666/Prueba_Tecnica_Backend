package com.telco.ventas.repository;

import com.telco.ventas.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    boolean existsByCodigoLlamada(String codigoLlamada);

    // --- Reportes: conteo por estado dentro de un rango, filtrado por supervisor ---
    // agenteSupervisorId = null -> sin filtrar por supervisor (uso admin/backoffice a futuro)
    @Query("""
            SELECT v.estado AS estado, COUNT(v) AS cantidad
            FROM Venta v
            WHERE v.fechaRegistro BETWEEN :desde AND :hasta
              AND (:supervisorId IS NULL OR v.agente.supervisor.id = :supervisorId)
            GROUP BY v.estado
            """)
    List<ResumenEstado> contarPorEstado(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("supervisorId") Long supervisorId);

    @Query("""
            SELECT COALESCE(SUM(v.monto), 0)
            FROM Venta v
            WHERE v.estado = com.telco.ventas.enums.EstadoVenta.APROBADA
              AND v.fechaRegistro BETWEEN :desde AND :hasta
              AND (:supervisorId IS NULL OR v.agente.supervisor.id = :supervisorId)
            """)
    BigDecimal sumarMontoAprobadas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("supervisorId") Long supervisorId);

    @Query("""
            SELECT CAST(v.fechaRegistro AS date) AS dia, COUNT(v) AS cantidad, COALESCE(SUM(v.monto), 0) AS monto
            FROM Venta v
            WHERE v.fechaRegistro BETWEEN :desde AND :hasta
              AND (:supervisorId IS NULL OR v.agente.supervisor.id = :supervisorId)
            GROUP BY CAST(v.fechaRegistro AS date)
            ORDER BY CAST(v.fechaRegistro AS date)
            """)
    List<VentasPorDia> ventasPorDia(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("supervisorId") Long supervisorId);

    // Interfaces de proyeccion (Spring Data las mapea automaticamente desde el SELECT)
    interface ResumenEstado {
        com.telco.ventas.enums.EstadoVenta getEstado();
        Long getCantidad();
    }

    interface VentasPorDia {
        java.sql.Date getDia();
        Long getCantidad();
        BigDecimal getMonto();
    }
}