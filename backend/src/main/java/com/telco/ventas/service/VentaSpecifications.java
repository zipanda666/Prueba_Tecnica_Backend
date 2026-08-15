package com.telco.ventas.service;

import com.telco.ventas.entity.Venta;
import com.telco.ventas.enums.EstadoVenta;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Cada metodo devuelve un filtro opcional: si el parametro es null, no agrega condicion (Specification.where(null)
 * es un "true" que no filtra nada). Se combinan con .and() en el Service segun el caso de uso.
 */
public class VentaSpecifications {

    private VentaSpecifications() {
    }

    public static Specification<Venta> conAgenteId(Long agenteId) {
        if (agenteId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("agente").get("id"), agenteId);
    }

    public static Specification<Venta> conSupervisorId(Long supervisorId) {
        if (supervisorId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("agente").get("supervisor").get("id"), supervisorId);
    }

    public static Specification<Venta> conEstado(EstadoVenta estado) {
        if (estado == null) return null;
        return (root, query, cb) -> cb.equal(root.get("estado"), estado);
    }

    public static Specification<Venta> desdeFecha(LocalDateTime desde) {
        if (desde == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde);
    }

    public static Specification<Venta> hastaFecha(LocalDateTime hasta) {
        if (hasta == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaRegistro"), hasta);
    }

    /** Combina cualquier cantidad de specs, ignorando las que sean null. */
    @SafeVarargs
    public static Specification<Venta> combinar(Specification<Venta>... specs) {
        Specification<Venta> result = Specification.where(null);
        for (Specification<Venta> s : specs) {
            if (s != null) {
                result = result.and(s);
            }
        }
        return result;
    }
}