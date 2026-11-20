package ritech.exportaciones.controlador;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import ritech.exportaciones.dao.ClienteDAO;
import ritech.exportaciones.dao.PedidoDAO;
import ritech.exportaciones.modelo.Pedido;
import ritech.exportaciones.modelo.TasaCambio;
import ritech.exportaciones.modelo.Usuario;
import ritech.exportaciones.servicio.AvisoServicio;
import ritech.exportaciones.servicio.TasaCambioServicio;

@Controller
public class PedidoControlador {

    @Autowired
    private PedidoDAO pedidoDAO;

    @Autowired
    private ClienteDAO clienteDAO;

    @Autowired
    private TasaCambioServicio tasaCambioServicio;

    @Autowired
    private AvisoServicio avisoServicio;

    /**
     * Un pedido solo se puede modificar mientras el cliente no lo haya visto.
     *
     * BORRADOR   lo esta armando el auxiliar
     * SOLICITADO lo pidio el cliente y falta ponerle precio
     *
     * Desde que sale la cotizacion queda congelado. Antes esto dejaba
     * modificar un pedido en CREADO, que es el que el cliente YA acepto:
     * se le podia agregar un lote despues de aprobado y despacharle algo
     * distinto de lo que aprobo.
     */
    private boolean esModificable(Pedido p) {
        return "BORRADOR".equals(p.getEstado()) || "SOLICITADO".equals(p.getEstado());
    }

    // ----------------------------------------------------------
    // Lista de pedidos
    // ----------------------------------------------------------

    @GetMapping("/pedidos")
    public String mostrar(HttpSession sesion, Model modelo) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Ver pedidos: gerente (para autorizar) y auxiliar (para operar).
        if (!usuario.puedeVerPedidos()) {
            return "redirect:/menu";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("pedidos", pedidoDAO.listar());
        modelo.addAttribute("clientes", clienteDAO.listar(""));
        modelo.addAttribute("paises", clienteDAO.listarPaises());
        modelo.addAttribute("monedas", clienteDAO.listarMonedas());
        modelo.addAttribute("hoy", LocalDate.now().toString());
        modelo.addAttribute("tasa", tasaCambioServicio.consultar());

        return "pedidos";
    }

    @PostMapping("/pedidos/crear")
    public String crear(@RequestParam(defaultValue = "0") int idCliente,
                        @RequestParam(defaultValue = "0") int idPaisDestino,
                        @RequestParam(defaultValue = "0") int idMoneda,
                        @RequestParam(defaultValue = "") String tasaCambio,
                        @RequestParam(defaultValue = "") String fechaPedido,
                        HttpSession sesion,
                        RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeOperarPedidos()) {
            flash.addFlashAttribute("error", "No tiene permiso para crear pedidos");
            return "redirect:/pedidos";
        }

        if (idCliente == 0) {
            flash.addFlashAttribute("error", "Debe seleccionar el cliente");
            return "redirect:/pedidos";
        }
        if (idPaisDestino == 0) {
            flash.addFlashAttribute("error", "Debe seleccionar el pais de destino");
            return "redirect:/pedidos";
        }
        if (idMoneda == 0) {
            flash.addFlashAttribute("error", "Debe seleccionar la moneda");
            return "redirect:/pedidos";
        }
        if (fechaPedido.trim().isEmpty()) {
            flash.addFlashAttribute("error", "Debe indicar la fecha del pedido");
            return "redirect:/pedidos";
        }

        double tasa;
        try {
            tasa = Double.parseDouble(tasaCambio.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            flash.addFlashAttribute("error", "La tasa de cambio debe ser un número");
            return "redirect:/pedidos";
        }
        if (tasa <= 0) {
            flash.addFlashAttribute("error", "La tasa de cambio debe ser mayor que cero");
            return "redirect:/pedidos";
        }

        Pedido p = new Pedido();
        p.setIdCliente(idCliente);
        p.setIdPaisDestino(idPaisDestino);
        p.setIdMoneda(idMoneda);
        p.setTasaCambio(tasa);
        p.setFechaPedido(fechaPedido.trim());
        p.setIdUsuario(usuario.getId());

        String numero = pedidoDAO.crear(p);

        flash.addFlashAttribute("mensaje",
                "Pedido " + numero + " creado. Ahora debe agregarle los lotes.");
        return "redirect:/pedidos";
    }

    // ----------------------------------------------------------
    // Detalle
    // ----------------------------------------------------------

    @GetMapping("/pedidos/detalle")
    public String detalle(@RequestParam int id,
                          HttpSession sesion,
                          Model modelo,
                          RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeVerPedidos()) {
            return "redirect:/menu";
        }

        Pedido pedido = pedidoDAO.buscarPorId(id);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe");
            return "redirect:/pedidos";
        }

        modelo.addAttribute("usuario", usuario);
        modelo.addAttribute("pedido", pedido);
        modelo.addAttribute("lotes", pedidoDAO.lotesDelPedido(id));
        modelo.addAttribute("disponibles", pedidoDAO.lotesDisponibles());
        modelo.addAttribute("sinCoordenadas", pedidoDAO.lotesSinCoordenadas(id));
        modelo.addAttribute("cantidadLotes", pedidoDAO.cantidadLotes(id));

        // Tasa de cambio del dia, ya convertida a la moneda de este pedido
        TasaCambio tasa = tasaCambioServicio.consultar();
        modelo.addAttribute("tasa", tasa);
        modelo.addAttribute("tasaSugerida", tasa.paraMoneda(pedido.getMoneda()));

        return "detalle";
    }

    @PostMapping("/pedidos/agregar-lote")
    public String agregarLote(@RequestParam int idPedido,
                              @RequestParam(defaultValue = "0") int idLote,
                              @RequestParam(defaultValue = "") String precioUnitario,
                              HttpSession sesion,
                              RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeOperarPedidos()) {
            flash.addFlashAttribute("error", "No tiene permiso para modificar pedidos");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe");
            return "redirect:/pedidos";
        }
        if (!esModificable(pedido)) {
            flash.addFlashAttribute("error",
                    "El pedido ya fue despachado y no se puede modificar");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }
        if (idLote == 0) {
            flash.addFlashAttribute("error", "Debe seleccionar un lote");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioUnitario.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            flash.addFlashAttribute("error", "El precio debe ser un número");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }
        if (precio <= 0) {
            flash.addFlashAttribute("error", "El precio debe ser mayor que cero");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        pedidoDAO.agregarLote(idPedido, idLote, precio);

        flash.addFlashAttribute("mensaje", "Lote agregado al pedido");
        return "redirect:/pedidos/detalle?id=" + idPedido;
    }

    @PostMapping("/pedidos/quitar-lote")
    public String quitarLote(@RequestParam int idPedidoLote,
                             @RequestParam int idPedido,
                             HttpSession sesion,
                             RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeOperarPedidos()) {
            flash.addFlashAttribute("error", "No tiene permiso para modificar pedidos");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido != null && !esModificable(pedido)) {
            flash.addFlashAttribute("error",
                    "El pedido ya fue despachado y no se puede modificar");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        pedidoDAO.quitarLote(idPedidoLote, idPedido);

        flash.addFlashAttribute("mensaje", "Lote retirado del pedido");
        return "redirect:/pedidos/detalle?id=" + idPedido;
    }

    // ----------------------------------------------------------
    // Solicitudes enviadas por el cliente
    // ----------------------------------------------------------

    @PostMapping("/pedidos/confirmar")
    public String confirmar(@RequestParam int idPedido,
                            @RequestParam(defaultValue = "") String tasaCambio,
                            @RequestParam(name = "idPedidoLote", required = false) int[] idPedidoLote,
                            @RequestParam(name = "precio", required = false) String[] precio,
                            HttpSession sesion,
                            RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        // Fijar precios y tasa de cambio es trabajo comercial del auxiliar.
        if (!usuario.puedeOperarPedidos()) {
            flash.addFlashAttribute("error",
                    "No tiene permiso para confirmar solicitudes");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe");
            return "redirect:/pedidos";
        }
        if (!"SOLICITADO".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "Este pedido ya fue confirmado");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        double tasa;
        try {
            tasa = Double.parseDouble(tasaCambio.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            flash.addFlashAttribute("error", "La tasa de cambio debe ser un número");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }
        if (tasa <= 0) {
            flash.addFlashAttribute("error", "La tasa de cambio debe ser mayor que cero");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        if (idPedidoLote == null || precio == null || idPedidoLote.length != precio.length) {
            flash.addFlashAttribute("error", "No se recibieron los precios de los lotes");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        // Primero se validan todos los precios, y solo despues se guarda alguno
        double[] valores = new double[precio.length];
        for (int i = 0; i < precio.length; i++) {
            try {
                valores[i] = Double.parseDouble(precio[i].trim().replace(",", "."));
            } catch (NumberFormatException e) {
                flash.addFlashAttribute("error",
                        "Hay un precio que no es un número válido");
                return "redirect:/pedidos/detalle?id=" + idPedido;
            }
            if (valores[i] <= 0) {
                flash.addFlashAttribute("error",
                        "Todos los precios deben ser mayores que cero");
                return "redirect:/pedidos/detalle?id=" + idPedido;
            }
        }

        for (int i = 0; i < idPedidoLote.length; i++) {
            pedidoDAO.fijarPrecio(idPedidoLote[i], valores[i]);
        }

        pedidoDAO.enviarCotizacion(idPedido, tasa);

        // El pedido se vuelve a leer: ya trae el total recalculado,
        // que es la cifra que va en el correo.
        avisoServicio.cotizacionEnviada(pedidoDAO.buscarPorId(idPedido));

        flash.addFlashAttribute("mensaje",
                "Cotización del pedido " + pedido.getNumero() + " enviada al cliente. "
                + "Queda a la espera de que el cliente acepte o rechace el precio.");
        return "redirect:/pedidos/detalle?id=" + idPedido;
    }

    // ----------------------------------------------------------
    // El auxiliar manda al cliente un pedido que armo el mismo
    // ----------------------------------------------------------

    @PostMapping("/pedidos/enviar")
    public String enviar(@RequestParam int idPedido,
                         HttpSession sesion,
                         RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeOperarPedidos()) {
            flash.addFlashAttribute("error", "No tiene permiso para enviar cotizaciones");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe");
            return "redirect:/pedidos";
        }

        if (pedidoDAO.cantidadLotes(idPedido) == 0) {
            flash.addFlashAttribute("error",
                    "Agregue al menos un lote antes de enviar la cotización");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        if (pedidoDAO.enviarBorrador(idPedido)) {

            avisoServicio.cotizacionEnviada(pedidoDAO.buscarPorId(idPedido));

            flash.addFlashAttribute("mensaje",
                    "Cotización del pedido " + pedido.getNumero() + " enviada al cliente. "
                    + "A partir de ahora el pedido no se puede modificar.");
        } else {
            flash.addFlashAttribute("error",
                    "Ese pedido ya fue enviado al cliente");
        }

        return "redirect:/pedidos/detalle?id=" + idPedido;
    }

    @PostMapping("/pedidos/rechazar")
    public String rechazar(@RequestParam int idPedido,
                           HttpSession sesion,
                           RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeOperarPedidos()) {
            flash.addFlashAttribute("error",
                    "No tiene permiso para rechazar solicitudes");
            return "redirect:/pedidos";
        }

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null || !"SOLICITADO".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error", "Solo se puede rechazar una solicitud pendiente");
            return "redirect:/pedidos";
        }

        pedidoDAO.rechazarSolicitud(idPedido);

        flash.addFlashAttribute("mensaje",
                "Solicitud rechazada. Los lotes volvieron al inventario disponible.");
        return "redirect:/pedidos";
    }

    // ----------------------------------------------------------
    // Despacho con verificacion EUDR
    //
    // Despachar compromete legalmente a RiTech ante la Union
    // Europea. Por eso lo autoriza la gerencia, no la operacion.
    // ----------------------------------------------------------

    @PostMapping("/pedidos/despachar")
    public String despachar(@RequestParam int idPedido,
                            HttpSession sesion,
                            RedirectAttributes flash) {

        Usuario usuario = (Usuario) sesion.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (!usuario.puedeDespachar()) {
            flash.addFlashAttribute("error",
                    "Solo la gerencia puede autorizar el despacho de un pedido");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null) {
            flash.addFlashAttribute("error", "El pedido no existe");
            return "redirect:/pedidos";
        }
        if (!"CREADO".equals(pedido.getEstado())) {
            flash.addFlashAttribute("error",
                    "Solo se puede despachar un pedido confirmado");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }
        if (pedidoDAO.cantidadLotes(idPedido) == 0) {
            flash.addFlashAttribute("error", "No se puede despachar un pedido sin lotes");
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        // Verificacion EUDR: se vuelve a hacer aqui, no se confia en la pantalla
        List<String> sinCoordenadas = pedidoDAO.lotesSinCoordenadas(idPedido);

        if (!sinCoordenadas.isEmpty()) {
            flash.addFlashAttribute("error",
                    "DESPACHO BLOQUEADO por el Reglamento (UE) 2023/1115. "
                    + "Los siguientes lotes no tienen coordenadas de origen: "
                    + String.join(", ", sinCoordenadas));
            return "redirect:/pedidos/detalle?id=" + idPedido;
        }

        pedidoDAO.despachar(idPedido);

        flash.addFlashAttribute("mensaje",
                "Pedido " + pedido.getNumero() + " despachado por "
                + usuario.getNombre() + ". Origen verificado para todos los lotes.");
        return "redirect:/pedidos/detalle?id=" + idPedido;
    }
}
