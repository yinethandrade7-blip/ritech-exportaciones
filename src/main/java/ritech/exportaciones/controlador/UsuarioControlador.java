package ritech.exportaciones.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import ritech.exportaciones.dao.ClienteDAO;
import ritech.exportaciones.dao.ProveedorDAO;
import ritech.exportaciones.dao.UsuarioDAO;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class UsuarioControlador {

    @Autowired
    private UsuarioDAO usuarioDAO;

    @Autowired
    private ProveedorDAO proveedorDAO;

    @Autowired
    private ClienteDAO clienteDAO;

    @GetMapping("/usuarios")
    public String mostrar(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeAdministrarUsuarios()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("usuarios", usuarioDAO.listar());
        modelo.addAttribute("roles", usuarioDAO.listarRoles());
        modelo.addAttribute("proveedores", proveedorDAO.listar(""));
        modelo.addAttribute("clientes", clienteDAO.listar(""));

        return "usuarios";
    }

    @PostMapping("/usuarios/crear")
    public String crear(@RequestParam String nombre,
                        @RequestParam String correo,
                        @RequestParam String clave,
                        @RequestParam(defaultValue = "0") int idRol,
                        @RequestParam(defaultValue = "0") int idProveedor,
                        @RequestParam(defaultValue = "0") int idCliente,
                        HttpSession sesion,
                        RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!usuario.puedeAdministrarUsuarios()) {
            return "redirect:/menu";
        }

        nombre = nombre.trim();
        correo = correo.trim();

        if (nombre.isEmpty() || correo.isEmpty() || clave.isEmpty()) {
            flash.addFlashAttribute("error", "Nombre, correo y contraseña son obligatorios");
            return "redirect:/usuarios";
        }

        if (!correo.contains("@")) {
            flash.addFlashAttribute("error", "El correo no tiene un formato válido");
            return "redirect:/usuarios";
        }

        if (clave.length() < 4) {
            flash.addFlashAttribute("error", "La contraseña debe tener al menos 4 caracteres");
            return "redirect:/usuarios";
        }

        if (idRol == 0) {
            flash.addFlashAttribute("error", "Debe seleccionar un rol");
            return "redirect:/usuarios";
        }

        if (usuarioDAO.existeCorreo(correo)) {
            flash.addFlashAttribute("error", "Ya existe un usuario con el correo " + correo);
            return "redirect:/usuarios";
        }

        String rol = usuarioDAO.nombreRol(idRol);

        if ("PROVEEDOR".equals(rol) && idProveedor == 0) {
            flash.addFlashAttribute("error",
                    "Para un usuario PROVEEDOR debe indicar a cual proveedor pertenece");
            return "redirect:/usuarios";
        }

        if ("CLIENTE".equals(rol) && idCliente == 0) {
            flash.addFlashAttribute("error",
                    "Para un usuario CLIENTE debe indicar a cual empresa pertenece");
            return "redirect:/usuarios";
        }

        Integer vinculoProveedor = "PROVEEDOR".equals(rol) ? idProveedor : null;
        Integer vinculoCliente   = "CLIENTE".equals(rol)   ? idCliente   : null;

        usuarioDAO.crear(nombre, correo, clave, idRol, vinculoProveedor, vinculoCliente);

        flash.addFlashAttribute("mensaje", "Usuario " + nombre + " creado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/estado")
    public String cambiarEstado(@RequestParam int id,
                                @RequestParam boolean activo,
                                HttpSession sesion,
                                RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }
        if (!usuario.puedeAdministrarUsuarios()) {
            return "redirect:/menu";
        }

        if (usuario.getId() == id) {
            flash.addFlashAttribute("error", "No puede desactivar su propia cuenta");
            return "redirect:/usuarios";
        }

        usuarioDAO.cambiarEstado(id, activo);

        flash.addFlashAttribute("mensaje", activo ? "Usuario activado" : "Usuario desactivado");
        return "redirect:/usuarios";
    }
}
