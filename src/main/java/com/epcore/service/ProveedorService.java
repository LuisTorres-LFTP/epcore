package com.epcore.service;

import com.epcore.entity.Proveedor;

import java.util.List;
import java.util.Optional;

public interface ProveedorService {

    Proveedor guardarProveedor(Proveedor proveedor);

    List<Proveedor> listarProveedores();

    Optional<Proveedor> buscarPorId(Long idProveedor);

    Optional<Proveedor> buscarPorNit(String nit);

    void eliminarProveedor(Long idProveedor);

    List<String> listarSectoresEconomicos();
}