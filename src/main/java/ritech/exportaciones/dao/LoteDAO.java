package ritech.exportaciones.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ritech.exportaciones.modelo.Lote;
import ritech.exportaciones.modelo.Variedad;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class LoteDAO {

    @Autowired
    private JdbcTemplate jdbc;

    private Variedad convertirVariedad(ResultSet rs, int numeroFila) throws SQLException {
        Variedad v = new Variedad();
        v.setId(rs.getInt("id_variedad"));
        v.setTexto(rs.getString("texto"));
        return v;
    }

    public List<Variedad> listarVariedades() {
        String sql = "SELECT v.id_variedad, CONCAT(pr.nombre, ' - ', v.nombre) AS texto "
                   + "FROM variedad v "
                   + "JOIN producto pr ON pr.id_producto = v.id_producto "
                   + "ORDER BY pr.nombre, v.nombre";

        return jdbc.query(sql, this::convertirVariedad);
    }

    public Integer idFincaDe(int idProveedor) {
        String sql = "SELECT id_finca FROM finca WHERE id_proveedor = ? LIMIT 1";
        List<Integer> encontradas = jdbc.queryForList(sql, Integer.class, idProveedor);

        if (encontradas.isEmpty()) {
            return null;
        }
        return encontradas.get(0);
    }

    public String siguienteCodigo() {
        String sql = "SELECT COALESCE(MAX(id_lote), 0) FROM lote";
        Integer ultimo = jdbc.queryForObject(sql, Integer.class);

        if (ultimo == null) {
            ultimo = 0;
        }
        return String.format("LOTE-%04d", ultimo + 1);
    }

    public String registrar(int idVariedad, int idFinca, double pesoKg,
                            String fechaCosecha, double precioCompraKg) {
        String codigo = siguienteCodigo();

        String sql = "INSERT INTO lote (codigo, id_variedad, id_finca, peso_kg, "
                   + "                  fecha_cosecha, estado, precio_compra_kg, pagado) "
                   + "VALUES (?, ?, ?, ?, ?, 'DISPONIBLE', ?, FALSE)";

        jdbc.update(sql, codigo, idVariedad, idFinca, pesoKg, fechaCosecha, precioCompraKg);

        return codigo;
    }

    private Lote convertirFila(ResultSet rs, int numeroFila) throws SQLException {
        Lote l = new Lote();
        l.setId(rs.getInt("id_lote"));
        l.setCodigo(rs.getString("codigo"));
        l.setProducto(rs.getString("producto"));
        l.setVariedad(rs.getString("variedad"));
        l.setPesoKg(rs.getDouble("peso_kg"));
        l.setFechaCosecha(String.valueOf(rs.getDate("fecha_cosecha")));
        l.setProveedor(rs.getString("proveedor"));
        l.setFinca(rs.getString("finca"));
        l.setEstado(rs.getString("estado"));

        double precio = rs.getDouble("precio_compra_kg");
        l.setPrecioCompraKg(rs.wasNull() ? null : precio);

        l.setPagado(rs.getBoolean("pagado"));
        l.setFechaPago(rs.getString("fecha_pago"));

        return l;
    }

    public List<Lote> listar() {
        String sql = "SELECT l.id_lote, l.codigo, pr.nombre AS producto, v.nombre AS variedad, "
                   + "       l.peso_kg, l.fecha_cosecha, l.estado, "
                   + "       l.precio_compra_kg, l.pagado, l.fecha_pago, "
                   + "       p.nombre AS proveedor, f.nombre AS finca "
                   + "FROM lote l "
                   + "JOIN variedad v  ON v.id_variedad = l.id_variedad "
                   + "JOIN producto pr ON pr.id_producto = v.id_producto "
                   + "JOIN finca f     ON f.id_finca = l.id_finca "
                   + "JOIN proveedor p ON p.id_proveedor = f.id_proveedor "
                   + "ORDER BY l.id_lote DESC";

        return jdbc.query(sql, this::convertirFila);
    }
    public Lote consultarOrigen(String codigo) {

        String sql = "SELECT l.id_lote, l.codigo, pr.nombre AS producto, v.nombre AS variedad, "
                   + "       l.peso_kg, l.fecha_cosecha, l.estado, "
                   + "       p.nombre AS proveedor, f.nombre AS finca, "
                   + "       f.lugar, f.latitud, f.longitud "
                   + "FROM lote l "
                   + "JOIN variedad v  ON v.id_variedad = l.id_variedad "
                   + "JOIN producto pr ON pr.id_producto = v.id_producto "
                   + "JOIN finca f     ON f.id_finca = l.id_finca "
                   + "JOIN proveedor p ON p.id_proveedor = f.id_proveedor "
                   + "WHERE l.codigo = ?";

        List<Lote> encontrados = jdbc.query(sql, (rs, fila) -> {
            Lote l = new Lote();
            l.setCodigo(rs.getString("codigo"));
            l.setProducto(rs.getString("producto"));
            l.setVariedad(rs.getString("variedad"));
            l.setPesoKg(rs.getDouble("peso_kg"));
            l.setFechaCosecha(String.valueOf(rs.getDate("fecha_cosecha")));
            l.setProveedor(rs.getString("proveedor"));
            l.setFinca(rs.getString("finca"));
            l.setLugar(rs.getString("lugar"));
            l.setEstado(rs.getString("estado"));

            double lat = rs.getDouble("latitud");
            l.setLatitud(rs.wasNull() ? null : lat);

            double lon = rs.getDouble("longitud");
            l.setLongitud(rs.wasNull() ? null : lon);

            return l;
        }, codigo.trim());

        if (encontrados.isEmpty()) {
            return null;
        }
        return encontrados.get(0);
    }
}