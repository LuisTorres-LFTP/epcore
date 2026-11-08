package com.epcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Agrupación comercial que define cada cliente para comparar proveedores.
 * Un proveedor entra al nicho si alguno de sus CIIU empieza por uno de los
 * prefijos (4 dígitos = clase exacta, 2 dígitos = toda la división).
 */
@Entity
@Table(name = "nichos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nicho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nicho")
    private Long idNicho;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "nicho_prefijos_ciiu", joinColumns = @JoinColumn(name = "id_nicho"))
    @Column(name = "prefijo_ciiu", length = 4)
    private Set<String> prefijosCiiu = new HashSet<>();

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @PrePersist
    public void prePersist() {
        if (activo == null) {
            activo = true;
        }
    }

    public boolean cubre(String codigoCiiu) {
        return codigoCiiu != null
                && prefijosCiiu.stream().anyMatch(codigoCiiu::startsWith);
    }
}
