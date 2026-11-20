package ritech.exportaciones.modelo;

public class Moneda {

    private int id;
    private String texto;
    private String codigo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    /** USD, EUR, COP. Sirve para saber que tasa de cambio aplicar. */
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
