package ritech.exportaciones.modelo;

public class LoteProveedor {

    private int idLote;
    private String codigo;
    private String variedad;
    private double pesoKg;
    private String fechaCosecha;
    private String estado;
    private String finca;
    private String lugar;
    private Double latitud;
    private Double longitud;
    private String pedido;
    private String cliente;
    private String destino;
    private String fechaPedido;
    private Double precioCompraKg;
    private boolean pagado;
    private String fechaPago;

    public int getIdLote() {
        return idLote;
    }

    public void setIdLote(int idLote) {
        this.idLote = idLote;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getVariedad() {
        return variedad;
    }

    public void setVariedad(String variedad) {
        this.variedad = variedad;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public String getFechaCosecha() {
        return fechaCosecha;
    }

    public void setFechaCosecha(String fechaCosecha) {
        this.fechaCosecha = fechaCosecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFinca() {
        return finca;
    }

    public void setFinca(String finca) {
        this.finca = finca;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public Double getLatitud() {
        return latitud;
    }

    public void setLatitud(Double latitud) {
        this.latitud = latitud;
    }

    public Double getLongitud() {
        return longitud;
    }

    public void setLongitud(Double longitud) {
        this.longitud = longitud;
    }

    public String getPedido() {
        return pedido;
    }

    public void setPedido(String pedido) {
        this.pedido = pedido;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public String getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(String fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public boolean tieneCoordenadas() {
        return latitud != null && longitud != null;
    }

    // =========================================================
    // Pago al productor
    // =========================================================

    public Double getPrecioCompraKg() {
        return precioCompraKg;
    }

    public void setPrecioCompraKg(Double precioCompraKg) {
        this.precioCompraKg = precioCompraKg;
    }

    public boolean isPagado() {
        return pagado;
    }

    public void setPagado(boolean pagado) {
        this.pagado = pagado;
    }

    public String getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(String fechaPago) {
        this.fechaPago = fechaPago;
    }

    /** Lo que RiTech le debe al productor por este lote, en pesos. */
    public Double getTotalCompra() {
        if (precioCompraKg == null) {
            return null;
        }
        return pesoKg * precioCompraKg;
    }

    /** Texto para mostrarle al productor en que va su pago. */
    public String getEstadoPago() {
        if (precioCompraKg == null) {
            return "Sin definir";
        }
        return pagado ? "Pagado" : "Pendiente de pago";
    }
}
