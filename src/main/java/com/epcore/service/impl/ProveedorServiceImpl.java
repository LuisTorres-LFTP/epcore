package com.epcore.service.impl;

import com.epcore.entity.Proveedor;
import com.epcore.repository.ProveedorRepository;
import com.epcore.service.ProveedorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Override
    public Proveedor guardarProveedor(Proveedor proveedor) {

        if (proveedor.getEstado() == null || proveedor.getEstado().isBlank()) {
            proveedor.setEstado("ACTIVO");
        }

        if (proveedor.getAutorizacionDatos() == null) {
            proveedor.setAutorizacionDatos(false);
        }

        return proveedorRepository.save(proveedor);
    }

    @Override
    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAll();
    }

    @Override
    public Optional<Proveedor> buscarPorId(Long idProveedor) {
        return proveedorRepository.findById(idProveedor);
    }

    @Override
    public Optional<Proveedor> buscarPorNit(String nit) {
        return proveedorRepository.findByNit(nit);
    }

    @Override
    public void eliminarProveedor(Long idProveedor) {
        proveedorRepository.deleteById(idProveedor);
    }

    @Override
    public List<String> listarSectoresEconomicos() {
        return proveedorRepository.listarSectoresEconomicos();
    }
}