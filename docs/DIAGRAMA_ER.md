# 📊 Diagrama Entidad-Relación (E-R) - Proyecto SaaS

## 🖼️ Diagrama Visual

![Diagrama E-R del Sistema SaaS](./DIAGRAMA_ER.png)

---

## 📐 Diagrama Mermaid (Código)

Puedes copiar este código y visualizarlo en cualquier editor compatible con Mermaid (GitHub, VS Code con extensión, etc.):

```mermaid
erDiagram
    USUARIOS ||--|| PERFILES : "1:1"
    USUARIOS ||--|| SUSCRIPCIONES : "1:1"
    PLANES ||--o{ SUSCRIPCIONES : "1:N"
    SUSCRIPCIONES ||--o{ FACTURAS : "1:N"
    FACTURAS ||--|| PAGOS : "1:1"
    PAGOS ||--|| PAGOS_TARJETA : "herencia"
    PAGOS ||--|| PAGOS_PAYPAL : "herencia"
    PAGOS ||--|| PAGOS_TRANSFERENCIA : "herencia"

    USUARIOS {
        Long id PK
        String email UK "UNIQUE"
        String pais
        String password
        String rol "ENUM: USER, ADMIN"
        Boolean pagoAutomatico "DEFAULT false"
        String metodoPagoPreferido
        LocalDateTime fechaAlta
    }

    PERFILES {
        Long id PK
        Long usuario_id FK "UNIQUE, FK->USUARIOS"
        String nombre
        String apellidos
        String telefono
    }

    PLANES {
        Long id PK
        String nombre UK "UNIQUE (Basic, Premium, Enterprise)"
        Double precioMensual
    }

    SUSCRIPCIONES {
        Long id PK
        Long usuario_id FK "UNIQUE, FK->USUARIOS"
        Long plan_id FK "FK->PLANES"
        EstadoSuscripcion estado "ENUM: ACTIVA, CANCELADA, MOROSA"
        LocalDateTime fechaInicio
        LocalDateTime fechaFinCiclo
        LocalDateTime fechaCancelacion "nullable"
    }

    FACTURAS {
        Long id PK
        Long suscripcion_id FK "FK->SUSCRIPCIONES"
        Double importe
        LocalDateTime fecha
    }

    PAGOS {
        Long id PK
        Long factura_id FK "UNIQUE, FK->FACTURAS"
        Double importe
        LocalDateTime fecha
    }

    PAGOS_TARJETA {
        Long id PK_FK "PK y FK->PAGOS"
        String ultimos4
        String titular
    }

    PAGOS_PAYPAL {
        Long id PK_FK "PK y FK->PAGOS"
        String emailPaypal
    }

    PAGOS_TRANSFERENCIA {
        Long id PK_FK "PK y FK->PAGOS"
        String iban
        String referencia
    }
```

---

## 📋 Leyenda

| Símbolo | Significado |
|---------|-------------|
| **PK** | Primary Key (Clave Primaria) |
| **FK** | Foreign Key (Clave Foránea) |
| **UK** | Unique Constraint (Restricción de unicidad) |
| **1:1** | Relación uno a uno |
| **1:N** | Relación uno a muchos |
| **🔍 AUDITED** | Tabla auditada con Hibernate Envers |

---

## 🎯 Resumen de Relaciones

### Relaciones Principales

1. **USUARIOS ↔ PERFILES** (1:1)
   - FK: `perfiles.usuario_id` → `usuarios.id`
   - Constraint: UNIQUE, NOT NULL

2. **USUARIOS ↔ SUSCRIPCIONES** (1:1)
   - FK: `suscripciones.usuario_id` → `usuarios.id`
   - Constraint: UNIQUE, NOT NULL

3. **PLANES ↔ SUSCRIPCIONES** (1:N)
   - FK: `suscripciones.plan_id` → `planes.id`
   - Un plan puede tener muchas suscripciones

4. **SUSCRIPCIONES ↔ FACTURAS** (1:N)
   - FK: `facturas.suscripcion_id` → `suscripciones.id`
   - Una suscripción puede generar múltiples facturas

5. **FACTURAS ↔ PAGOS** (1:1)
   - FK: `pagos.factura_id` → `facturas.id`
   - Constraint: UNIQUE, NOT NULL

### Herencia de Tablas (JOINED Strategy)

**PAGOS** (tabla padre) tiene 3 tablas hijas:
- `PAGOS_TARJETA` - Pagos con tarjeta de crédito/débito
- `PAGOS_PAYPAL` - Pagos con PayPal
- `PAGOS_TRANSFERENCIA` - Pagos por transferencia bancaria

Cada tabla hija:
- Tiene su propio `id` que es **PK y FK** a `pagos.id`
- Hereda los campos comunes de `PAGOS`
- Añade campos específicos del tipo de pago

---

## 🔍 Auditoría con Hibernate Envers

La tabla **SUSCRIPCIONES** está marcada con `@Audited`, lo que significa que Hibernate Envers creará automáticamente:

### Tabla de Auditoría: `suscripciones_AUD`

```sql
CREATE TABLE suscripciones_AUD (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    usuario_id BIGINT,
    plan_id BIGINT,
    estado VARCHAR(20),
    fechaInicio TIMESTAMP,
    fechaFinCiclo TIMESTAMP,
    fechaCancelacion TIMESTAMP,
    PRIMARY KEY (id, REV)
);
```

### Tabla de Revisiones: `REVINFO`

```sql
CREATE TABLE REVINFO (
    REV INTEGER NOT NULL AUTO_INCREMENT,
    REVTSTMP BIGINT,
    PRIMARY KEY (REV)
);
```

**Tipos de revisión (REVTYPE):**
- `0` = ADD (inserción)
- `1` = MOD (modificación)
- `2` = DEL (eliminación)

---

## 📊 Tablas Generadas en la Base de Datos

Al ejecutar la aplicación, JPA/Hibernate creará las siguientes tablas:

### Tablas Principales
1. `usuarios`
2. `perfiles`
3. `planes`
4. `suscripciones`
5. `facturas`
6. `pagos` (tabla padre)
7. `pagos_tarjeta`
8. `pagos_paypal`
9. `pagos_transferencia`

### Tablas de Auditoría (Envers)
10. `suscripciones_AUD`
11. `REVINFO`

**Total: 11 tablas**

---

## 🎨 Código de Colores del Diagrama

- **🔵 Azul** - Entidades de Usuario/Perfil
- **🟢 Verde** - Entidades de Suscripción/Plan (incluye auditoría)
- **🟠 Naranja** - Entidades de Facturación
- **🟣 Morado** - Entidades de Pago (con herencia)

---

## 📝 Notas Técnicas

### Estrategia de Herencia: JOINED

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Pago { ... }
```

**Ventajas:**
- ✅ Normalización completa (sin redundancia)
- ✅ Cada tipo de pago en su propia tabla
- ✅ Fácil de extender con nuevos tipos de pago
- ✅ Consultas eficientes por tipo específico

**Desventajas:**
- ⚠️ Requiere JOINs para consultas polimórficas
- ⚠️ Más tablas en la base de datos

### Enum EstadoSuscripcion

```java
@Enumerated(EnumType.STRING)
private EstadoSuscripcion estado;
```

**Valores posibles:**
- `ACTIVA` - Suscripción activa y al día
- `CANCELADA` - Suscripción cancelada por el usuario
- `MOROSA` - Suscripción con pagos pendientes

---

## 🚀 Próximos Pasos

1. ✅ Validar el modelo con el profesor/tutor
2. ✅ Implementar servicios de negocio
3. ✅ Crear tests para las relaciones
4. ✅ Probar la auditoría de Envers
5. ✅ Implementar la lógica de facturación automática

---

**Proyecto:** proyecto-saas  
**Tecnologías:** Spring Boot 4.0.2 + JPA + Hibernate Envers  
**Autor:** David Lázaro  
**Fecha:** 2026-02-05
