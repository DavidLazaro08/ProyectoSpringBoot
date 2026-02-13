package com.proyectospringboot.proyectosaas.service;

import com.proyectospringboot.proyectosaas.domain.entity.Suscripcion;
import com.proyectospringboot.proyectosaas.repository.SuscripcionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditoriaService {

        private final EntityManager entityManager;
        private final SuscripcionRepository suscripcionRepository;

        public AuditoriaService(EntityManager entityManager,
                        SuscripcionRepository suscripcionRepository) {
                this.entityManager = entityManager;
                this.suscripcionRepository = suscripcionRepository;
        }

        @org.springframework.transaction.annotation.Transactional(readOnly = true)
        public List<RevisionDTO> obtenerHistorialCambios() {
                AuditReader auditReader = AuditReaderFactory.get(entityManager);
                List<RevisionDTO> revisiones = new ArrayList<>();

                // Obtener todas las suscripciones para luego buscar sus revisiones
                List<Suscripcion> suscripciones = suscripcionRepository.findAll();

                for (Suscripcion suscripcion : suscripciones) {
                        // Obtener números de revisión para esta suscripción
                        List<Number> revisionNumbers = auditReader.getRevisions(Suscripcion.class, suscripcion.getId());

                        for (Number revNumber : revisionNumbers) {
                                // Obtener la entidad en esa revisión
                                Suscripcion suscripcionEnRevision = auditReader.find(Suscripcion.class,
                                                suscripcion.getId(), revNumber);

                                // Obtener fecha de la revisión
                                // Obtener fecha de la revisión
                                LocalDateTime fechaRevision = LocalDateTime.ofInstant(
                                                Instant.ofEpochMilli(auditReader.getRevisionDate(revNumber).getTime()),
                                                ZoneId.systemDefault());

                                String emailUsuario = suscripcionEnRevision.getUsuario() != null
                                                ? suscripcionEnRevision.getUsuario().getEmail()
                                                : "N/A";

                                String nombrePlan = (suscripcionEnRevision.getPlan() != null)
                                                ? suscripcionEnRevision.getPlan().getNombre()
                                                : "Plan desconocido";

                                String resumenCambio = "Plan: " + nombrePlan +
                                                ", Estado: " + suscripcionEnRevision.getEstado();

                                revisiones.add(new RevisionDTO(
                                                fechaRevision,
                                                "Suscripcion",
                                                suscripcion.getId(),
                                                emailUsuario,
                                                resumenCambio));
                        }
                }

                // Ordenar por fecha descendente
                revisiones.sort((r1, r2) -> r2.fechaRevision().compareTo(r1.fechaRevision()));

                return revisiones;
        }

        public record RevisionDTO(
                        LocalDateTime fechaRevision,
                        String entidad,
                        Long idEntidad,
                        String emailUsuario,
                        String resumenCambio) {
        }
}
