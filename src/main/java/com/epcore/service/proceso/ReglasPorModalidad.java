package com.epcore.service.proceso;

import com.epcore.entity.proceso.FactoresPuntaje;
import com.epcore.entity.proceso.ProcesoEnums.Modalidad;
import com.epcore.entity.proceso.ProcesoEnums.Visibilidad;
import com.epcore.entity.proceso.RequisitosProceso;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.epcore.service.proceso.ReglasModalidad.bd;

/** Implementaciones de ReglasModalidad y el selector según la modalidad del proceso. */
@Component
public class ReglasPorModalidad {

    /** Entidad financiera: SARLAFT y homologación obligatorios; puntúa desempeño histórico. */
    static final class ReglasFinanciera implements ReglasModalidad {
        public Modalidad modalidad() { return Modalidad.FINANCIERA; }
        public Visibilidad visibilidadPorDefecto() { return Visibilidad.INVITACION; }
        public RequisitosProceso plantillaRequisitos() {
            return RequisitosProceso.builder().liquidezMinima(bd("1.2")).endeudamientoMaximo(bd("0.70"))
                    .coberturaInteresesMinima(bd("2")).patrimonioMinimoPct(20).contratosMinimos(2)
                    .experienciaMinimaPct(50).codigosUnspsc("").exigirSarlaft(true).exigirDocumentosAprobados(true).build();
        }
        public FactoresPuntaje plantillaFactores() {
            return FactoresPuntaje.builder().precio(40).tecnica(35).plazo(5).experiencia(10).desempeno(10).build();
        }
        public List<String> validarFactores(FactoresPuntaje f) {
            List<String> e = ReglasModalidad.super.validarFactores(f);
            if (f != null && f.getIndustriaNacional() > 0) {
                e.add("El puntaje por industria nacional (Ley 816 de 2003) solo aplica en licitación pública.");
            }
            return e;
        }
    }

    /** Convocatoria privada: requisitos más flexibles. */
    static final class ReglasPrivada implements ReglasModalidad {
        public Modalidad modalidad() { return Modalidad.PRIVADA; }
        public Visibilidad visibilidadPorDefecto() { return Visibilidad.INVITACION; }
        public RequisitosProceso plantillaRequisitos() {
            return RequisitosProceso.builder().liquidezMinima(bd("1.0")).endeudamientoMaximo(bd("0.75"))
                    .coberturaInteresesMinima(bd("1.5")).patrimonioMinimoPct(10).contratosMinimos(1)
                    .experienciaMinimaPct(30).codigosUnspsc("").exigirSarlaft(true).exigirDocumentosAprobados(true).build();
        }
        public FactoresPuntaje plantillaFactores() {
            return FactoresPuntaje.builder().precio(45).tecnica(30).plazo(10).desempeno(15).build();
        }
        public List<String> validarFactores(FactoresPuntaje f) {
            List<String> e = ReglasModalidad.super.validarFactores(f);
            if (f != null && f.getIndustriaNacional() > 0) {
                e.add("El puntaje por industria nacional (Ley 816 de 2003) solo aplica en licitación pública.");
            }
            return e;
        }
    }

    /** Licitación pública (Ley 80/1993, Ley 1150/2007, Decreto 1082/2015): no puntúa desempeño. */
    static final class ReglasPublica implements ReglasModalidad {
        public Modalidad modalidad() { return Modalidad.PUBLICA; }
        public Visibilidad visibilidadPorDefecto() { return Visibilidad.SECOP; }
        public RequisitosProceso plantillaRequisitos() {
            return RequisitosProceso.builder().liquidezMinima(bd("1.2")).endeudamientoMaximo(bd("0.65"))
                    .coberturaInteresesMinima(bd("2")).patrimonioMinimoPct(0).contratosMinimos(3)
                    .experienciaMinimaPct(100).codigosUnspsc("").exigirSarlaft(false).exigirDocumentosAprobados(true).build();
        }
        public FactoresPuntaje plantillaFactores() {
            return FactoresPuntaje.builder().precio(50).tecnica(30).experiencia(10).industriaNacional(10).build();
        }
        public List<String> validarFactores(FactoresPuntaje f) {
            List<String> e = ReglasModalidad.super.validarFactores(f);
            if (f != null && f.getDesempeno() > 0) {
                e.add("En licitación pública no se puede puntuar el desempeño histórico con la entidad.");
            }
            return e;
        }
    }

    private final Map<Modalidad, ReglasModalidad> reglas = new EnumMap<>(Modalidad.class);

    public ReglasPorModalidad() {
        for (ReglasModalidad r : List.of(new ReglasFinanciera(), new ReglasPrivada(), new ReglasPublica())) {
            reglas.put(r.modalidad(), r);
        }
    }

    public ReglasModalidad de(Modalidad modalidad) {
        ReglasModalidad r = reglas.get(modalidad);
        if (r == null) {
            throw new IllegalArgumentException("Modalidad no soportada: " + modalidad);
        }
        return r;
    }
}
