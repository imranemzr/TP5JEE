package com.example.service;

import com.example.model.Salle;
import com.example.repository.SalleRepository;

import java.time.LocalDateTime;
import java.util.List;

public class SalleServiceImpl implements SalleService {

    private final SalleRepository salleRepository;

    public SalleServiceImpl(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    @Override
    public List<Salle> getAvailableRooms(
            LocalDateTime debut,
            LocalDateTime fin) {

        return salleRepository.findAvailableRooms(debut, fin);
    }

    @Override
    public List<Salle> searchRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin,
            int page,
            int size) {

        return salleRepository.searchRooms(
                nom, capaciteMin, debut, fin, page, size
        );
    }

    @Override
    public long countRooms(
            String nom,
            Integer capaciteMin,
            LocalDateTime debut,
            LocalDateTime fin) {

        return salleRepository.countRooms(
                nom, capaciteMin, debut, fin
        );
    }
}
