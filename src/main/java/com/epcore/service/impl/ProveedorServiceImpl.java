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

        validarCamposObligatorios(proveedor);
        proveedor.setNit(proveedor.getNit().trim());

        proveedorRepository.findByNit(proveedor.getNit())
                .filter(existente -> !existente.getIdProveedor().equals(proveedor.getIdProveedor()))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException(
                            "Ya existe un proveedor registrado con el NIT " + proveedor.getNit() + ".");
                });

        if (proveedor.getEstado() == null || proveedor.getEstado().isBlank()) {
            proveedor.setEstado("ACTIVO");
        }

        if (proveedor.getAutorizacionDatos() == null) {
            proveedor.setAutorizacionDatos(false);
        }

        return proveedorRepository.save(proveedor);
    }

    private void validarCamposObligatorios(Proveedor proveedor) {
        if (esVacio(proveedor.getTipoPersona())) {
            throw new IllegalArgumentException("Seleccione el tipo de persona.");
        }
        if (esVacio(proveedor.getNit())) {
            throw new IllegalArgumentException("El NIT es obligatorio.");
        }
        if (esVacio(proveedor.getRazonSocial())) {
            throw new IllegalArgumentException("La razón social es obligatoria.");
        }
        if (esVacio(proveedor.getCorreo())) {
            throw new IllegalArgumentException("El correo es obligatorio.");
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
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