package ritech.exportaciones.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import ritech.exportaciones.dao.AvisoDAO;
import ritech.exportaciones.modelo.Usuario;
import ritech.exportaciones.servicio.CorreoServicio;

/**
 * El registro de los correos que manda el sistema.
 *
 * Sirve para dos cosas: comprobar que el aviso si salio, y ver que
 * decia exactamente. Un correo que se manda y no deja rastro no se
 * puede auditar despues, cuando el cliente diga que nunca le llego.
 */
@Controller
public class AvisoControlador {

    @Autowired
    private AvisoDAO avisoDAO;

    @Autowired
    private CorreoServicio correoServicio;

    @GetMapping("/avisos")
    public String mostrar(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeVerAvisos()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("avisos", avisoDAO.ultimos(60));
        modelo.addAttribute("enviados", avisoDAO.cuantos("ENVIADO"));
        modelo.addAttribute("pendientes", avisoDAO.cuantos("PENDIENTE"));
        modelo.addAttribute("fallidos", avisoDAO.cuantos("ERROR"));
        modelo.addAttribute("configurado", correoServicio.estaConfigurado());
        modelo.addAttribute("cuenta", correoServicio.cuenta());

        return "avisos";
    }
}
