package com.example.repository;

import com.example.model.Salle;
import com.example.model.Reservation;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SalleRepositoryImpl implements SalleRepository {

    private final EntityManager entityManager;

    public SalleRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Salle> findAvailableRooms(
            LocalDateTime debut,
            LocalDateTime fin) {

        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN (" +
                "SELECT r.salle.id FROM Reservation r WHERE " +
                "(:debut < r.fin AND :fin > r.debut))";

        return entityManager.createQuery(jpql, Salle.class)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    @Override
    public List<Salle> searchRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin,
            int page,
            int size) {

        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Page et taille invalides");
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(
                cb, cq, salle, nom, capaciteMin, debut, fin
        );

        cq.select(salle);
        cq.where(cb.and(predicates.toArray(new Predicate[0])));
        cq.orderBy(cb.asc(salle.get("id")));

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        query.setFirstResult((page - 1) * size);
        query.setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public long countRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(
                cb, cq, salle, nom, capaciteMin, debut, fin
        );

        cq.select(cb.count(salle));
        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> cq,
            Root<Salle> salle,
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin) {

        List<Predicate> predicates = new ArrayList<>();

        if (nom != null && !nom.isEmpty()) {
            predicates.add(
                    cb.like(
                            cb.lower(salle.get("nom")),
                            "%" + nom.toLowerCase() + "%"
                    )
            );
        }

        if (capaciteMin != null) {
            predicates.add(
                    cb.ge(salle.get("capacite"), capaciteMin)
            );
        }

        if (debut != null && fin != null) {

            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<Reservation> reservation =
                    subquery.from(Reservation.class);

            subquery.select(reservation.get("salle").get("id"));

            Predicate chevauchement = cb.and(
                    cb.lessThan(
                            reservation.<LocalDateTime>get("debut"), fin
                    ),
                    cb.greaterThan(
                            reservation.<LocalDateTime>get("fin"), debut
                    )
            );

            subquery.where(chevauchement);

            predicates.add(
                    cb.not(salle.get("id").in(subquery))
            );
        }

        return predicates;
    }
}
