package ritech.exportaciones.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ritech.exportaciones.modelo.LoteProveedor;
import ritech.exportaciones.modelo.Pedido;

@Repository
public class PortalDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // ----------------------------------------------------------
    // Conversion de lotes
    // ----------------------------------------------------------

    private LoteProveedor convertirLote(ResultSet rs, int numeroFila) throws SQLException {
        LoteProveedor l = new LoteProveedor();
        l.setCodigo(rs.getString("codigo"));
        l.setVariedad(rs.getString("variedad"));
        l.setPesoKg(rs.getDouble("peso_kg"));
        l.setFechaCosecha(rs.getString("fecha_cosecha"));
        l.setEstado(rs.getString("estado"));
        l.setFinca(rs.getString("finca"));
        l.setLugar(rs.getString("lugar"));

        double lat = rs.getDouble("latitud");
        l.setLatitud(rs.wasNull() ? null : lat);

        double lon = rs.getDouble("longitud");
        l.setLongitud(rs.wasNull() ? null : lon);

        l.setPedido(rs.getString("pedido"));
        l.setCliente(rs.getString("cliente"));
        l.setDestino(rs.getString("destino"));
        l.setFechaPedido(rs.getString("fecha_pedido"));

        double precio = rs.getDouble("precio_compra_kg");
        l.setPrecioCompraKg(rs.wasNull() ? null : precio);

        l.setPagado(rs.getBoolean("pagado"));
        l.setFechaPago(rs.getString("fecha_pago"));

        return l;
    }

    // ----------------------------------------------------------
    // Portal del proveedor
    // ----------------------------------------------------------

    public List<LoteProveedor> lotesDelProveedor(int idProveedor) {

        String sql = "SELECT l.codigo, " +
                     "       CONCAT(prod.nombre, ' - ', v.nombre) AS variedad, " +
                     "       l.peso_kg, l.fecha_cosecha, l.estado, " +
                     "       f.nombre AS finca, f.lugar, f.latitud, f.longitud, " +
                     "       l.precio_compra_kg, l.pagado, l.fecha_pago, " +
                     "       ped.numero AS pedido, " +
                     "       c.nombre_empresa AS cliente, " +
                     "       pa.nombre AS destino, " +
                     "       ped.fecha_pedido " +
                     "FROM lote l " +
                     "JOIN variedad v    ON l.id_variedad = v.id_variedad " +
                     "JOIN producto prod ON v.id_producto = prod.id_producto " +
                     "JOIN finca f       ON l.id_finca    = f.id_finca " +
                     "LEFT JOIN pedido_lote pl ON l.id_lote    = pl.id_lote " +
                     // La condicion del estado va DENTRO del LEFT JOIN, no en
                     // el WHERE. Si estuviera en el WHERE, los lotes que nunca
                     // han estado en un pedido desapareceran de la lista.
                     // Asi, un pedido rechazado simplemente no se muestra: el
                     // lote volvio al inventario y decir que una empresa se lo
                     // compro seria mentirle al productor.
                     "LEFT JOIN pedido ped     ON pl.id_pedido = ped.id_pedido " +
                     "                        AND ped.estado <> 'RECHAZADO' " +
                     "LEFT JOIN cliente c      ON ped.id_cliente = c.id_cliente " +
                     "LEFT JOIN pais pa        ON ped.id_pais_destino = pa.id_pais " +
                     "WHERE f.id_proveedor = ? " +
                     "ORDER BY l.codigo";

        return jdbc.query(sql, this::convertirLote, idProveedor);
    }

    public double kilosTotales(int idProveedor) {
        String sql = "SELECT COALESCE(SUM(l.peso_kg), 0) " +
                     "FROM lote l JOIN finca f ON l.id_finca = f.id_finca " +
                     "WHERE f.id_proveedor = ?";
        Double total = jdbc.queryForObject(sql, Double.class, idProveedor);
        return total == null ? 0 : total;
    }

    public double kilosExportados(int idProveedor) {
        String sql = "SELECT COALESCE(SUM(l.peso_kg), 0) " +
                     "FROM lote l JOIN finca f ON l.id_finca = f.id_finca " +
                     "WHERE f.id_proveedor = ? AND l.estado = 'DESPACHADO'";
        Double total = jdbc.queryForObject(sql, Double.class, idProveedor);
        return total == null ? 0 : total;
    }

    /** Plata que RiTech todavia le debe a este productor, en pesos. */
    public double totalPorPagar(int idProveedor) {
        String sql = "SELECT COALESCE(SUM(l.peso_kg * l.precio_compra_kg), 0) " +
                     "FROM lote l JOIN finca f ON l.id_finca = f.id_finca " +
                     "WHERE f.id_proveedor = ? " +
                     "  AND l.precio_compra_kg IS NOT NULL " +
                     "  AND l.pagado = FALSE";
        Double total = jdbc.queryForObject(sql, Double.class, idProveedor);
        return total == null ? 0 : total;
    }

    /** Plata que ya se le pago a este productor, en pesos. */
    public double totalPagado(int idProveedor) {
        String sql = "SELECT COALESCE(SUM(l.peso_kg * l.precio_compra_kg), 0) " +
                     "FROM lote l JOIN finca f ON l.id_finca = f.id_finca " +
                     "WHERE f.id_proveedor = ? " +
                     "  AND l.precio_compra_kg IS NOT NULL " +
                     "  AND l.pagado = TRUE";
        Double total = jdbc.queryForObject(sql, Double.class, idProveedor);
        return total == null ? 0 : total;
    }

    // ----------------------------------------------------------
    // Inventario que ve el cliente
    // ----------------------------------------------------------

    public List<LoteProveedor> inventarioDisponible() {

        String sql = "SELECT l.id_lote, l.codigo, " +
                     "       CONCAT(prod.nombre, ' - ', v.nombre) AS variedad, " +
                     "       l.peso_kg, l.fecha_cosecha, l.estado, " +
                     "       f.nombre AS finca, f.lugar, f.latitud, f.longitud, " +
                     "       NULL AS pedido, NULL AS cliente, " +
                     "       NULL AS destino, NULL AS fecha_pedido, " +
                     // A proposito no se consulta el precio de compra:
                     // lo que RiTech le paga al productor es informacion
                     // interna y el cliente europeo no tiene por que verla.
                     "       NULL AS precio_compra_kg, FALSE AS pagado, " +
                     "       NULL AS fecha_pago " +
                     "FROM lote l " +
                     "JOIN variedad v    ON l.id_variedad = v.id_variedad " +
                     "JOIN producto prod ON v.id_producto = prod.id_producto " +
                     "JOIN finca f       ON l.id_finca    = f.id_finca " +
                     "WHERE l.estado = 'DISPONIBLE' " +
                     "ORDER BY l.codigo";

        return jdbc.query(sql, (rs, n) -> {
            LoteProveedor l = convertirLote(rs, n);
            l.setIdLote(rs.getInt("id_lote"));
            return l;
        });
    }

    // ----------------------------------------------------------
    // El cliente solicita lotes
    // ----------------------------------------------------------

    public String siguienteNumeroPedido() {
        String sql = "SELECT COALESCE(MAX(id_pedido), 0) + 1 FROM pedido";
        Integer siguiente = jdbc.queryForObject(sql, Integer.class);
        return String.format("PED-%04d", siguiente == null ? 1 : siguiente);
    }

    public boolean loteSigueDisponible(int idLote) {
        String sql = "SELECT COUNT(*) FROM lote WHERE id_lote = ? AND estado = 'DISPONIBLE'";
        Integer total = jdbc.queryForObject(sql, Integer.class, idLote);
        return total != null && total > 0;
    }

    @Transactional
    public String crearSolicitud(int idCliente, int idUsuario, int[] idsLotes) {

        String numero = siguienteNumeroPedido();

        // El pais de destino y la moneda salen de la ficha del cliente.
        // La moneda queda en euros si el cliente no tiene una definida.
        jdbc.update(
            "INSERT INTO pedido " +
            "(numero, id_cliente, id_pais_destino, id_moneda, tasa_cambio, " +
            " fecha_pedido, estado, total, id_usuario) " +
            "SELECT ?, c.id_cliente, c.id_pais, " +
            "       COALESCE(c.id_moneda_habitual, 3), 0, CURDATE(), 'SOLICITADO', 0, ? " +
            "FROM cliente c WHERE c.id_cliente = ?",
            numero, idUsuario, idCliente);

        Integer idPedido = jdbc.queryForObject(
            "SELECT id_pedido FROM pedido WHERE numero = ?", Integer.class, numero);

        for (int idLote : idsLotes) {
            jdbc.update("INSERT INTO pedido_lote " +
                        "(id_pedido, id_lote, precio_unitario, total_parcial) " +
                        "VALUES (?, ?, 0, 0)", idPedido, idLote);

            jdbc.update("UPDATE lote SET estado = 'SOLICITADO' WHERE id_lote = ?", idLote);
        }

        return numero;
    }

    // ----------------------------------------------------------
    // Pedidos de un cliente
    // ----------------------------------------------------------

    private Pedido convertirPedido(ResultSet rs, int numeroFila) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getInt("id_pedido"));
        p.setNumero(rs.getString("numero"));
        p.setCliente(rs.getString("cliente"));
        p.setPaisDestino(rs.getString("pais_destino"));
        p.setMoneda(rs.getString("moneda"));
        p.setTasaCambio(rs.getDouble("tasa_cambio"));
        p.setFechaPedido(rs.getString("fecha_pedido"));
        p.setEstado(rs.getString("estado"));
        p.setTotal(rs.getDouble("total"));
        p.setObservacionCliente(rs.getString("observacion_cliente"));
        return p;
    }

    public List<Pedido> pedidosDelCliente(int idCliente) {

        String sql = "SELECT p.id_pedido, p.numero, c.nombre_empresa AS cliente, " +
                     "       pa.nombre AS pais_destino, m.codigo_iso AS moneda, " +
                     "       p.tasa_cambio, p.fecha_pedido, p.estado, p.total, " +
                     "       p.observacion_cliente " +
                     "FROM pedido p " +
                     "JOIN cliente c ON p.id_cliente      = c.id_cliente " +
                     "JOIN pais pa   ON p.id_pais_destino = pa.id_pais " +
                     "JOIN moneda m  ON p.id_moneda       = m.id_moneda " +
                     // Un BORRADOR todavia lo esta armando el auxiliar.
                     // El cliente no tiene por que verlo.
                     "WHERE p.id_cliente = ? AND p.estado <> 'BORRADOR' " +
                     "ORDER BY p.id_pedido DESC";

        return jdbc.query(sql, this::convertirPedido, idCliente);
    }

    public boolean pedidoEsDelCliente(int idPedido, int idCliente) {
        // El BORRADOR se excluye aqui tambien: si no, un cliente podria
        // abrirlo escribiendo el numero en la direccion del navegador.
        String sql = "SELECT COUNT(*) FROM pedido " +
                     "WHERE id_pedido = ? AND id_cliente = ? AND estado <> 'BORRADOR'";
        Integer total = jdbc.queryForObject(sql, Integer.class, idPedido, idCliente);
        return total != null && total > 0;
    }

    // ----------------------------------------------------------
    // Respuesta del cliente a la cotizacion
    //
    // Las dos consultas llevan AND estado = 'COTIZADO'. Eso hace
    // que solo se pueda responder una vez: si el cliente le da dos
    // veces al boton, la segunda no cambia nada.
    // ----------------------------------------------------------

    @Transactional
    public boolean aceptarCotizacion(int idPedido) {

        int filas = jdbc.update(
                "UPDATE pedido SET estado = 'CREADO' " +
                "WHERE id_pedido = ? AND estado = 'COTIZADO'", idPedido);

        return filas > 0;
    }

    @Transactional
    public boolean rechazarCotizacion(int idPedido, String motivo) {

        // Primero se marca el pedido. Si no estaba COTIZADO, no se
        // cambia nada mas y los lotes siguen reservados.
        int filas = jdbc.update(
                "UPDATE pedido SET estado = 'RECHAZADO', observacion_cliente = ? " +
                "WHERE id_pedido = ? AND estado = 'COTIZADO'", motivo, idPedido);

        if (filas == 0) {
            return false;
        }

        // El cliente dijo que no: los lotes vuelven al inventario
        // para que otra empresa pueda pedirlos.
        jdbc.update("UPDATE lote SET estado = 'DISPONIBLE' " +
                    "WHERE id_lote IN (SELECT id_lote FROM pedido_lote WHERE id_pedido = ?)",
                idPedido);

        return true;
    }
}
