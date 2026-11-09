package com.epcore.repository;

import com.epcore.entity.EntradaListaRestrictiva;
import com.epcore.entity.ListaRestrictiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EntradaListaRestrictivaRepository extends JpaRepository<EntradaListaRestrictiva, Long> {

    @Modifying
    @Query("DELETE FROM EntradaListaRestrictiva e WHERE e.lista = :lista")
    void borrarLista(ListaRestrictiva lista);

    long countByLista(ListaRestrictiva lista);

    List<EntradaListaRestrictiva> findByNombreNormalizadoIn(List<String> nombres);
}
