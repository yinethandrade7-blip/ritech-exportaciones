package ritech.exportaciones.modelo;

public class Usuario {

    private int id;
    private String nombre;
    private String correo;
    private String rol;
    private boolean activo;
    private int idRol;
    private Integer idProveedor;
    private Integer idCliente;
    private String vinculo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public int getIdRol() {
        return idRol;
    }

    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getVinculo() {
        return vinculo;
    }

    public void setVinculo(String vinculo) {
        this.vinculo = vinculo;
    }

    // =========================================================
    // QUIEN ES
    //
    // Estos metodos solo dicen que rol tiene el usuario.
    // Casi no se usan en el resto del programa: se usan abajo,
    // para construir los permisos.
    // =========================================================

    public boolean esAdministrador() {
        return "ADMINISTRADOR".equalsIgnoreCase(rol);
    }

    public boolean esGerente() {
        return "GERENTE".equalsIgnoreCase(rol);
    }

    public boolean esAuxiliar() {
        return "AUXILIAR".equalsIgnoreCase(rol);
    }

    public boolean esProveedor() {
        return "PROVEEDOR".equalsIgnoreCase(rol);
    }

    public boolean esCliente() {
        return "CLIENTE".equalsIgnoreCase(rol);
    }

    // =========================================================
    // QUE PUEDE HACER
    //
    // Esto es lo que usan los controladores y las pantallas.
    // El resto del programa nunca pregunta "quien eres",
    // pregunta "puedes hacer esto".
    //
    // Si manana cambia quien puede hacer que, se cambia aqui
    // y en ningun otro archivo.
    //
    // Principio de minimo privilegio: cada rol recibe los
    // permisos que necesita para su trabajo y ni uno mas.
    // =========================================================

    // --- Administrador del sistema: cuentas y datos maestros ---

    public boolean puedeAdministrarUsuarios() {
        return esAdministrador();
    }

    public boolean puedeEditarMaestros() {
        // Crear, modificar y eliminar proveedores y clientes
        return esAdministrador();
    }

    public boolean puedeVerMaestros() {
        // El auxiliar necesita ver proveedores y clientes para
        // poder registrar lotes y crear pedidos.
        // El gerente los ve para entender los reportes.
        return esAdministrador() || esGerente() || esAuxiliar();
    }

    // --- Auxiliar: operacion diaria ---

    public boolean puedeOperarLotes() {
        return esAuxiliar();
    }

    public boolean puedeOperarPedidos() {
        return esAuxiliar();
    }

    // --- Gerente: direccion ---

    public boolean puedeDespachar() {
        // Despachar compromete legalmente a la empresa ante la
        // Union Europea. Esa autorizacion es de la gerencia.
        return esGerente();
    }

    public boolean puedeVerReportes() {
        return esGerente();
    }

    public boolean puedeAutorizarPagos() {
        // Pagarle al productor es sacar plata de la empresa.
        // Esa decision es de la gerencia, igual que el despacho.
        return esGerente();
    }

    // --- Consulta ---

    public boolean puedeVerLotes() {
        return esGerente() || esAuxiliar();
    }

    public boolean puedeVerPedidos() {
        return esGerente() || esAuxiliar();
    }

    public boolean puedeConsultarOrigen() {
        return esGerente() || esAuxiliar() || esCliente();
    }
}
