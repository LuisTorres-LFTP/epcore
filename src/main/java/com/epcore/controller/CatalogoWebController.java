package com.epcore.controller;

import com.epcore.entity.ListaRestrictiva;
import com.epcore.service.catalogo.ImportadorCiiuService;
import com.epcore.service.catalogo.ListasRestrictivasService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.InputStream;
import java.util.List;

/** Actualización de catálogos oficiales: CIIU (DANE) y listas restrictivas (OFAC, ONU). */
@Controller
@RequestMapping("/catalogos")
public class CatalogoWebController {

    private final ImportadorCiiuService ciiu;
    private final ListasRestrictivasService listas;

    public CatalogoWebController(ImportadorCiiuService ciiu, ListasRestrictivasService listas) {
        this.ciiu = ciiu;
        this.listas = listas;
    }

    @GetMapping
    public String ver(Model model) {
        model.addAttribute("conteos", listas.conteos());
        return "catalogos/index";
    }

    @PostMapping("/ciiu")
    public String importarCiiu(@RequestParam("archivo") MultipartFile archivo,
                               @RequestParam(required = false) String version, RedirectAttributes f) {
        if (archivo == null || archivo.isEmpty()) {
            f.addFlashAttribute("error", "Seleccione el archivo CSV del catálogo CIIU.");
            return "redirect:/catalogos";
        }
        try (InputStream in = archivo.getInputStream()) {
            f.addFlashAttribute("resumenCiiu", ciiu.importar(in, version));
            f.addFlashAttribute("mensaje", "Catálogo CIIU actualizado.");
        } catch (IllegalArgumentException e) {
            f.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            f.addFlashAttribute("error", "No se pudo leer el archivo. Verifique que sea un CSV codigo;descripcion en UTF-8.");
        }
        return "redirect:/catalogos";
    }

    @PostMapping("/listas")
    public String cargarLista(@RequestParam("archivo") MultipartFile archivo,
                              @RequestParam ListaRestrictiva lista, RedirectAttributes f) {
        if (archivo == null || archivo.isEmpty()) {
            f.addFlashAttribute("error", "Seleccione el archivo oficial de la lista.");
            return "redirect:/catalogos";
        }
        try (InputStream in = archivo.getInputStream()) {
            List<String> nombres = lista == ListaRestrictiva.ONU
                    ? ListasRestrictivasService.parsearOnu(in) : ListasRestrictivasService.parsearOfac(in);
            ListasRestrictivasService.Resultado r = listas.cargarYCruzar(lista, nombres);
            f.addFlashAttribute("mensaje", "Lista " + r.lista() + " cargada: " + r.nombres() + " nombres. "
                    + r.coincidencias() + " posibles coincidencias enviadas al oficial de cumplimiento.");
        } catch (IllegalArgumentException e) {
            f.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            f.addFlashAttribute("error", "No se pudo leer el archivo de la lista.");
        }
        return "redirect:/catalogos";
    }
}
