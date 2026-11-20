package ritech.exportaciones.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import ritech.exportaciones.dao.PedidoDAO;
import ritech.exportaciones.dao.PortalDAO;
import ritech.exportaciones.modelo.Pedido;
import ritech.exportaciones.modelo.Usuario;

@Controller
public class PortalControlador {

    @Autowired
    private PortalDAO portalDAO;

    @Autowired
    private PedidoDAO pedidoDAO;

    // ----------------------------------------------------------
    // Portal del proveedor
    // ----------------------------------------------------------

    @GetMapping("/mis-lotes")
    public String misLotes(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esProveedor()) {
            return "redirect:/menu";
        }

        if (usuario.getIdProveedor() == null) {
            modelo.addAttribute("usuario", usuario);
            modelo.addAttribute("sinVinculo", true);
            return "mis-lotes";
        }

        int idProveedor = usuario.getIdProveedor();

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("sinVinculo", false);
        modelo.addAttribute("lotes", portalDAO.lotesDelProveedor(idProveedor));
        modelo.addAttribute("kilosTotales", portalDAO.kilosTotales(idProveedor));
        modelo.addAttribute("kilosExportados", portalDAO.kilosExportados(idProveedor));
        modelo.addAttribute("porPagar", portalDAO.totalPorPagar(idProveedor));
        modelo.addAttribute("pagado", portalDAO.totalPagado(idProveedor));

        return "mis-lotes";
    }

    // ----------------------------------------------------------
    // Portal del cliente: inventario y solicitud
    // ----------------------------------------------------------

    @GetMapping("/mi-inventario")
    public String miInventario(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esCliente()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("lotes", portalDAO.inventarioDisponible());
        modelo.addAttribute("sinVinculo", usuario.getIdCliente() == null);

        return "mi-inventario";
    }

    @PostMapping("/mi-inventario/solicitar")
    public String solicitar(@RequestParam(name = "idLote", required = false) int[] idLote,
                            HttpSession sesion,
                            RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esCliente() || usuario.getIdCliente() == null) {
            return "redirect:/menu";
        }

        if (idLote == null || idLote.length == 0) {
            flash.addFlashAttribute("error", "Debe marcar al menos un lote");
            return "redirect:/mi-inventario";
        }

        // Alguien pudo haber solicitado el mismo lote mientras la pantalla estaba abierta
        for (int id : idLote) {
            if (!portalDAO.loteSigueDisponible(id)) {
                flash.addFlashAttribute("error",
                        "Uno de los lotes que selecciono ya no esta disponible. "
                        + "Revise el inventario e intente de nuevo.");
                return "redirect:/mi-inventario";
            }
        }

        String numero = portalDAO.crearSolicitud(
                usuario.getIdCliente(), usuario.getId(), idLote);

        flash.addFlashAttribute("mensaje",
                "Solicitud " + numero + " enviada. RiTech S.A.S le confirmara el precio.");
        return "redirect:/mis-pedidos";
    }

    // ----------------------------------------------------------
    // Portal del cliente: pedidos
    // ----------------------------------------------------------

    @GetMapping("/mis-pedidos")
    public String misPedidos(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esCliente()) {
            return "redirect:/menu";
        }

        if (usuario.getIdCliente() == null) {
            modelo.addAttribute("usuario", usuario);
            modelo.addAttribute("sinVinculo", true);
            return "mis-pedidos";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("sinVinculo", false);
        modelo.addAttribute("pedidos", portalDAO.pedidosDelCliente(usuario.getIdCliente()));

        return "mis-pedidos";
    }

    @GetMapping("/mi-pedido")
    public String miPedido(@RequestParam int id,
                           HttpSession sesion,
                           Model modelo,
                           RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esCliente() || usuario.getIdCliente() == null) {
            return "redirect:/menu";
        }

        if (!portalDAO.pedidoEsDelCliente(id, usuario.getIdCliente())) {
            flash.addFlashAttribute("error", "Ese pedido no pertenece a su empresa");
            return "redirect:/mis-pedidos";
        }

        Pedido pedido = pedidoDAO.buscarPorId(id);

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("pedido", pedido);
        modelo.addAttribute("lotes", pedidoDAO.lotesDelPedido(id));

        return "mi-pedido";
    }

    // ----------------------------------------------------------
    // El cliente responde la cotizacion
    //
    // Las dos operaciones revisan primero que el pedido sea de la
    // empresa de quien esta conectado. El numero del pedido viaja
    // en el formulario, y cualquiera podria cambiarlo a mano.
    // ----------------------------------------------------------

    @PostMapping("/mi-pedido/aceptar")
    public String aceptar(@RequestParam int id,
                          HttpSession sesion,
                          RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esCliente() || usuario.getIdCliente() == null) {
            return "redirect:/menu";
        }

        if (!portalDAO.pedidoEsDelCliente(id, usuario.getIdCliente())) {
            flash.addFlashAttribute("error", "Ese pedido no pertenece a su empresa");
            return "redirect:/mis-pedidos";
        }

        if (portalDAO.aceptarCotizacion(id)) {
            flash.addFlashAttribute("mensaje",
                    "Precio aceptado. RiTech S.A.S procedera con el despacho.");
        } else {
            flash.addFlashAttribute("error",
                    "Ese pedido ya no esta esperando respuesta.");
        }

        return "redirect:/mi-pedido?id=" + id;
    }

    @PostMapping("/mi-pedido/rechazar")
    public String rechazar(@RequestParam int id,
                           @RequestParam(defaultValue = "") String motivo,
                           HttpSession sesion,
                           RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.esCliente() || usuario.getIdCliente() == null) {
            return "redirect:/menu";
        }

        if (!portalDAO.pedidoEsDelCliente(id, usuario.getIdCliente())) {
            flash.addFlashAttribute("error", "Ese pedido no pertenece a su empresa");
            return "redirect:/mis-pedidos";
        }

        motivo = motivo.trim();

        if (motivo.isEmpty()) {
            flash.addFlashAttribute("error",
                    "Indique por que no acepta el precio, para que RiTech pueda revisarlo");
            return "redirect:/mi-pedido?id=" + id;
        }

        if (motivo.length() > 255) {
            motivo = motivo.substring(0, 255);
        }

        if (portalDAO.rechazarCotizacion(id, motivo)) {
            flash.addFlashAttribute("mensaje",
                    "Cotización rechazada. Los lotes volvieron al inventario disponible.");
        } else {
            flash.addFlashAttribute("error",
                    "Ese pedido ya no esta esperando respuesta.");
        }

        return "redirect:/mi-pedido?id=" + id;
    }
}
