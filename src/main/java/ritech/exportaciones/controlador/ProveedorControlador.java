package ritech.exportaciones.controlador;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ritech.exportaciones.dao.ProveedorDAO;
import ritech.exportaciones.modelo.Proveedor;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class ProveedorControlador {

    @Autowired
    private ProveedorDAO proveedorDAO;

    @GetMapping("/proveedores")
    public String mostrarProveedores(@RequestParam(required = false) String buscar,
                                     @RequestParam(required = false) Integer editar,
                                     HttpSession sesion,
                                     Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Antes entraba cualquiera que hubiera iniciado sesion,
        // incluido un cliente europeo.
        if (!usuario.puedeVerMaestros()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("proveedores", proveedorDAO.listar(buscar));
        modelo.addAttribute("buscar", buscar);

        // Solo tiene sentido cargar el formulario de edicion
        // si el usuario realmente puede editar.
        if (editar != null && usuario.puedeEditarMaestros()) {
            modelo.addAttribute("proveedorEditar", proveedorDAO.buscarPorId(editar));
        }

        return "proveedores";
    }

    @PostMapping("/proveedores")
    public String guardarProveedor(@RequestParam(required = false) String id,
                                   @RequestParam String documento,
                                   @RequestParam String nombre,
                                   @RequestParam(required = false) String telefono,
                                   @RequestParam(required = false) String correo,
                                   @RequestParam String nombreFinca,
                                   @RequestParam(required = false) String lugar,
                                   @RequestParam(required = false) String latitud,
                                   @RequestParam(required = false) String longitud,
                                   HttpSession sesion,
                                   Model modelo,
                                   RedirectAttributes redirect) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeEditarMaestros()) {
            redirect.addFlashAttribute("error",
                "No tiene permiso para crear o modificar proveedores");
            return "redirect:/proveedores";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("proveedores", proveedorDAO.listar(""));

        boolean esEdicion = (id != null && !id.isBlank());

        if (documento.isBlank() || nombre.isBlank() || nombreFinca.isBlank()) {
            modelo.addAttribute("error",
                "El documento, el nombre y el nombre de la finca son obligatorios");
            return "proveedores";
        }

        if (latitud == null || latitud.isBlank() || longitud == null || longitud.isBlank()) {
            modelo.addAttribute("error",
                "Las coordenadas de la finca son obligatorias. "
              + "Sin ellas el producto no puede exportarse a la Unión Europea.");
            return "proveedores";
        }

        double lat;
        double lon;
        try {
            lat = Double.parseDouble(latitud.trim().replace(",", "."));
            lon = Double.parseDouble(longitud.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            modelo.addAttribute("error",
                "La latitud y la longitud deben ser numeros. Ejemplo: 1.214500 y -77.278400");
            return "proveedores";
        }

        Proveedor p = new Proveedor();
        p.setDocumento(documento.trim());
        p.setNombre(nombre.trim());
        p.setTelefono(telefono);
        p.setCorreo(correo);
        p.setNombreFinca(nombreFinca.trim());
        p.setLugar(lugar);
        p.setLatitud(lat);
        p.setLongitud(lon);

        if (esEdicion) {

            int idProveedor = Integer.parseInt(id);

            if (proveedorDAO.existeDocumentoEnOtro(documento.trim(), idProveedor)) {
                modelo.addAttribute("error", "Ya existe otro proveedor con ese documento");
                return "proveedores";
            }

            p.setId(idProveedor);
            proveedorDAO.actualizar(p);
            redirect.addFlashAttribute("mensaje", "Proveedor actualizado correctamente");

        } else {

            if (proveedorDAO.existeDocumento(documento.trim())) {
                modelo.addAttribute("error", "El proveedor ya existe");
                return "proveedores";
            }

            proveedorDAO.guardar(p);
            redirect.addFlashAttribute("mensaje", "Proveedor guardado correctamente");
        }

        return "redirect:/proveedores";
    }

    @GetMapping("/proveedores/eliminar")
    public String eliminarProveedor(@RequestParam int id,
                                    HttpSession sesion,
                                    RedirectAttributes redirect) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Este era el agujero mas grave: cualquier usuario con
        // sesion abierta podia borrar un proveedor escribiendo
        // la direccion a mano en el navegador.
        if (!usuario.puedeEditarMaestros()) {
            redirect.addFlashAttribute("error",
                "No tiene permiso para eliminar proveedores");
            return "redirect:/proveedores";
        }

        if (proveedorDAO.tieneLotes(id)) {
            redirect.addFlashAttribute("error",
                "No se puede eliminar: el proveedor tiene lotes registrados.");
            return "redirect:/proveedores";
        }

        proveedorDAO.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Proveedor eliminado");

        return "redirect:/proveedores";
    }
}
