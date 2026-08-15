package com.telco.ventas.entity;

import com.telco.ventas.enums.EstadoVenta;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agente_id", nullable = false)
    private Usuario agente;

    // --- Snapshot de datos del cliente al momento de la venta ---
    // Intencionalmente desnormalizado: no existe tabla clientes (ver docs/decisiones-tecnicas.md)
    @NotBlank
    @Pattern(regexp = "^\\d{8}$|^\\d{11}$", message = "dni_cliente debe tener 8 digitos (DNI) u 11 digitos (RUC)")
    @Column(name = "dni_cliente", nullable = false, length = 11)
    private String dniCliente;

    @NotBlank
    @Column(name = "nombre_cliente", nullable = false, length = 150)
    private String nombreCliente;

    @NotBlank
    @Pattern(regexp = "^\\d{9}$", message = "telefono_cliente debe tener 9 digitos")
    @Column(name = "telefono_cliente", nullable = false, length = 9)
    private String telefonoCliente;

    @NotBlank
    @Column(name = "direccion_cliente", nullable = false, length = 255)
    private String direccionCliente;
    // --- Fin snapshot cliente ---

    @Column(name = "plan_actual", length = 100)
    private String planActual;

    @NotBlank
    @Column(name = "plan_nuevo", nullable = false, length = 100)
    private String planNuevo;

    @NotBlank
    @Column(name = "codigo_llamada", nullable = false, unique = true, length = 50)
    private String codigoLlamada;

    @NotBlank
    @Builder.Default
    @Column(nullable = false, length = 50)
    private String producto = "FIJA_HOGAR";

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    // Usuario BACKOFFICE que aprobo/rechazo la venta (auditoria)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por")
    private Usuario validadoPor;

    @NotNull
    @Builder.Default
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}