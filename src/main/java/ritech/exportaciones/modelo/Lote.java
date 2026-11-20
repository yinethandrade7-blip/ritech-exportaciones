package ritech.exportaciones.modelo;

public class Lote {

    private int id;
    private String codigo;
    private String producto;
    private String variedad;
    private double pesoKg;
    private String fechaCosecha;
    private String proveedor;
    private String finca;
    private String estado;
    private Double latitud;
    private Double longitud;
    private String lugar;
    private Double precioCompraKg;
    private boolean pagado;
    private String fechaPago;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
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

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getFinca() {
        return finca;
    }

    public void setFinca(String finca) {
        this.finca = finca;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    // =========================================================
    // Compra al productor
    //
    // Lo que RiTech le paga al caficultor por kilo, en pesos.
    // Es distinto del precio al que se le vende al cliente
    // europeo: ese va en pedido_lote y esta en dolares o euros.
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

    /** Lo que se le debe al productor por este lote, en pesos. */
    public Double getTotalCompra() {
        if (precioCompraKg == null) {
            return null;
        }
        return pesoKg * precioCompraKg;
    }
}
