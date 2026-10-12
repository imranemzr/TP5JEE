package com.example.service;

import com.example.model.Salle;
import com.example.model.Reservation;
import javax.persistence.criteria.*;
import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;

public class SalleServiceAncien {

    private EntityManager entityManager;

    public SalleServiceAncien(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Salle> rechercherSallesDisponibles(
            LocalDateTime debutRecherche,
            LocalDateTime finRecherche) {

        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                "SELECT r.salle.id FROM Reservation r WHERE " +
                "(:debut < r.fin AND :fin > r.debut)" +
                ")";

        TypedQuery<Salle> query =
                entityManager.createQuery(jpql, Salle.class);

        query.setParameter("debut", debutRecherche);
        query.setParameter("fin", finRecherche);

        return query.getResultList();
    }


    public List<Salle> rechercherSallesMultiCriteres(
            String nomRecherche,
            Integer capaciteMin,
            LocalDateTime debutRecherche,
            LocalDateTime finRecherche) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = new ArrayList<>();

        // Critère 1 : recherche par nom
        if (nomRecherche != null && !nomRecherche.isEmpty()) {
            predicates.add(
                    cb.like(
                            cb.lower(salle.get("nom")),
                            "%" + nomRecherche.toLowerCase() + "%"
                    )
            );
        }

        // Critère 2 : capacité minimale
        if (capaciteMin != null) {
            predicates.add(
                    cb.ge(salle.get("capacite"), capaciteMin)
            );
        }

        // Critère 3 : disponibilité sur un créneau
        if (debutRecherche != null && finRecherche != null) {

            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);

            subquery.select(reservation.get("salle").get("id"));

            Predicate chevauchement = cb.and(
                    cb.lessThan(reservation.<LocalDateTime>get("debut"), finRecherche),
                    cb.greaterThan(reservation.<LocalDateTime>get("fin"), debutRecherche)
            );

            subquery.where(chevauchement);

            predicates.add(
                    cb.not(salle.get("id").in(subquery))
            );
        }

        cq.select(salle).where(
                cb.and(predicates.toArray(new Predicate[0]))
        );

        TypedQuery<Salle> query = entityManager.createQuery(cq);

        return query.getResultList();
    }


    public List<Salle> rechercherSallesAvecPagination(int pageNumber, int pageSize) {

        if (pageNumber < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Page et taille doivent être positives");
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        cq.select(salle);
        cq.orderBy(cb.asc(salle.get("id")));

        TypedQuery<Salle> query = entityManager.createQuery(cq);

        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }


    public long compterSalles() {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Salle> salle = cq.from(Salle.class);

        cq.select(cb.count(salle));

        return entityManager.createQuery(cq).getSingleResult();
    }

    public int calculerTotalPages(int pageSize) {

        if (pageSize < 1) {
            throw new IllegalArgumentException("La taille de page doit être positive");
        }

        long totalElements = compterSalles();

        return (int) Math.ceil((double) totalElements / pageSize);
    }



}
