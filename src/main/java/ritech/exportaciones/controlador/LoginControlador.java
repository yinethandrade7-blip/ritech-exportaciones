package ritech.exportaciones.controlador;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ritech.exportaciones.dao.UsuarioDAO;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class LoginControlador {

    @Autowired
    private UsuarioDAO usuarioDAO;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String clave,
                                HttpSession sesion,
                                Model modelo) {

        if (correo.isBlank() || clave.isBlank()) {
            modelo.addAttribute("error", "Debe diligenciar el correo y la contraseña");
            return "login";
        }

        Usuario usuario = usuarioDAO.autenticar(correo, clave);

        if (usuario == null) {
            modelo.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }

        sesion.setAttribute("usuario", usuario);
        return "redirect:/menu";
    }

    @GetMapping("/menu")
    public String mostrarMenu(HttpSession sesion, Model modelo) {
        Usuario usuario = (Usuario) sesion.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        modelo.addAttribute("usuario", usuario);
        return "menu";
    }

    @GetMapping("/salir")
    public String cerrarSesion(HttpSession sesion) {
        sesion.invalidate();
        return "redirect:/login";
    }
}
