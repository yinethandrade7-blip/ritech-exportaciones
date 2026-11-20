package ritech.exportaciones.servicio;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ritech.exportaciones.dao.AvisoDAO;
import ritech.exportaciones.modelo.Lote;
import ritech.exportaciones.modelo.Pedido;

/**
 * Los avisos del negocio.
 *
 * Aqui se decide A QUIEN se le avisa y QUE dice el correo. Lo de
 * conectarse al servidor de correo es trabajo de CorreoServicio.
 *
 * Regla de oro de esta clase: un aviso que falla NO puede tumbar la
 * operacion. Si el pedido se creo bien, el pedido se creo bien,
 * aunque el correo no haya salido. Por eso todo va dentro de un
 * try y el aviso queda guardado con el motivo de la falla.
 */
@Service
public class AvisoServicio {

    @Autowired
    private AvisoDAO avisoDAO;

    @Autowired
    private CorreoServicio correoServicio;

    private static final String FIRMA =
        "\n\n--\nRiTech S.A.S\nExportación de café y cacao de Nariño hacia la Unión Europea\n"
      + "Este es un aviso automático del sistema, no responda a este correo.";

    // ----------------------------------------------------------
    // 1. El cliente pide lotes  ->  se le avisa al auxiliar
    // ----------------------------------------------------------
    public void nuevaSolicitud(String numero, int idCliente, int cuantosLotes) {

        String empresa = avisoDAO.empresaDelCliente(idCliente);

        String asunto = "Nueva solicitud " + numero + " de " + empresa;

        String cuerpo =
            "La empresa " + empresa + " acaba de solicitar " + cuantosLotes
          + (cuantosLotes == 1 ? " lote." : " lotes.") + "\n\n"
          + "Solicitud: " + numero + "\n\n"
          + "Entre al sistema, revise los lotes y mándele la cotización."
          + FIRMA;

        for (String auxiliar : avisoDAO.correosDeAuxiliares()) {
            mandar(auxiliar, asunto, cuerpo);
        }
    }

    // ----------------------------------------------------------
    // 2. El auxiliar manda la cotizacion  ->  se le avisa al cliente
    // ----------------------------------------------------------
    public void cotizacionEnviada(Pedido pedido) {

        String correo = avisoDAO.correoDelCliente(pedido.getId());

        if (correo == null || correo.isBlank()) {
            return;
        }

        String asunto = "Cotización " + pedido.getNumero() + " - RiTech S.A.S";

        String cuerpo =
            "Su solicitud " + pedido.getNumero() + " ya tiene precio.\n\n"
          + "Total: " + plata(pedido.getTotal()) + " " + pedido.getMoneda() + "\n"
          + "Destino: " + pedido.getPaisDestino() + "\n"
          + "Tasa de cambio aplicada: " + plata(pedido.getTasaCambio())
          + " pesos por " + pedido.getMoneda() + "\n\n"
          + "Entre al sistema para aceptar o rechazar el precio. Si no está de "
          + "acuerdo puede indicar el motivo y lo revisamos."
          + FIRMA;

        mandar(correo, asunto, cuerpo);
    }

    // ----------------------------------------------------------
    // 3. El cliente responde  ->  se le avisa al auxiliar
    // ----------------------------------------------------------
    public void clienteAcepto(Pedido pedido) {

        String asunto = "Aceptada la cotización " + pedido.getNumero();

        String cuerpo =
            pedido.getCliente() + " aceptó el precio del pedido "
          + pedido.getNumero() + ".\n\n"
          + "Total: " + plata(pedido.getTotal()) + " " + pedido.getMoneda() + "\n\n"
          + "El pedido queda listo para que la gerencia autorice el despacho."
          + FIRMA;

        avisarAuxiliares(asunto, cuerpo);
    }

    public void clienteRechazo(Pedido pedido, String motivo) {

        String asunto = "Rechazada la cotización " + pedido.getNumero();

        String cuerpo =
            pedido.getCliente() + " no aceptó el precio del pedido "
          + pedido.getNumero() + ".\n\n"
          + "Motivo que escribió el cliente:\n" + motivo + "\n\n"
          + "Los lotes volvieron al inventario disponible."
          + FIRMA;

        avisarAuxiliares(asunto, cuerpo);
    }

    // ----------------------------------------------------------
    // 4. Se le paga al productor  ->  se le avisa a el
    // ----------------------------------------------------------
    public void pagoAlProveedor(Lote lote) {

        String correo = avisoDAO.correoDelProveedorDelLote(lote.getId());

        if (correo == null || correo.isBlank()) {
            return;
        }

        String asunto = "Pago del lote " + lote.getCodigo() + " - RiTech S.A.S";

        String cuerpo =
            "Señor(a) " + lote.getProveedor() + ",\n\n"
          + "RiTech S.A.S autorizó el pago de su lote " + lote.getCodigo() + ".\n\n"
          + "Finca: " + lote.getFinca() + "\n"
          + "Peso: " + plata(lote.getPesoKg()) + " kg\n"
          + (lote.getPrecioCompraKg() == null ? ""
             : "Precio acordado: $" + plata(lote.getPrecioCompraKg()) + " por kilo\n")
          + (lote.getTotalCompra() == null ? ""
             : "Valor a recibir: $" + plata(lote.getTotalCompra()) + "\n")
          + "\nPuede consultar el estado de todos sus lotes entrando al sistema."
          + FIRMA;

        mandar(correo, asunto, cuerpo);
    }

    // ----------------------------------------------------------
    // Maquinaria
    // ----------------------------------------------------------

    private void avisarAuxiliares(String asunto, String cuerpo) {

        List<String> correos = avisoDAO.correosDeAuxiliares();

        for (String correo : correos) {
            mandar(correo, asunto, cuerpo);
        }
    }

    /**
     * Guarda el aviso y despues trata de mandarlo.
     *
     * Ese orden importa: si se guardara despues de mandarlo, un corte
     * de internet dejaria el aviso sin rastro en ninguna parte.
     */
    private void mandar(String destinatario, String asunto, String cuerpo) {

        if (destinatario == null || destinatario.isBlank()) {
            return;
        }

        int idAviso;

        try {
            idAviso = avisoDAO.registrar(destinatario, asunto, cuerpo);
        } catch (Exception e) {
            // Ni siquiera se pudo guardar: se deja constancia en la consola
            // y se sigue. La operacion del usuario no se interrumpe.
            System.out.println("[RiTech] No se pudo registrar el aviso: " + e.getMessage());
            return;
        }

        if (!correoServicio.estaConfigurado()) {
            avisoDAO.marcarPendiente(idAviso,
                "Falta configurar la cuenta de correo en correo.properties");
            return;
        }

        try {
            correoServicio.enviar(destinatario, asunto, cuerpo);
            avisoDAO.marcarEnviado(idAviso);
            System.out.println("[RiTech] Aviso enviado a " + destinatario);

        } catch (Exception e) {
            avisoDAO.marcarError(idAviso, e.getMessage());
            System.out.println("[RiTech] No se pudo enviar el aviso a "
                             + destinatario + ": " + e.getMessage());
        }
    }

    private String plata(double valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.of("es", "CO"));
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", simbolos).format(valor);
    }
}
