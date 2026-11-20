package ritech.exportaciones.dao;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteDAO {

    @Autowired
    private JdbcTemplate jdbc;

    public Map<String, Object> resumenGeneral() {

        String sql =
            "SELECT " +
            " (SELECT COUNT(*) FROM lote) AS total_lotes, " +
            " (SELECT COALESCE(SUM(peso_kg),0) FROM lote WHERE estado='DISPONIBLE') AS kg_disponible, " +
            " (SELECT COALESCE(SUM(peso_kg),0) FROM lote WHERE estado='RESERVADO')  AS kg_reservado, " +
            " (SELECT COALESCE(SUM(peso_kg),0) FROM lote WHERE estado='DESPACHADO') AS kg_despachado, " +
            " (SELECT COUNT(*) FROM pedido) AS total_pedidos, " +
            " (SELECT COUNT(*) FROM pedido WHERE estado='DESPACHADO') AS pedidos_despachados, " +
            " (SELECT COALESCE(SUM(total * tasa_cambio),0) FROM pedido WHERE estado='DESPACHADO') AS valor_cop, " +
            " (SELECT COUNT(*) FROM proveedor) AS total_proveedores, " +
            " (SELECT COUNT(*) FROM cliente)   AS total_clientes";

        return jdbc.queryForMap(sql);
    }

    public List<Map<String, Object>> porPaisDestino() {

        String sql =
            "SELECT pa.nombre AS pais, " +
            "       COUNT(DISTINCT p.id_pedido) AS pedidos, " +
            "       COALESCE(SUM(l.peso_kg), 0) AS kilos, " +
            "       COALESCE(SUM(pl.total_parcial * p.tasa_cambio), 0) AS valor_cop " +
            "FROM pedido p " +
            "JOIN pais pa ON p.id_pais_destino = pa.id_pais " +
            "LEFT JOIN pedido_lote pl ON p.id_pedido = pl.id_pedido " +
            "LEFT JOIN lote l         ON pl.id_lote  = l.id_lote " +
            "GROUP BY pa.nombre " +
            "ORDER BY kilos DESC";

        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> porProveedor() {

        String sql =
            "SELECT pr.nombre AS proveedor, " +
            "       COALESCE(f.nombre, 'Sin finca') AS finca, " +
            "       COUNT(l.id_lote) AS lotes, " +
            "       COALESCE(SUM(l.peso_kg), 0) AS kilos, " +
            "       COALESCE(SUM(CASE WHEN l.estado='DESPACHADO' THEN l.peso_kg ELSE 0 END), 0) AS kilos_exportados " +
            "FROM proveedor pr " +
            "LEFT JOIN finca f ON pr.id_proveedor = f.id_proveedor " +
            "LEFT JOIN lote l  ON f.id_finca      = l.id_finca " +
            "GROUP BY pr.id_proveedor, pr.nombre, f.nombre " +
            "ORDER BY kilos DESC";

        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> porProducto() {

        String sql =
            "SELECT prod.nombre AS producto, v.nombre AS variedad, " +
            "       COUNT(l.id_lote) AS lotes, " +
            "       COALESCE(SUM(l.peso_kg), 0) AS kilos " +
            "FROM lote l " +
            "JOIN variedad v    ON l.id_variedad = v.id_variedad " +
            "JOIN producto prod ON v.id_producto = prod.id_producto " +
            "GROUP BY prod.nombre, v.nombre " +
            "ORDER BY kilos DESC";

        return jdbc.queryForList(sql);
    }

    public List<Map<String, Object>> cumplimientoEudr() {

        String sql =
            "SELECT pr.nombre AS proveedor, f.nombre AS finca, f.lugar, " +
            "       f.latitud, f.longitud, " +
            "       CASE WHEN f.latitud IS NULL OR f.longitud IS NULL " +
            "            THEN 'PENDIENTE' ELSE 'VERIFICADA' END AS estado, " +
            "       COALESCE((SELECT SUM(peso_kg) FROM lote WHERE id_finca = f.id_finca), 0) AS kilos " +
            "FROM finca f " +
            "JOIN proveedor pr ON f.id_proveedor = pr.id_proveedor " +
            "ORDER BY estado, pr.nombre";

        return jdbc.queryForList(sql);
    }
}
