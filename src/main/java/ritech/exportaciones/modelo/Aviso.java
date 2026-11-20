package ritech.exportaciones.modelo;

/**
 * Un aviso que el sistema le manda por correo a alguien.
 *
 * Se guarda aunque el correo no haya salido. Asi queda constancia de
 * que el sistema si intento avisar, y de por que no pudo si fallo.
 */
public class Aviso {

    private int idAviso;
    private String momento;
    private String destinatario;
    private String asunto;
    private String cuerpo;
    private String estado;    // ENVIADO, PENDIENTE o ERROR
    private String detalle;

    public int getIdAviso() { return idAviso; }
    public void setIdAviso(int idAviso) { this.idAviso = idAviso; }

    public String getMomento() { return momento; }
    public void setMomento(String momento) { this.momento = momento; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public boolean isEnviado()   { return "ENVIADO".equals(estado); }
    public boolean isPendiente() { return "PENDIENTE".equals(estado); }
    public boolean isError()     { return "ERROR".equals(estado); }
}
