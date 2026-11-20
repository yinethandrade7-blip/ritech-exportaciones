package ritech.exportaciones.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ritech.exportaciones.modelo.Proveedor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ProveedorDAO {

    @Autowired
    private JdbcTemplate jdbc;

    public boolean existeDocumento(String documento) {
        String sql = "SELECT COUNT(*) FROM proveedor WHERE documento = ?";
        Integer cantidad = jdbc.queryForObject(sql, Integer.class, documento);
        return cantidad != null && cantidad > 0;
    }

    public boolean existeDocumentoEnOtro(String documento, int idProveedor) {
        String sql = "SELECT COUNT(*) FROM proveedor WHERE documento = ? AND id_proveedor <> ?";
        Integer cantidad = jdbc.queryForObject(sql, Integer.class, documento, idProveedor);
        return cantidad != null && cantidad > 0;
    }

    public boolean tieneLotes(int idProveedor) {
        String sql = "SELECT COUNT(*) FROM lote l "
                   + "JOIN finca f ON f.id_finca = l.id_finca "
                   + "WHERE f.id_proveedor = ?";
        Integer cantidad = jdbc.queryForObject(sql, Integer.class, idProveedor);
        return cantidad != null && cantidad > 0;
    }

    @Transactional
    public void guardar(Proveedor p) {

        String sqlProveedor = "INSERT INTO proveedor (documento, nombre, telefono, correo) "
                            + "VALUES (?, ?, ?, ?)";

        jdbc.update(sqlProveedor,
                    p.getDocumento(),
                    p.getNombre(),
                    p.getTelefono(),
                    p.getCorreo());

        String sqlBuscarId = "SELECT id_proveedor FROM proveedor WHERE documento = ?";
        Integer idProveedor = jdbc.queryForObject(sqlBuscarId, Integer.class, p.getDocumento());

        String sqlFinca = "INSERT INTO finca (id_proveedor, nombre, lugar, latitud, longitud) "
                        + "VALUES (?, ?, ?, ?, ?)";

        jdbc.update(sqlFinca,
                    idProveedor,
                    p.getNombreFinca(),
                    p.getLugar(),
                    p.getLatitud(),
                    p.getLongitud());

        p.setId(idProveedor);
    }

    @Transactional
    public void actualizar(Proveedor p) {

        String sqlProveedor = "UPDATE proveedor "
                            + "SET documento = ?, nombre = ?, telefono = ?, correo = ? "
                            + "WHERE id_proveedor = ?";

        jdbc.update(sqlProveedor,
                    p.getDocumento(),
                    p.getNombre(),
                    p.getTelefono(),
                    p.getCorreo(),
                    p.getId());

        String sqlFinca = "UPDATE finca "
                        + "SET nombre = ?, lugar = ?, latitud = ?, longitud = ? "
                        + "WHERE id_proveedor = ?";

        jdbc.update(sqlFinca,
                    p.getNombreFinca(),
                    p.getLugar(),
                    p.getLatitud(),
                    p.getLongitud(),
                    p.getId());
    }

    @Transactional
    public void eliminar(int idProveedor) {
        jdbc.update("DELETE FROM finca WHERE id_proveedor = ?", idProveedor);
        jdbc.update("DELETE FROM proveedor WHERE id_proveedor = ?", idProveedor);
    }

    private Proveedor convertirFila(ResultSet rs, int numeroFila) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id_proveedor"));
        p.setDocumento(rs.getString("documento"));
        p.setNombre(rs.getString("nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setCorreo(rs.getString("correo"));
        p.setNombreFinca(rs.getString("finca"));
        p.setLugar(rs.getString("lugar"));

        double lat = rs.getDouble("latitud");
        p.setLatitud(rs.wasNull() ? null : lat);

        double lon = rs.getDouble("longitud");
        p.setLongitud(rs.wasNull() ? null : lon);

        return p;
    }

    public Proveedor buscarPorId(int idProveedor) {

        String sql = "SELECT p.id_proveedor, p.documento, p.nombre, p.telefono, p.correo, "
                   + "       f.nombre AS finca, f.lugar, f.latitud, f.longitud "
                   + "FROM proveedor p "
                   + "LEFT JOIN finca f ON f.id_proveedor = p.id_proveedor "
                   + "WHERE p.id_proveedor = ?";

        List<Proveedor> encontrados = jdbc.query(sql, this::convertirFila, idProveedor);

        if (encontrados.isEmpty()) {
            return null;
        }
        return encontrados.get(0);
    }

    public List<Proveedor> listar(String filtro) {

        String texto = (filtro == null) ? "" : filtro.trim();

        String sql = "SELECT p.id_proveedor, p.documento, p.nombre, p.telefono, p.correo, "
                   + "       f.nombre AS finca, f.lugar, f.latitud, f.longitud "
                   + "FROM proveedor p "
                   + "LEFT JOIN finca f ON f.id_proveedor = p.id_proveedor "
                   + "WHERE (? = '' OR p.nombre LIKE ? OR p.documento LIKE ?) "
                   + "ORDER BY p.nombre";

        return jdbc.query(sql, this::convertirFila, texto, "%" + texto + "%", "%" + texto + "%");
    }
}
