package ritech.exportaciones.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import ritech.exportaciones.dao.PagoDAO;
import ritech.exportaciones.modelo.Lote;
import ritech.exportaciones.modelo.Usuario;
import ritech.exportaciones.servicio.AvisoServicio;

/**
 * Pagos a los productores.
 *
 * Esta pantalla es solo de la gerencia: muestra cuanta plata le
 * debe RiTech a cada caficultor y permite registrar los pagos.
 */
@Controller
public class PagoControlador {

    @Autowired
    private PagoDAO pagoDAO;

    @Autowired
    private AvisoServicio avisoServicio;

    @GetMapping("/pagos")
    public String mostrar(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeAutorizarPagos()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("deudas", pagoDAO.deudaPorProveedor());
        modelo.addAttribute("pendientes", pagoDAO.lotesPendientes());
        modelo.addAttribute("pagados", pagoDAO.lotesPagados());
        modelo.addAttribute("totalPendiente", pagoDAO.totalPendiente());
        modelo.addAttribute("totalPagado", pagoDAO.totalPagado());
        modelo.addAttribute("lotesSinPrecio", pagoDAO.lotesSinPrecio());

        return "pagos";
    }

    @PostMapping("/pagos/registrar")
    public String registrar(@RequestParam int idLote,
                            HttpSession sesion,
                            RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // La revision va aqui tambien, no solo en la pantalla:
        // el numero del lote viaja en el formulario y se puede cambiar.
        if (!usuario.puedeAutorizarPagos()) {
            flash.addFlashAttribute("error",
                    "Solo la gerencia puede registrar pagos a los productores");
            return "redirect:/menu";
        }

        String codigo = pagoDAO.codigoDe(idLote);

        if (codigo == null) {
            flash.addFlashAttribute("error", "Ese lote no existe");
            return "redirect:/pagos";
        }

        if (pagoDAO.registrarPago(idLote)) {

            flash.addFlashAttribute("mensaje",
                    "Pago del lote " + codigo + " registrado por " + usuario.getNombre());

            // Al productor se le avisa cuanta plata le va a llegar.
            // Si el correo falla, el pago igual quedo registrado.
            Lote lote = pagoDAO.buscarLote(idLote);
            if (lote != null) {
                avisoServicio.pagoAlProveedor(lote);
            }

        } else {
            flash.addFlashAttribute("error",
                    "El lote " + codigo + " ya estaba pagado o no tiene precio de compra");
        }

        return "redirect:/pagos";
    }
}
