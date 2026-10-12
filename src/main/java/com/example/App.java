
package com.example;

import com.example.model.Salle;
import com.example.model.Reservation;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

public class App {

    public static void main(String[] args) {

        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("SallePU");

        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();

            for (int i = 1; i <= 15; i++) {
                Salle salle = new Salle();
                salle.setNom("Salle " + i);
                salle.setCapacite(10 + i * 5);
                em.persist(salle);
            }

            em.getTransaction().commit();

            em.getTransaction().begin();

            Salle salle1 = em.find(Salle.class, 1L);

            Reservation reservation = new Reservation();
            reservation.setSalle(salle1);
            reservation.setDebut(LocalDateTime.of(2024, 6, 1, 9, 0));
            reservation.setFin(LocalDateTime.of(2024, 6, 1, 11, 0));

            em.persist(reservation);
            em.getTransaction().commit();

            SalleRepository repository = new SalleRepositoryImpl(em);
            SalleService service = new SalleServiceImpl(repository);

            LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 9, 0);
            LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 11, 0);

            System.out.println("\n=== TEST JPQL : SALLES DISPONIBLES ===");

            List<Salle> disponibles =
                    service.getAvailableRooms(debut, fin);

            for (Salle salle : disponibles) {
                System.out.println(salle.getNom()
                        + " - Capacité : " + salle.getCapacite());
            }

            System.out.println("\n=== TEST CRITERIA : CAPACITE >= 40 ===");

            List<Salle> resultats =
                    service.searchRooms(null, 40, null, null, 1, 10);

            for (Salle salle : resultats) {
                System.out.println(salle.getNom()
                        + " - Capacité : " + salle.getCapacite());
            }

            System.out.println("\n=== TEST PAGINATION : PAGE 2 ===");

            List<Salle> page2 =
                    service.searchRooms(null, null, null, null, 2, 5);

            for (Salle salle : page2) {
                System.out.println(salle.getNom());
            }

            long total = service.countRooms(
                    null, null, null, null
            );

            int totalPages = (int) Math.ceil((double) total / 5);

            System.out.println("\nTotal salles : " + total);
            System.out.println("Total pages : " + totalPages);

        } finally {
            em.close();
            emf.close();
        }
    }
}
