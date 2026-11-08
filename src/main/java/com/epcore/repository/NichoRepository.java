package com.epcore.repository;

import com.epcore.entity.Nicho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NichoRepository extends JpaRepository<Nicho, Long> {

    List<Nicho> findByActivoTrueOrderByNombreAsc();
}
