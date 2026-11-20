package ritech.exportaciones.controlador;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ritech.exportaciones.dao.LoteDAO;
import ritech.exportaciones.modelo.Lote;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class OrigenControlador {

    @Autowired
    private LoteDAO loteDAO;

    @GetMapping("/origen")
    public String consultarOrigen(@RequestParam(required = false) String codigo,
                                  HttpSession sesion,
                                  Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // La consulta de origen es para quien negocia o compra:
        // gerente, auxiliar y cliente europeo.
        if (!usuario.puedeConsultarOrigen()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("codigo", codigo);

        if (codigo == null || codigo.isBlank()) {
            return "origen";
        }

        Lote lote = loteDAO.consultarOrigen(codigo);

        if (lote == null) {
            modelo.addAttribute("error", "El lote " + codigo + " no fue encontrado.");
            return "origen";
        }

        if (lote.getLatitud() == null || lote.getLongitud() == null) {
            modelo.addAttribute("advertencia",
                "Este lote no tiene coordenadas registradas: sin trazabilidad.");
        }

        modelo.addAttribute("lote", lote);
        return "origen";
    }
}
