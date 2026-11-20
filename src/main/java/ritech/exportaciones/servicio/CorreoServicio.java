package ritech.exportaciones.servicio;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Manda un correo de verdad, por SMTP.
 *
 * La configuracion no esta aqui sino en el archivo correo.properties,
 * y ese archivo NO se sube al repositorio (esta en el .gitignore).
 * La razon: una clave de aplicacion de Gmail sirve para mandar correos
 * en nombre de esa cuenta. Si se sube a un repositorio publico,
 * cualquiera puede mandar correos haciendose pasar por la empresa.
 *
 * Si el archivo no existe, o la clave esta vacia, el sistema NO se
 * cae: esta clase avisa que no esta configurada y el aviso queda
 * guardado como PENDIENTE para que alguien lo revise.
 */
@Service
public class CorreoServicio {

    @Value("${correo.servidor:smtp.gmail.com}")
    private String servidor;

    @Value("${correo.puerto:465}")
    private int puerto;

    @Value("${correo.usuario:}")
    private String usuario;

    @Value("${correo.clave:}")
    private String clave;

    @Value("${correo.remitente:RiTech S.A.S}")
    private String remitente;

    /**
     * Devuelve false cuando todavia no se ha puesto la cuenta de correo.
     * Las pantallas usan esto para explicar por que los avisos quedan
     * pendientes, en vez de dejar al usuario adivinando.
     */
    public boolean estaConfigurado() {
        return !usuario.isBlank() && !clave.isBlank();
    }

    public String cuenta() {
        return usuario;
    }

    /**
     * Manda el correo. Si algo falla lanza la excepcion, para que quien
     * llama la guarde en el aviso; no se la traga en silencio.
     */
    public void enviar(String destinatario, String asunto, String cuerpo) {

        if (!estaConfigurado()) {
            throw new IllegalStateException(
                "La cuenta de correo no esta configurada en correo.properties");
        }

        JavaMailSenderImpl envio = new JavaMailSenderImpl();

        envio.setHost(servidor);
        envio.setPort(puerto);
        envio.setUsername(usuario);
        envio.setPassword(clave);
        envio.setDefaultEncoding("UTF-8");

        Properties opciones = envio.getJavaMailProperties();
        opciones.put("mail.transport.protocol", "smtp");
        opciones.put("mail.smtp.auth", "true");
        opciones.put("mail.smtp.ssl.enable", "true");
        opciones.put("mail.smtp.connectiontimeout", "10000");
        opciones.put("mail.smtp.timeout", "10000");
        opciones.put("mail.smtp.writetimeout", "10000");

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente + " <" + usuario + ">");
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        envio.send(mensaje);
    }
}
