package ritech.exportaciones.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ritech.exportaciones.modelo.Pedido;
import ritech.exportaciones.modelo.PedidoLote;

@Repository
public class PedidoDAO {

    @Autowired
    private JdbcTemplate jdbc;

    private static final String SELECT_BASE =
            "SELECT p.id_pedido, p.numero, " +
            "       p.id_cliente, c.nombre_empresa AS cliente, " +
            "       p.id_pais_destino, pa.nombre AS pais_destino, " +
            "       p.id_moneda, m.codigo_iso AS moneda, " +
            "       p.tasa_cambio, p.fecha_pedido, p.estado, p.total, " +
            "       p.observacion_cliente, " +
            "       p.id_usuario, u.nombre AS usuario " +
            "FROM pedido p " +
            "JOIN cliente c  ON p.id_cliente      = c.id_cliente " +
            "JOIN pais pa    ON p.id_pais_destino = pa.id_pais " +
            "JOIN moneda m   ON p.id_moneda       = m.id_moneda " +
            "JOIN usuario u  ON p.id_usuario      = u.id_usuario ";

    private static final String SELECT_LOTE =
            "SELECT l.id_lote, l.codigo, " +
            "       CONCAT(prod.nombre, ' - ', v.nombre) AS variedad, " +
            "       l.peso_kg, " +
            "       prov.nombre AS proveedor, " +
            "       f.nombre AS finca, f.lugar, f.latitud, f.longitud " +
            "FROM lote l " +
            "JOIN variedad v     ON l.id_variedad   = v.id_variedad " +
            "JOIN producto prod  ON v.id_producto   = prod.id_producto " +
            "JOIN finca f        ON l.id_finca      = f.id_finca " +
            "JOIN proveedor prov ON f.id_proveedor  = prov.id_proveedor ";

    // ----------------------------------------------------------
    // Numeracion y creacion
    // ----------------------------------------------------------

    public String siguienteNumero() {
        String sql = "SELECT COALESCE(MAX(id_pedido), 0) + 1 FROM pedido";
        Integer siguiente = jdbc.queryForObject(sql, Integer.class);
        return String.format("PED-%04d", siguiente == null ? 1 : siguiente);
    }

    public String crear(Pedido p) {

        String numero = siguienteNumero();

        String sql = "INSERT INTO pedido " +
                     "(numero, id_cliente, id_pais_destino, id_moneda, tasa_cambio, " +
                     " fecha_pedido, estado, total, id_usuario) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'BORRADOR', 0, ?)";

        jdbc.update(sql,
                numero,
                p.getIdCliente(),
                p.getIdPaisDestino(),
                p.getIdMoneda(),
                p.getTasaCambio(),
                p.getFechaPedido(),
                p.getIdUsuario());

        return numero;
    }

    // ----------------------------------------------------------
    // Consultas de pedidos
    // ----------------------------------------------------------

    private Pedido convertirPedido(ResultSet rs, int numeroFila) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getInt("id_pedido"));
        p.setNumero(rs.getString("numero"));
        p.setIdCliente(rs.getInt("id_cliente"));
        p.setCliente(rs.getString("cliente"));
        p.setIdPaisDestino(rs.getInt("id_pais_destino"));
        p.setPaisDestino(rs.getString("pais_destino"));
        p.setIdMoneda(rs.getInt("id_moneda"));
        p.setMoneda(rs.getString("moneda"));
        p.setTasaCambio(rs.getDouble("tasa_cambio"));
        p.setFechaPedido(rs.getString("fecha_pedido"));
        p.setEstado(rs.getString("estado"));
        p.setTotal(rs.getDouble("total"));
        p.setObservacionCliente(rs.getString("observacion_cliente"));
        p.setIdUsuario(rs.getInt("id_usuario"));
        p.setUsuario(rs.getString("usuario"));
        return p;
    }

    public List<Pedido> listar() {
        return jdbc.query(SELECT_BASE + "ORDER BY p.id_pedido DESC", this::convertirPedido);
    }

    public Pedido buscarPorId(int idPedido) {
        String sql = SELECT_BASE + "WHERE p.id_pedido = ?";
        List<Pedido> resultado = jdbc.query(sql, this::convertirPedido, idPedido);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    // ----------------------------------------------------------
    // Lotes
    // ----------------------------------------------------------

    private PedidoLote convertirLote(ResultSet rs, int numeroFila) throws SQLException {
        PedidoLote pl = new PedidoLote();
        pl.setIdLote(rs.getInt("id_lote"));
        pl.setCodigo(rs.getString("codigo"));
        pl.setVariedad(rs.getString("variedad"));
        pl.setPesoKg(rs.getDouble("peso_kg"));
        pl.setProveedor(rs.getString("proveedor"));
        pl.setFinca(rs.getString("finca"));
        pl.setLugar(rs.getString("lugar"));

        double lat = rs.getDouble("latitud");
        pl.setLatitud(rs.wasNull() ? null : lat);

        double lon = rs.getDouble("longitud");
        pl.setLongitud(rs.wasNull() ? null : lon);

        return pl;
    }

    private PedidoLote convertirLoteDelPedido(ResultSet rs, int numeroFila) throws SQLException {
        PedidoLote pl = convertirLote(rs, numeroFila);
        pl.setId(rs.getInt("id_pedido_lote"));
        pl.setPrecioUnitario(rs.getDouble("precio_unitario"));
        pl.setTotalParcial(rs.getDouble("total_parcial"));
        return pl;
    }

    public List<PedidoLote> lotesDisponibles() {
        String sql = SELECT_LOTE + "WHERE l.estado = 'DISPONIBLE' ORDER BY l.codigo";
        return jdbc.query(sql, this::convertirLote);
    }

    public List<PedidoLote> lotesDelPedido(int idPedido) {
        String sql = "SELECT pl.id_pedido_lote, pl.precio_unitario, pl.total_parcial, " +
                     "       l.id_lote, l.codigo, " +
                     "       CONCAT(prod.nombre, ' - ', v.nombre) AS variedad, " +
                     "       l.peso_kg, " +
                     "       prov.nombre AS proveedor, " +
                     "       f.nombre AS finca, f.lugar, f.latitud, f.longitud " +
                     "FROM pedido_lote pl " +
                     "JOIN lote l         ON pl.id_lote     = l.id_lote " +
                     "JOIN variedad v     ON l.id_variedad  = v.id_variedad " +
                     "JOIN producto prod  ON v.id_producto  = prod.id_producto " +
                     "JOIN finca f        ON l.id_finca     = f.id_finca " +
                     "JOIN proveedor prov ON f.id_proveedor = prov.id_proveedor " +
                     "WHERE pl.id_pedido = ? " +
                     "ORDER BY l.codigo";

        return jdbc.query(sql, this::convertirLoteDelPedido, idPedido);
    }

    @Transactional
    public void agregarLote(int idPedido, int idLote, double precioUnitario) {

        Double peso = jdbc.queryForObject(
                "SELECT peso_kg FROM lote WHERE id_lote = ?", Double.class, idLote);

        double totalParcial = (peso == null ? 0 : peso) * precioUnitario;

        jdbc.update("INSERT INTO pedido_lote " +
                    "(id_pedido, id_lote, precio_unitario, total_parcial) " +
                    "VALUES (?, ?, ?, ?)",
                idPedido, idLote, precioUnitario, totalParcial);

        jdbc.update("UPDATE lote SET estado = 'RESERVADO' WHERE id_lote = ?", idLote);

        recalcularTotal(idPedido);
    }

    @Transactional
    public void quitarLote(int idPedidoLote, int idPedido) {

        Integer idLote = jdbc.queryForObject(
                "SELECT id_lote FROM pedido_lote WHERE id_pedido_lote = ?",
                Integer.class, idPedidoLote);

        jdbc.update("DELETE FROM pedido_lote WHERE id_pedido_lote = ?", idPedidoLote);

        if (idLote != null) {
            jdbc.update("UPDATE lote SET estado = 'DISPONIBLE' WHERE id_lote = ?", idLote);
        }

        recalcularTotal(idPedido);
    }

    private void recalcularTotal(int idPedido) {
        jdbc.update("UPDATE pedido SET total = " +
                    "(SELECT COALESCE(SUM(total_parcial), 0) " +
                    " FROM pedido_lote WHERE id_pedido = ?) " +
                    "WHERE id_pedido = ?",
                idPedido, idPedido);
    }

    // ----------------------------------------------------------
    // Solicitudes enviadas por el cliente
    // ----------------------------------------------------------

    public void fijarPrecio(int idPedidoLote, double precio) {
        jdbc.update("UPDATE pedido_lote pl " +
                    "JOIN lote l ON pl.id_lote = l.id_lote " +
                    "SET pl.precio_unitario = ?, pl.total_parcial = l.peso_kg * ? " +
                    "WHERE pl.id_pedido_lote = ?",
                precio, precio, idPedidoLote);
    }

    @Transactional
    public void enviarCotizacion(int idPedido, double tasaCambio) {

        // El pedido NO queda listo todavia: queda COTIZADO, esperando
        // que el cliente acepte o rechace el precio desde su portal.
        jdbc.update("UPDATE pedido SET tasa_cambio = ?, estado = 'COTIZADO', " +
                    "       observacion_cliente = NULL " +
                    "WHERE id_pedido = ?", tasaCambio, idPedido);

        jdbc.update("UPDATE lote SET estado = 'RESERVADO' " +
                    "WHERE id_lote IN (SELECT id_lote FROM pedido_lote WHERE id_pedido = ?)",
                idPedido);

        recalcularTotal(idPedido);
    }

    /**
     * El auxiliar termina de armar un pedido y se lo manda al cliente.
     *
     * A partir de aqui el pedido ya no se puede modificar: lo que el
     * cliente apruebe tiene que ser exactamente lo que se despache.
     */
    @Transactional
    public boolean enviarBorrador(int idPedido) {

        int filas = jdbc.update(
                "UPDATE pedido SET estado = 'COTIZADO', observacion_cliente = NULL " +
                "WHERE id_pedido = ? AND estado = 'BORRADOR'", idPedido);

        if (filas == 0) {
            return false;
        }

        jdbc.update("UPDATE lote SET estado = 'RESERVADO' " +
                    "WHERE id_lote IN (SELECT id_lote FROM pedido_lote WHERE id_pedido = ?)",
                idPedido);

        recalcularTotal(idPedido);
        return true;
    }

    @Transactional
    public void rechazarSolicitud(int idPedido) {

        jdbc.update("UPDATE lote SET estado = 'DISPONIBLE' " +
                    "WHERE id_lote IN (SELECT id_lote FROM pedido_lote WHERE id_pedido = ?)",
                idPedido);

        jdbc.update("DELETE FROM pedido_lote WHERE id_pedido = ?", idPedido);
        jdbc.update("DELETE FROM pedido WHERE id_pedido = ?", idPedido);
    }

    // ----------------------------------------------------------
    // Verificacion de origen y despacho
    // ----------------------------------------------------------

    public int cantidadLotes(int idPedido) {
        String sql = "SELECT COUNT(*) FROM pedido_lote WHERE id_pedido = ?";
        Integer total = jdbc.queryForObject(sql, Integer.class, idPedido);
        return total == null ? 0 : total;
    }

    public List<String> lotesSinCoordenadas(int idPedido) {
        String sql = "SELECT l.codigo " +
                     "FROM pedido_lote pl " +
                     "JOIN lote l  ON pl.id_lote  = l.id_lote " +
                     "JOIN finca f ON l.id_finca  = f.id_finca " +
                     "WHERE pl.id_pedido = ? " +
                     "  AND (f.latitud IS NULL OR f.longitud IS NULL) " +
                     "ORDER BY l.codigo";

        return jdbc.queryForList(sql, String.class, idPedido);
    }

    @Transactional
    public void despachar(int idPedido) {

        jdbc.update("UPDATE pedido SET estado = 'DESPACHADO' WHERE id_pedido = ?", idPedido);

        jdbc.update("UPDATE lote SET estado = 'DESPACHADO' " +
                    "WHERE id_lote IN (SELECT id_lote FROM pedido_lote WHERE id_pedido = ?)",
                idPedido);
    }
}
