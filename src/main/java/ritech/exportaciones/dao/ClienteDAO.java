package ritech.exportaciones.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ritech.exportaciones.modelo.Cliente;
import ritech.exportaciones.modelo.Moneda;
import ritech.exportaciones.modelo.Pais;

@Repository
public class ClienteDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ----------------------------------------------------------
    // Listas para los combos del formulario
    // ----------------------------------------------------------

    public List<Pais> listarPaises() {
        String sql = "SELECT id_pais, nombre " +
                     "FROM pais " +
                     "WHERE es_union_europea = TRUE " +
                     "ORDER BY nombre";

        return jdbc.query(sql, (rs, numeroFila) -> {
            Pais p = new Pais();
            p.setId(rs.getInt("id_pais"));
            p.setNombre(rs.getString("nombre"));
            return p;
        });
    }

    public List<Moneda> listarMonedas() {
        String sql = "SELECT id_moneda, codigo_iso, " +
                     "       CONCAT(codigo_iso, ' - ', nombre) AS texto " +
                     "FROM moneda " +
                     "ORDER BY codigo_iso";

        return jdbc.query(sql, (rs, numeroFila) -> {
            Moneda m = new Moneda();
            m.setId(rs.getInt("id_moneda"));
            m.setTexto(rs.getString("texto"));
            m.setCodigo(rs.getString("codigo_iso"));
            return m;
        });
    }

    // ----------------------------------------------------------
    // Validaciones
    // ----------------------------------------------------------

    public boolean existeEmpresa(String nombreEmpresa) {
        String sql = "SELECT COUNT(*) FROM cliente WHERE nombre_empresa = ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, nombreEmpresa);
        return total != null && total > 0;
    }

    public boolean existeEmpresaEnOtro(String nombreEmpresa, int idCliente) {
        String sql = "SELECT COUNT(*) FROM cliente " +
                     "WHERE nombre_empresa = ? AND id_cliente <> ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, nombreEmpresa, idCliente);
        return total != null && total > 0;
    }

    public boolean tienePedidos(int idCliente) {
        String sql = "SELECT COUNT(*) FROM pedido WHERE id_cliente = ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, idCliente);
        return total != null && total > 0;
    }

    public boolean tieneUsuario(int idCliente) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE id_cliente = ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, idCliente);
        return total != null && total > 0;
    }

    // ----------------------------------------------------------
    // Guardar y actualizar
    // ----------------------------------------------------------

    public void guardar(Cliente c) {
        String sql = "INSERT INTO cliente " +
                     "(nombre_empresa, id_pais, ciudad, persona_contacto, correo, id_moneda_habitual) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        jdbc.update(sql,
                c.getNombreEmpresa(),
                c.getIdPais(),
                c.getCiudad(),
                c.getPersonaContacto(),
                c.getCorreo(),
                c.getIdMoneda());
    }

    public void actualizar(Cliente c) {
        String sql = "UPDATE cliente SET " +
                     "nombre_empresa = ?, " +
                     "id_pais = ?, " +
                     "ciudad = ?, " +
                     "persona_contacto = ?, " +
                     "correo = ?, " +
                     "id_moneda_habitual = ? " +
                     "WHERE id_cliente = ?";

        jdbc.update(sql,
                c.getNombreEmpresa(),
                c.getIdPais(),
                c.getCiudad(),
                c.getPersonaContacto(),
                c.getCorreo(),
                c.getIdMoneda(),
                c.getId());
    }

    public void eliminar(int idCliente) {
        jdbc.update("DELETE FROM cliente WHERE id_cliente = ?", idCliente);
    }

    // ----------------------------------------------------------
    // Consultas
    // ----------------------------------------------------------

    private Cliente convertirFila(ResultSet rs, int numeroFila) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setNombreEmpresa(rs.getString("nombre_empresa"));
        c.setIdPais(rs.getInt("id_pais"));
        c.setPais(rs.getString("pais"));
        c.setCiudad(rs.getString("ciudad"));
        c.setPersonaContacto(rs.getString("persona_contacto"));
        c.setCorreo(rs.getString("correo"));

        int idMoneda = rs.getInt("id_moneda_habitual");
        if (rs.wasNull()) {
            c.setIdMoneda(null);
        } else {
            c.setIdMoneda(idMoneda);
        }

        c.setMoneda(rs.getString("moneda"));
        return c;
    }

    public Cliente buscarPorId(int idCliente) {
        String sql = "SELECT c.id_cliente, c.nombre_empresa, c.id_pais, " +
                     "       p.nombre AS pais, c.ciudad, c.persona_contacto, " +
                     "       c.correo, c.id_moneda_habitual, " +
                     "       m.codigo_iso AS moneda " +
                     "FROM cliente c " +
                     "JOIN pais p ON c.id_pais = p.id_pais " +
                     "LEFT JOIN moneda m ON c.id_moneda_habitual = m.id_moneda " +
                     "WHERE c.id_cliente = ?";

        List<Cliente> resultado = jdbc.query(sql, this::convertirFila, idCliente);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Cliente> listar(String filtro) {
        String texto = (filtro == null) ? "" : filtro.trim();

        String sql = "SELECT c.id_cliente, c.nombre_empresa, c.id_pais, " +
                     "       p.nombre AS pais, c.ciudad, c.persona_contacto, " +
                     "       c.correo, c.id_moneda_habitual, " +
                     "       m.codigo_iso AS moneda " +
                     "FROM cliente c " +
                     "JOIN pais p ON c.id_pais = p.id_pais " +
                     "LEFT JOIN moneda m ON c.id_moneda_habitual = m.id_moneda " +
                     "WHERE (? = '' OR c.nombre_empresa LIKE CONCAT('%', ?, '%') " +
                     "               OR p.nombre LIKE CONCAT('%', ?, '%') " +
                     "               OR c.ciudad LIKE CONCAT('%', ?, '%')) " +
                     "ORDER BY c.nombre_empresa";

        return jdbc.query(sql, this::convertirFila, texto, texto, texto, texto);
    }
}