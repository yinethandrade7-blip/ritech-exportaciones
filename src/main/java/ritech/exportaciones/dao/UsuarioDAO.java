package ritech.exportaciones.dao;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ritech.exportaciones.modelo.Rol;
import ritech.exportaciones.modelo.Usuario;

@Repository
public class UsuarioDAO {

    @Autowired
    private JdbcTemplate jdbc;

    private static final String SELECT_BASE =
            "SELECT u.id_usuario, u.nombre, u.correo, u.activo, u.id_rol, " +
            "       r.nombre AS rol, " +
            "       u.id_proveedor, u.id_cliente, " +
            "       COALESCE(p.nombre, c.nombre_empresa) AS vinculo " +
            "FROM usuario u " +
            "JOIN rol r ON u.id_rol = r.id_rol " +
            "LEFT JOIN proveedor p ON u.id_proveedor = p.id_proveedor " +
            "LEFT JOIN cliente   c ON u.id_cliente   = c.id_cliente ";

    public static String cifrar(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo cifrar la contraseña", e);
        }
    }

    private Usuario convertirFila(ResultSet rs, int numeroFila) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setCorreo(rs.getString("correo"));
        u.setRol(rs.getString("rol"));
        u.setActivo(rs.getBoolean("activo"));
        u.setIdRol(rs.getInt("id_rol"));

        int idProveedor = rs.getInt("id_proveedor");
        u.setIdProveedor(rs.wasNull() ? null : idProveedor);

        int idCliente = rs.getInt("id_cliente");
        u.setIdCliente(rs.wasNull() ? null : idCliente);

        u.setVinculo(rs.getString("vinculo"));
        return u;
    }

    public Usuario autenticar(String correo, String clave) {
        String sql = SELECT_BASE +
                     "WHERE u.correo = ? AND u.contrasena_hash = ? AND u.activo = TRUE";

        List<Usuario> resultado = jdbc.query(sql, this::convertirFila, correo, cifrar(clave));
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public List<Usuario> listar() {
        return jdbc.query(SELECT_BASE + "ORDER BY r.nombre, u.nombre", this::convertirFila);
    }

    public List<Rol> listarRoles() {
        String sql = "SELECT id_rol, nombre FROM rol ORDER BY id_rol";
        return jdbc.query(sql, (rs, n) -> {
            Rol r = new Rol();
            r.setId(rs.getInt("id_rol"));
            r.setNombre(rs.getString("nombre"));
            return r;
        });
    }

    public String nombreRol(int idRol) {
        String sql = "SELECT nombre FROM rol WHERE id_rol = ?";
        List<String> resultado = jdbc.queryForList(sql, String.class, idRol);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public boolean existeCorreo(String correo) {
        String sql = "SELECT COUNT(*) FROM usuario WHERE correo = ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, correo);
        return total != null && total > 0;
    }

    public void crear(String nombre, String correo, String clave,
                      int idRol, Integer idProveedor, Integer idCliente) {

        String sql = "INSERT INTO usuario " +
                     "(nombre, correo, contrasena_hash, id_rol, activo, id_proveedor, id_cliente) " +
                     "VALUES (?, ?, ?, ?, TRUE, ?, ?)";

        jdbc.update(sql, nombre, correo, cifrar(clave), idRol, idProveedor, idCliente);
    }

    public void cambiarEstado(int idUsuario, boolean activo) {
        jdbc.update("UPDATE usuario SET activo = ? WHERE id_usuario = ?", activo, idUsuario);
    }
}