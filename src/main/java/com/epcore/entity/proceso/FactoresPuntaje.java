package com.epcore.entity.proceso;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** Factores de puntaje del proceso (deben sumar 100). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class FactoresPuntaje {

    @Column(name = "pts_precio")
    private int precio;

    @Column(name = "pts_tecnica")
    private int tecnica;

    @Column(name = "pts_plazo")
    private int plazo;

    @Column(name = "pts_experiencia")
    private int experiencia;

    /** Desempeño histórico con la empresa (no aplica en licitación pública). */
    @Column(name = "pts_desempeno")
    private int desempeno;

    /** Apoyo a la industria nacional, Ley 816 de 2003 (licitación pública). */
    @Column(name = "pts_industria_nacional")
    private int industriaNacional;

    public int total() {
        return precio + tecnica + plazo + experiencia + desempeno + industriaNacional;
    }
}
