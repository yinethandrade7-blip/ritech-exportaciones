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
import ritech.exportaciones.modelo.Cliente;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class ClienteControlador {

    @Autowired
    private ClienteDAO clienteDAO;

    // ----------------------------------------------------------
    // Mostrar la pantalla
    // ----------------------------------------------------------

    @GetMapping("/clientes")
    public String mostrar(@RequestParam(defaultValue = "") String filtro,
                          @RequestParam(defaultValue = "0") int editar,
                          HttpSession sesion,
                          Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeVerMaestros()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("filtro", filtro);
        modelo.addAttribute("clientes", clienteDAO.listar(filtro));
        modelo.addAttribute("paises", clienteDAO.listarPaises());
        modelo.addAttribute("monedas", clienteDAO.listarMonedas());

        if (editar > 0 && usuario.puedeEditarMaestros()) {
            modelo.addAttribute("cliente", clienteDAO.buscarPorId(editar));
        }

        return "clientes";
    }

    // ----------------------------------------------------------
    // Guardar un cliente nuevo
    // ----------------------------------------------------------

    @PostMapping("/clientes/guardar")
    public String guardar(@RequestParam String nombreEmpresa,
                          @RequestParam(defaultValue = "0") int idPais,
                          @RequestParam(defaultValue = "") String ciudad,
                          @RequestParam(defaultValue = "") String personaContacto,
                          @RequestParam(defaultValue = "") String correo,
                          @RequestParam(defaultValue = "0") int idMoneda,
                          HttpSession sesion,
                          RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeEditarMaestros()) {
            flash.addFlashAttribute("error",
                    "No tiene permiso para registrar clientes");
            return "redirect:/clientes";
        }

        String error = validar(nombreEmpresa, idPais, correo);
        if (error != null) {
            flash.addFlashAttribute("error", error);
            return "redirect:/clientes";
        }

        if (clienteDAO.existeEmpresa(nombreEmpresa.trim())) {
            flash.addFlashAttribute("error",
                    "Ya existe un cliente registrado con el nombre " + nombreEmpresa.trim());
            return "redirect:/clientes";
        }

        Cliente c = new Cliente();
        c.setNombreEmpresa(nombreEmpresa.trim());
        c.setIdPais(idPais);
        c.setCiudad(ciudad.trim());
        c.setPersonaContacto(personaContacto.trim());
        c.setCorreo(correo.trim());
        c.setIdMoneda(idMoneda == 0 ? null : idMoneda);

        clienteDAO.guardar(c);

        flash.addFlashAttribute("mensaje",
                "Cliente " + c.getNombreEmpresa() + " registrado correctamente");
        return "redirect:/clientes";
    }

    // ----------------------------------------------------------
    // Actualizar un cliente existente
    // ----------------------------------------------------------

    @PostMapping("/clientes/actualizar")
    public String actualizar(@RequestParam int id,
                             @RequestParam String nombreEmpresa,
                             @RequestParam(defaultValue = "0") int idPais,
                             @RequestParam(defaultValue = "") String ciudad,
                             @RequestParam(defaultValue = "") String personaContacto,
                             @RequestParam(defaultValue = "") String correo,
                             @RequestParam(defaultValue = "0") int idMoneda,
                             HttpSession sesion,
                             RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeEditarMaestros()) {
            flash.addFlashAttribute("error",
                    "No tiene permiso para modificar clientes");
            return "redirect:/clientes";
        }

        String error = validar(nombreEmpresa, idPais, correo);
        if (error != null) {
            flash.addFlashAttribute("error", error);
            return "redirect:/clientes?editar=" + id;
        }

        if (clienteDAO.existeEmpresaEnOtro(nombreEmpresa.trim(), id)) {
            flash.addFlashAttribute("error",
                    "Otro cliente ya usa el nombre " + nombreEmpresa.trim());
            return "redirect:/clientes?editar=" + id;
        }

        Cliente c = new Cliente();
        c.setId(id);
        c.setNombreEmpresa(nombreEmpresa.trim());
        c.setIdPais(idPais);
        c.setCiudad(ciudad.trim());
        c.setPersonaContacto(personaContacto.trim());
        c.setCorreo(correo.trim());
        c.setIdMoneda(idMoneda == 0 ? null : idMoneda);

        clienteDAO.actualizar(c);

        flash.addFlashAttribute("mensaje",
                "Cliente " + c.getNombreEmpresa() + " actualizado correctamente");
        return "redirect:/clientes";
    }

    // ----------------------------------------------------------
    // Eliminar
    // ----------------------------------------------------------

    @PostMapping("/clientes/eliminar")
    public String eliminar(@RequestParam int id,
                           HttpSession sesion,
                           RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeEditarMaestros()) {
            flash.addFlashAttribute("error",
                    "No tiene permiso para eliminar clientes");
            return "redirect:/clientes";
        }

        if (clienteDAO.tienePedidos(id)) {
            flash.addFlashAttribute("error",
                    "No se puede eliminar: el cliente tiene pedidos registrados");
            return "redirect:/clientes";
        }

        if (clienteDAO.tieneUsuario(id)) {
            flash.addFlashAttribute("error",
                    "No se puede eliminar: el cliente tiene una cuenta de acceso creada");
            return "redirect:/clientes";
        }

        clienteDAO.eliminar(id);

        flash.addFlashAttribute("mensaje", "Cliente eliminado");
        return "redirect:/clientes";
    }

    // ----------------------------------------------------------
    // Validacion compartida
    // ----------------------------------------------------------

    private String validar(String nombreEmpresa, int idPais, String correo) {

        if (nombreEmpresa == null || nombreEmpresa.trim().isEmpty()) {
            return "El nombre de la empresa es obligatorio";
        }

        if (idPais == 0) {
            return "Debe seleccionar el pais de destino";
        }

        if (!correo.trim().isEmpty() && !correo.contains("@")) {
            return "El correo no tiene un formato válido";
        }

        return null;
    }
}
