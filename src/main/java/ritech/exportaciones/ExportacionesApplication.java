package ritech.exportaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

/**
 * Arranque del sistema.
 *
 * El @PropertySource carga la configuracion de la cuenta de correo
 * desde un archivo aparte. Va aparte, y no en application.properties,
 * porque ese archivo no se sube al repositorio: lleva la clave con
 * la que se mandan los correos.
 *
 * ignoreResourceNotFound = true significa que si el archivo no esta,
 * el sistema arranca igual. Quien clone el proyecto desde GitHub lo
 * va a encontrar funcionando, solo que sin mandar correos hasta que
 * ponga su propia cuenta.
 */
@SpringBootApplication
@PropertySource(value = "classpath:correo.properties", ignoreResourceNotFound = true)
public class ExportacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExportacionesApplication.class, args);
    }

}
