package ritech.exportaciones.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ritech.exportaciones.modelo.DeudaProveedor;
import ritech.exportaciones.modelo.Lote;

/**
 * Consultas sobre la plata que RiTech le debe a los productores.
 *
 * Hasta el Sprint 3 el sistema solo manejaba la venta al comprador
 * europeo. Faltaba la otra mitad del negocio: la compra al
 * caficultor. Esto es esa mitad.
 */
@Repository
public class PagoDAO {

    @Autowired
    private JdbcTemplate jdbc;

    // Un lote solo cuenta para el pago si ya se sabe a que precio
    // se le compro. Los lotes sin precio quedan por fuera.
    private static final String CON_PRECIO = " l.precio_compra_kg IS NOT NULL ";

    // ----------------------------------------------------------
    // Resumen: cuanto se le debe a cada productor
    // ----------------------------------------------------------

    public List<DeudaProveedor> deudaPorProveedor() {

        String sql =
            "SELECT p.id_proveedor, p.nombre, " +
            "       MIN(f.nombre) AS finca, " +
            "       SUM(CASE WHEN l.pagado = FALSE THEN 1 ELSE 0 END) AS lotes_pendientes, " +
            "       COALESCE(SUM(CASE WHEN l.pagado = FALSE " +
            "                         THEN l.peso_kg ELSE 0 END), 0) AS kilos_pendientes, " +
            "       COALESCE(SUM(CASE WHEN l.pagado = FALSE " +
            "                         THEN l.peso_kg * l.precio_compra_kg " +
            "                         ELSE 0 END), 0) AS total_pendiente, " +
            "       COALESCE(SUM(CASE WHEN l.pagado = TRUE " +
            "                         THEN l.peso_kg * l.precio_compra_kg " +
            "                         ELSE 0 END), 0) AS total_pagado " +
            "FROM proveedor p " +
            "JOIN finca f ON f.id_proveedor = p.id_proveedor " +
            "JOIN lote  l ON l.id_finca     = f.id_finca " +
            "WHERE " + CON_PRECIO +
            "GROUP BY p.id_proveedor, p.nombre " +
            "ORDER BY total_pendiente DESC, p.nombre";

        return jdbc.query(sql, this::convertirDeuda);
    }

    private DeudaProveedor convertirDeuda(ResultSet rs, int fila) throws SQLException {
        DeudaProveedor d = new DeudaProveedor();
        d.setIdProveedor(rs.getInt("id_proveedor"));
        d.setNombre(rs.getString("nombre"));
        d.setFinca(rs.getString("finca"));
        d.setLotesPendientes(rs.getInt("lotes_pendientes"));
        d.setKilosPendientes(rs.getDouble("kilos_pendientes"));
        d.setTotalPendiente(rs.getDouble("total_pendiente"));
        d.setTotalPagado(rs.getDouble("total_pagado"));
        return d;
    }

    // ----------------------------------------------------------
    // Totales generales, para el encabezado de la pantalla
    // ----------------------------------------------------------

    public double totalPendiente() {
        String sql = "SELECT COALESCE(SUM(l.peso_kg * l.precio_compra_kg), 0) " +
                     "FROM lote l WHERE " + CON_PRECIO + " AND l.pagado = FALSE";
        Double total = jdbc.queryForObject(sql, Double.class);
        return total == null ? 0 : total;
    }

    public double totalPagado() {
        String sql = "SELECT COALESCE(SUM(l.peso_kg * l.precio_compra_kg), 0) " +
                     "FROM lote l WHERE " + CON_PRECIO + " AND l.pagado = TRUE";
        Double total = jdbc.queryForObject(sql, Double.class);
        return total == null ? 0 : total;
    }

    public int lotesSinPrecio() {
        String sql = "SELECT COUNT(*) FROM lote l WHERE l.precio_compra_kg IS NULL";
        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total == null ? 0 : total;
    }

    // ----------------------------------------------------------
    // Detalle de lotes
    // ----------------------------------------------------------

    private static final String SELECT_LOTE =
        "SELECT l.id_lote, l.codigo, prod.nombre AS producto, v.nombre AS variedad, " +
        "       l.peso_kg, l.fecha_cosecha, l.estado, " +
        "       l.precio_compra_kg, l.pagado, l.fecha_pago, " +
        "       p.nombre AS proveedor, f.nombre AS finca " +
        "FROM lote l " +
        "JOIN variedad v    ON v.id_variedad  = l.id_variedad " +
        "JOIN producto prod ON prod.id_producto = v.id_producto " +
        "JOIN finca f       ON f.id_finca     = l.id_finca " +
        "JOIN proveedor p   ON p.id_proveedor = f.id_proveedor ";

    private Lote convertirLote(ResultSet rs, int fila) throws SQLException {
        Lote l = new Lote();
        l.setId(rs.getInt("id_lote"));
        l.setCodigo(rs.getString("codigo"));
        l.setProducto(rs.getString("producto"));
        l.setVariedad(rs.getString("variedad"));
        l.setPesoKg(rs.getDouble("peso_kg"));
        l.setFechaCosecha(String.valueOf(rs.getDate("fecha_cosecha")));
        l.setEstado(rs.getString("estado"));
        l.setProveedor(rs.getString("proveedor"));
        l.setFinca(rs.getString("finca"));

        double precio = rs.getDouble("precio_compra_kg");
        l.setPrecioCompraKg(rs.wasNull() ? null : precio);

        l.setPagado(rs.getBoolean("pagado"));
        l.setFechaPago(rs.getString("fecha_pago"));

        return l;
    }

    public List<Lote> lotesPendientes() {
        String sql = SELECT_LOTE +
                     "WHERE " + CON_PRECIO + " AND l.pagado = FALSE " +
                     "ORDER BY p.nombre, l.codigo";
        return jdbc.query(sql, this::convertirLote);
    }

    public List<Lote> lotesPagados() {
        String sql = SELECT_LOTE +
                     "WHERE " + CON_PRECIO + " AND l.pagado = TRUE " +
                     "ORDER BY l.fecha_pago DESC, l.codigo";
        return jdbc.query(sql, this::convertirLote);
    }

    // ----------------------------------------------------------
    // Registrar el pago
    //
    // La consulta lleva AND pagado = FALSE para que un doble clic
    // no vuelva a escribir la fecha de un pago ya registrado.
    // ----------------------------------------------------------

    public boolean registrarPago(int idLote) {

        int filas = jdbc.update(
                "UPDATE lote SET pagado = TRUE, fecha_pago = CURDATE() " +
                "WHERE id_lote = ? AND pagado = FALSE " +
                "  AND precio_compra_kg IS NOT NULL", idLote);

        return filas > 0;
    }

    public String codigoDe(int idLote) {
        List<String> encontrados = jdbc.queryForList(
                "SELECT codigo FROM lote WHERE id_lote = ?", String.class, idLote);
        return encontrados.isEmpty() ? null : encontrados.get(0);
    }
}
