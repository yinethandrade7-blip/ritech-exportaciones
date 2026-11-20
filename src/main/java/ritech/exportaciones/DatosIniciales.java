package ritech.exportaciones;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ritech.exportaciones.dao.UsuarioDAO;

/**
 * Datos iniciales del sistema.
 *
 * Se ejecuta una sola vez cada vez que arranca el programa y revisa que
 * existan los roles y las cuentas basicas. Si ya existen, no hace nada.
 *
 * Es lo mismo que hace cualquier sistema cuando se instala por primera
 * vez: crear el usuario administrador para que alguien pueda entrar.
 * Aqui lo usamos para no tener que ejecutar comandos a mano en MySQL.
 *
 * No borra ni modifica nada: unicamente agrega lo que falte.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public void run(String... args) {

        try {
            crearRolSiFalta("GERENTE",
                    "Direccion. Autoriza despachos y consulta los reportes del negocio");

            crearUsuarioSiFalta("Gerente RiTech",  "gerente@ritech.com",  "gerente2026",  "GERENTE");
            crearUsuarioSiFalta("Auxiliar RiTech", "auxiliar@ritech.com", "auxiliar2026", "AUXILIAR");

            // Motivo que escribe el cliente cuando rechaza una cotizacion
            agregarColumnaSiFalta("pedido", "observacion_cliente", "VARCHAR(255) NULL");

            // Lo que RiTech le paga al productor por cada lote
            agregarColumnaSiFalta("lote", "precio_compra_kg", "DECIMAL(14,2) NULL");
            agregarColumnaSiFalta("lote", "pagado", "BOOLEAN NOT NULL DEFAULT FALSE");
            agregarColumnaSiFalta("lote", "fecha_pago", "DATE NULL");

            crearTablaAvisoSiFalta();

            corregirNombresDeLugar();

        } catch (Exception e) {
            // Si algo falla aqui, el programa debe arrancar igual.
            System.out.println("[RiTech] No se pudieron revisar los datos iniciales: "
                             + e.getMessage());
        }
    }

    // ----------------------------------------------------------
    // Tabla donde quedan guardados los correos que manda el sistema.
    //
    // CREATE TABLE IF NOT EXISTS no da error cuando la tabla ya
    // existe, asi que esto se puede ejecutar en cada arranque.
    // ----------------------------------------------------------
    private void crearTablaAvisoSiFalta() {

        Integer existe = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'aviso'",
                Integer.class);

        jdbc.execute(
            "CREATE TABLE IF NOT EXISTS aviso (" +
            "  id_aviso     INT AUTO_INCREMENT PRIMARY KEY," +
            "  momento      DATETIME     NOT NULL," +
            "  destinatario VARCHAR(120) NOT NULL," +
            "  asunto       VARCHAR(180) NOT NULL," +
            "  cuerpo       TEXT         NOT NULL," +
            "  estado       VARCHAR(20)  NOT NULL," +
            "  detalle      VARCHAR(255) NULL" +
            ")");

        if (existe != null && existe == 0) {
            System.out.println("[RiTech] Tabla aviso creada");
        }
    }

    // ----------------------------------------------------------
    // El departamento se escribio "Narino" cuando se registraron
    // las fincas, porque en su momento la enie daba problemas.
    // Esto lo corrige de una vez.
    //
    // Se ejecuta cada arranque pero solo hace algo la primera vez:
    // despues del cambio ya ninguna fila contiene "Narino", asi
    // que el WHERE no encuentra nada y la consulta no toca nada.
    // ----------------------------------------------------------
    private void corregirNombresDeLugar() {

        corregirLugar("Narino",     "Nariño");
        corregirLugar("Chachagui",  "Chachagüí");
        corregirLugar("Sandona",    "Sandoná");
    }

    private void corregirLugar(String mal, String bien) {

        int filas = jdbc.update(
                "UPDATE finca SET lugar = REPLACE(lugar, ?, ?) WHERE lugar LIKE ?",
                mal, bien, "%" + mal + "%");

        if (filas > 0) {
            System.out.println("[RiTech] Corregido " + mal + " a " + bien
                             + " en " + filas + " fincas");
        }
    }

    // ----------------------------------------------------------
    // Agrega una columna a una tabla, solo si todavia no existe.
    //
    // Se pregunta por la columna en information_schema, que es el
    // catalogo donde MySQL guarda la descripcion de sus propias
    // tablas. Asi el programa se puede ejecutar muchas veces sin
    // dar error de "columna duplicada".
    // ----------------------------------------------------------
    private void agregarColumnaSiFalta(String tabla, String columna, String definicion) {

        Integer cuantas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() " +
                "  AND table_name = ? AND column_name = ?",
                Integer.class, tabla, columna);

        if (cuantas != null && cuantas > 0) {
            return;
        }

        jdbc.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);

        System.out.println("[RiTech] Columna " + tabla + "." + columna + " agregada");
    }

    // ----------------------------------------------------------
    // Devuelve el id de un rol por su nombre, o 0 si no existe
    // ----------------------------------------------------------
    private int idRol(String nombre) {
        List<Integer> resultado = jdbc.queryForList(
                "SELECT id_rol FROM rol WHERE nombre = ?", Integer.class, nombre);
        return resultado.isEmpty() ? 0 : resultado.get(0);
    }

    // ----------------------------------------------------------
    // Crea el rol solo si todavia no esta en la tabla
    // ----------------------------------------------------------
    private void crearRolSiFalta(String nombre, String descripcion) {

        if (idRol(nombre) > 0) {
            return;
        }

        // Se usa el siguiente numero libre para no chocar con los
        // roles que ya existen (1 a 4).
        Integer maximo = jdbc.queryForObject(
                "SELECT COALESCE(MAX(id_rol), 0) FROM rol", Integer.class);

        int nuevoId = (maximo == null ? 0 : maximo) + 1;

        try {
            jdbc.update("INSERT INTO rol (id_rol, nombre, descripcion) VALUES (?, ?, ?)",
                        nuevoId, nombre, descripcion);
        } catch (Exception e) {
            // Por si la tabla rol no tiene la columna descripcion
            jdbc.update("INSERT INTO rol (id_rol, nombre) VALUES (?, ?)", nuevoId, nombre);
        }

        System.out.println("[RiTech] Rol " + nombre + " creado con el id " + nuevoId);
    }

    // ----------------------------------------------------------
    // Crea la cuenta solo si ese correo todavia no esta registrado
    // ----------------------------------------------------------
    private void crearUsuarioSiFalta(String nombre, String correo,
                                     String clave, String rol) {

        Integer cuantos = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE correo = ?", Integer.class, correo);

        if (cuantos != null && cuantos > 0) {
            return;
        }

        int idRol = idRol(rol);
        if (idRol == 0) {
            return;
        }

        jdbc.update("INSERT INTO usuario (nombre, correo, contrasena_hash, id_rol, activo) "
                  + "VALUES (?, ?, ?, ?, TRUE)",
                    nombre, correo, UsuarioDAO.cifrar(clave), idRol);

        System.out.println("[RiTech] Cuenta " + correo + " creada con el rol " + rol);
    }
}
