package ritech.exportaciones.controlador;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import ritech.exportaciones.dao.ReporteDAO;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class ReporteControlador {

    @Autowired
    private ReporteDAO reporteDAO;

    @GetMapping("/reportes")
    public String mostrar(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Los reportes muestran ventas, totales, margenes y riesgo
        // de incumplimiento. Esa informacion es de la gerencia.
        // El administrador del sistema ya no la ve.
        if (!usuario.puedeVerReportes()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("resumen", reporteDAO.resumenGeneral());
        modelo.addAttribute("porPais", reporteDAO.porPaisDestino());
        modelo.addAttribute("porProveedor", reporteDAO.porProveedor());
        modelo.addAttribute("porProducto", reporteDAO.porProducto());
        modelo.addAttribute("eudr", reporteDAO.cumplimientoEudr());
        modelo.addAttribute("hoy", LocalDate.now().toString());

        return "reportes";
    }
}
