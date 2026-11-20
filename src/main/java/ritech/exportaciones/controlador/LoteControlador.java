package ritech.exportaciones.controlador;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ritech.exportaciones.dao.LoteDAO;
import ritech.exportaciones.dao.ProveedorDAO;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class LoteControlador {

    @Autowired
    private LoteDAO loteDAO;

    @Autowired
    private ProveedorDAO proveedorDAO;

    @GetMapping("/lotes")
    public String mostrarLotes(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Ver el inventario: gerente y auxiliar.
        // El administrador no entra aqui: el no maneja el negocio.
        if (!usuario.puedeVerLotes()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("lotes", loteDAO.listar());
        modelo.addAttribute("variedades", loteDAO.listarVariedades());
        modelo.addAttribute("proveedores", proveedorDAO.listar(""));

        return "lotes";
    }

    @PostMapping("/lotes")
    public String registrarLote(@RequestParam(required = false) String idProveedor,
                                @RequestParam(required = false) String idVariedad,
                                @RequestParam(required = false) String peso,
                                @RequestParam(required = false) String fechaCosecha,
                                @RequestParam(required = false) String precioCompra,
                                HttpSession sesion,
                                Model modelo,
                                RedirectAttributes redirect) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Registrar lotes es operacion: solo el auxiliar.
        // Esta revision no sobra aunque la pantalla esconda el
        // formulario: la direccion se puede escribir a mano.
        if (!usuario.puedeOperarLotes()) {
            redirect.addFlashAttribute("error",
                "No tiene permiso para registrar lotes");
            return "redirect:/lotes";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("lotes", loteDAO.listar());
        modelo.addAttribute("variedades", loteDAO.listarVariedades());
        modelo.addAttribute("proveedores", proveedorDAO.listar(""));

        if (idProveedor == null || idProveedor.isBlank()
         || idVariedad == null || idVariedad.isBlank()) {
            modelo.addAttribute("error", "Debe seleccionar el proveedor y la variedad");
            return "lotes";
        }

        double pesoKg;
        try {
            pesoKg = Double.parseDouble(peso.trim().replace(",", "."));
        } catch (Exception e) {
            modelo.addAttribute("error", "El peso debe ser un número");
            return "lotes";
        }

        if (pesoKg <= 0) {
            modelo.addAttribute("error", "El peso debe ser mayor que cero");
            return "lotes";
        }

        if (fechaCosecha == null || !fechaCosecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
            modelo.addAttribute("error",
                "La fecha debe tener el formato aaaa-mm-dd. Ejemplo: 2026-09-15");
            return "lotes";
        }

        // Precio que RiTech le paga al productor por kilo, en pesos.
        // Es obligatorio: no se recibe cafe sin saber cuanto se le
        // va a pagar a quien lo cultivo.
        double precioCompraKg;
        try {
            precioCompraKg = Double.parseDouble(precioCompra.trim().replace(",", "."));
        } catch (Exception e) {
            modelo.addAttribute("error",
                "El precio de compra por kilo debe ser un número en pesos");
            return "lotes";
        }

        if (precioCompraKg <= 0) {
            modelo.addAttribute("error",
                "El precio de compra por kilo debe ser mayor que cero");
            return "lotes";
        }

        Integer idFinca = loteDAO.idFincaDe(Integer.parseInt(idProveedor));

        if (idFinca == null) {
            modelo.addAttribute("error",
                "El proveedor seleccionado no tiene finca registrada. "
              + "El lote no podría exportarse a la Unión Europea.");
            return "lotes";
        }

        String codigo = loteDAO.registrar(Integer.parseInt(idVariedad),
                                          idFinca,
                                          pesoKg,
                                          fechaCosecha,
                                          precioCompraKg);

        redirect.addFlashAttribute("mensaje", "Lote registrado con el código " + codigo);

        return "redirect:/lotes";
    }
}
