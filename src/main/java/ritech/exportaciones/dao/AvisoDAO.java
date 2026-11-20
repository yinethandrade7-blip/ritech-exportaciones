package ritech.exportaciones.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import ritech.exportaciones.modelo.Aviso;

/**
 * Los avisos que el sistema le manda por correo a la gente.
 *
 * Cada aviso queda guardado en la base de datos ANTES de intentar
 * mandarlo. Esto es a proposito: si el correo no sale, el aviso no
 * se pierde, queda ahi con el motivo de la falla y alguien lo puede
 * revisar. Un sistema que solo manda correos y no deja rastro no
 * sirve para auditar nada.
 */
@Repository
public class AvisoDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ----------------------------------------------------------
    // Guarda el aviso y devuelve su numero, para poder marcarlo
    // despues como enviado o fallido.
    //
    // Se pide el numero con un KeyHolder y no con un SELECT MAX
    // aparte: dos consultas distintas pueden salir por conexiones
    // distintas, y entre una y otra otro usuario pudo insertar otro
    // aviso. El KeyHolder devuelve el numero de ESTA insercion.
    // ----------------------------------------------------------
    public int registrar(String destinatario, String asunto, String cuerpo) {

        final String sql =
            "INSERT INTO aviso (momento, destinatario, asunto, cuerpo, estado) " +
            "VALUES (NOW(), ?, ?, ?, 'PENDIENTE')";

        KeyHolder llave = new GeneratedKeyHolder();

        jdbc.update(conexion -> {
            PreparedStatement ps = conexion.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, destinatario);
            ps.setString(2, asunto);
            ps.setString(3, cuerpo);
            return ps;
        }, llave);

        Number numero = llave.getKey();

        return numero == null ? 0 : numero.intValue();
    }

    public void marcarEnviado(int idAviso) {
        jdbc.update("UPDATE aviso SET estado = 'ENVIADO', detalle = NULL " +
                    "WHERE id_aviso = ?", idAviso);
    }

    public void marcarError(int idAviso, String detalle) {

        if (detalle != null && detalle.length() > 255) {
            detalle = detalle.substring(0, 255);
        }

        jdbc.update("UPDATE aviso SET estado = 'ERROR', detalle = ? " +
                    "WHERE id_aviso = ?", detalle, idAviso);
    }

    public void marcarPendiente(int idAviso, String detalle) {

        if (detalle != null && detalle.length() > 255) {
            detalle = detalle.substring(0, 255);
        }

        jdbc.update("UPDATE aviso SET estado = 'PENDIENTE', detalle = ? " +
                    "WHERE id_aviso = ?", detalle, idAviso);
    }

    // ----------------------------------------------------------
    // Consultas para la pantalla de avisos
    // ----------------------------------------------------------

    public List<Aviso> ultimos(int cuantos) {

        String sql =
            "SELECT id_aviso, DATE_FORMAT(momento, '%d/%m/%Y %H:%i') AS momento, " +
            "       destinatario, asunto, cuerpo, estado, detalle " +
            "FROM aviso ORDER BY id_aviso DESC LIMIT " + cuantos;

        return jdbc.query(sql, this::convertir);
    }

    public int cuantos(String estado) {

        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM aviso WHERE estado = ?", Integer.class, estado);

        return n == null ? 0 : n;
    }

    private Aviso convertir(ResultSet rs, int fila) throws SQLException {

        Aviso a = new Aviso();

        a.setIdAviso(rs.getInt("id_aviso"));
        a.setMomento(rs.getString("momento"));
        a.setDestinatario(rs.getString("destinatario"));
        a.setAsunto(rs.getString("asunto"));
        a.setCuerpo(rs.getString("cuerpo"));
        a.setEstado(rs.getString("estado"));
        a.setDetalle(rs.getString("detalle"));

        return a;
    }

    // ----------------------------------------------------------
    // A quien hay que avisarle
    // ----------------------------------------------------------

    /** Correos de los auxiliares activos: son los que operan los pedidos */
    public List<String> correosDeAuxiliares() {
        return jdbc.queryForList(
            "SELECT u.correo FROM usuario u " +
            "JOIN rol r ON r.id_rol = u.id_rol " +
            "WHERE r.nombre = 'AUXILIAR' AND u.activo = TRUE",
            String.class);
    }

    /** Correo de contacto de la empresa compradora */
    public String correoDelCliente(int idPedido) {
        List<String> r = jdbc.queryForList(
            "SELECT c.correo FROM pedido p " +
            "JOIN cliente c ON c.id_cliente = p.id_cliente " +
            "WHERE p.id_pedido = ?",
            String.class, idPedido);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Nombre de la empresa compradora, para escribirlo en el aviso */
    public String empresaDelCliente(int idCliente) {
        List<String> r = jdbc.queryForList(
            "SELECT nombre_empresa FROM cliente WHERE id_cliente = ?",
            String.class, idCliente);
        return r.isEmpty() ? "La empresa" : r.get(0);
    }

    /** Correo del productor dueño de la finca de donde salio el lote */
    public String correoDelProveedorDelLote(int idLote) {
        List<String> r = jdbc.queryForList(
            "SELECT pr.correo FROM lote l " +
            "JOIN finca f     ON f.id_finca     = l.id_finca " +
            "JOIN proveedor pr ON pr.id_proveedor = f.id_proveedor " +
            "WHERE l.id_lote = ?",
            String.class, idLote);
        return r.isEmpty() ? null : r.get(0);
    }
}
