# ProyectoSpringBoot — Plataforma SaaS (Core)

Proyecto final desarrollado para la asignatura **Desarrollo de Interfaces**  
Grado Superior en Desarrollo de Aplicaciones Multiplataforma (2º DAM).

Este repositorio contiene la **implementación del core de una plataforma SaaS**, centrada en el diseño del modelo de datos, la persistencia con JPA/Hibernate y una validación funcional mínima mediante vistas Thymeleaf.

---

## Levantar Base de Datos (PostgreSQL con Docker)

Este proyecto utiliza PostgreSQL. Para poder ejecutarlo correctamente, es necesario levantar previamente la base de datos mediante Docker.

### Pasos:

1. Tener Docker instalado.
2. Desde la raíz del proyecto ejecutar:

   ```bash
   docker compose up -d
   ```

3. Arrancar la aplicación Spring Boot desde IntelliJ o con:

   ```bash
   mvn spring-boot:run
   ```

La base de datos se levantará en `localhost:5433` y Hibernate creará/actualizará automáticamente las tablas gracias a la configuración:

```properties
spring.jpa.hibernate.ddl-auto=update
```

---

## 📌 Objetivo del proyecto

El objetivo del proyecto es desarrollar la base de una plataforma SaaS que permita:

- Registrar usuarios
- Asociar una suscripción a un plan
- Mantener un historial de cambios de suscripción
- Sentar las bases para una futura facturación automática

El trabajo se desarrolla **por semanas**, siguiendo una planificación incremental.

---

## 🗓️ Estado actual — SEMANA 2 (Completada)

Se ha implementado la lógica de negocio y la gestión avanzada de planes y facturación.

### Funcionalidades Implementadas
- **Renovación de suscripciones**: Lógica para cerrar ciclos de facturación y abrir nuevos.
- **Cálculo de impuestos**: Sistema dinámico basado en el país del usuario (España 21%, USA 10%, Francia 20%, etc.).
- **Facturación**:
  - Generación automática de facturas al renovar.
  - Vistas con filtros por fecha y monto (JPA Criteria / Specifications).
  - Descarga simulada de PDF.
- **Pagos**:
  - Simulación de pasarela de pago (éxito/fallo aleatorio).
  - Gestión de estados de suscripción (ACTIVA, PENDIENTE_PAGO, CANCELADA).
- **Auditoría (Admin)**:
  - Panel de administrador protegido con clave simple.
  - Visualización de historial de cambios en suscripciones (Envers) para ver quién cambió de plan y cuándo.

### Refactorización y Calidad
- Métodos de repositorios en español y optimizados (`buscarPorUsuarioId`, `buscarVencidas`).
- Uso de DTOs para transferir datos a la vista (`DashboardDTO`, `FacturaFiltroDTO`).

### 📸 Capturas de Pantalla

| Home | Registro |
| :---: | :---: |
| ![Home](src/main/resources/capturas/01%20Home.png) | ![Registro](src/main/resources/capturas/02%20Registro.png) |

| Dashboard | Facturas |
| :---: | :---: |
| ![Dashboard](src/main/resources/capturas/03%20Dashboard.png) | ![Facturas](src/main/resources/capturas/04%20Facturas.png) |

| Panel Admin (Auditoría) |
| :---: |
| ![Admin](src/main/resources/capturas/05%20Admin.png) |

---

## 📈 Próximas fases (Roadmap)

- Implementación de seguridad real con Spring Security (Login/Roles)
- API REST para consumo externo
- Pruebas unitarias con JUnit y Mockito (Cobertura > 80%)
- Despliegue en entorno Cloud (Docker Compose + Render/AWS)

## ✅ Pruebas Unitarias (JUnit)

Se han implementado tests para asegurar la lógica crítica del negocio. Dado que es un **MVP Académico**, no hemos buscado cobertura 100%, sino probar lo importante:

### Ejecutar Tests
Desde IntelliJ: Click derecho en folder `src/test/java` -> `Run 'All Tests'`

Desde Maven:
```bash
mvn test
```

### Casos Cubiertos
1. **Impuestos**: Verificación de regla 21% (ES) vs 0% (Resto).
2. **Renovación**: Solo se cobra cuando la fecha ha vencido.
3. **Prorrateo**:
   - Upgrade (Basic -> Premium): Cobra diferencia prorrateada.
   - Downgrade (Premium -> Basic): No cobra nada.
4. **Facturación Masiva**: El proceso batch genera N facturas correctamente.

---

## 📂 Control de versiones

El proyecto se desarrolla con control de versiones mediante **Git**  
y se entrega en un repositorio público de GitHub con el nombre:

**ProyectoSpringBoot**

---

## ✍️ Notas finales

Este proyecto está planteado con un enfoque **académico y progresivo**, priorizando:
- Claridad del modelo
- Simplicidad defendible
- Coherencia con los requisitos de cada fase

El desarrollo continuará en las siguientes semanas conforme a la planificación establecida.
