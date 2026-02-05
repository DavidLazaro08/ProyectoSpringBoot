# ProyectoSpringBoot — Plataforma SaaS (Core)

Proyecto final desarrollado para la asignatura **Desarrollo de Interfaces**  
Grado Superior en Desarrollo de Aplicaciones Multiplataforma (2º DAM).

Este repositorio contiene la **implementación del core de una plataforma SaaS**, centrada en el diseño del modelo de datos, la persistencia con JPA/Hibernate y una validación funcional mínima mediante vistas Thymeleaf.

---

## 📌 Objetivo del proyecto

El objetivo del proyecto es desarrollar la base de una plataforma SaaS que permita:

- Registrar usuarios
- Asociar una suscripción a un plan
- Mantener un historial de cambios de suscripción
- Sentar las bases para una futura facturación automática

El trabajo se desarrolla **por semanas**, siguiendo una planificación incremental.

---

## 🗓️ Estado actual — SEMANA 1

En esta primera fase se ha trabajado exclusivamente el **modelo de datos y su validación básica**, cumpliendo los siguientes puntos:

### Modelo de datos
Se han definido las siguientes entidades principales:

- **Usuario**
- **Perfil**
- **Plan** (Basic, Premium, Enterprise)
- **Suscripción**
- **Factura**
- **Pago** (herencia: Tarjeta, PayPal, Transferencia)

El modelo está normalizado y preparado para soportar cambios futuros.

### JPA e Hibernate
- Uso de **Spring Data JPA**
- Enumeración `EstadoSuscripcion`:
  - `ACTIVA`
  - `CANCELADA`
  - `MOROSA`
- Auditoría de cambios mediante **Hibernate Envers**, aplicada sobre la entidad `Suscripcion` para registrar cambios de plan y fechas.

### Persistencia
- Base de datos relacional **PostgreSQL**
- Configuración mediante `application.properties`

### Vistas (Thymeleaf)
Se han implementado vistas funcionales mínimas para validar el flujo:

1. Registro de usuario
2. Selección de plan
3. Vista de resultado con confirmación de la suscripción  
   (ejemplo: *“Hola X, tu plan es Y”*)

No se ha priorizado la estética, sino la validación funcional.

### Datos de prueba
Existe una clase `DataInitializer` utilizada **únicamente durante el desarrollo** para:
- Cargar datos iniciales
- Probar el funcionamiento de la auditoría con Envers

No forma parte de la lógica de negocio final.

---

## 🧱 Arquitectura

El proyecto sigue una arquitectura **MVC** clara:

- `domain` → Entidades JPA
- `repository` → Repositorios Spring Data
- `service` → Lógica de negocio
- `controller` → Controladores web
- `templates` → Vistas Thymeleaf

---

## 🛠️ Tecnologías utilizadas

- Java
- Spring Boot
- Spring Data JPA
- Hibernate + Envers
- PostgreSQL
- Thymeleaf
- Maven

---

## 📈 Próximas fases (no implementadas aún)

- Renovación automática de suscripciones
- Cálculo de impuestos por país
- Prorrateo al cambiar de plan
- Filtros de facturación
- Pruebas unitarias con JUnit
- Mejora de la interfaz de usuario
- Documentación final del proyecto

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
