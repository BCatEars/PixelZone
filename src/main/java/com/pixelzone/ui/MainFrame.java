package com.pixelzone.ui;

import com.pixelzone.security.Permisos;
import com.pixelzone.session.UserSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Ventana principal con navegacion lateral por modulos (arbol) y un
 * {@link CardLayout} a la derecha. Cada item solo se agrega si la sesion
 * tiene el permiso requerido (RBAC de interfaz).
 */
public class MainFrame extends JFrame {

    private final UserSession sesion;
    private final Runnable alCerrarSesion;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel tarjetas = new JPanel(cardLayout);
    private final JLabel etiquetaEstado = new JLabel("Sesion iniciada.");
    private final Map<DefaultMutableTreeNode, String> tarjetaPorNodo = new HashMap<>();
    private int contadorTarjetas = 0;

    public MainFrame(UserSession sesion, Runnable alCerrarSesion) {
        this.sesion = sesion;
        this.alCerrarSesion = alCerrarSesion;
        construir();
    }

    private void construir() {
        setTitle(titulo());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);

        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("PixelZone");

        DefaultMutableTreeNode caja = modulo(raiz, "Caja");
        item(caja, "Punto de Venta", s -> s.tienePermiso(Permisos.GESTION_VENTAS),
                () -> new PanelPuntoDeVenta(sesion, this));
        item(caja, "Rentas", s -> s.tienePermiso(Permisos.GESTION_RENTAS),
                () -> new PanelRentas(sesion, this));
        item(caja, "Apartados", s -> s.tienePermiso(Permisos.GESTION_VENTAS),
                () -> new PanelApartados(sesion, this));

        DefaultMutableTreeNode inventario = modulo(raiz, "Inventario");
        item(inventario, "Productos", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelProductos(sesion, this));
        item(inventario, "Ejemplares", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelEjemplares(sesion, this));
        item(inventario, "Proveedores", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelProveedores(sesion, this));
        item(inventario, "Compra de usados", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelCompraUsado(sesion, this));

        DefaultMutableTreeNode actores = modulo(raiz, "Actores");
        item(actores, "Clientes", s -> s.tienePermiso(Permisos.GESTION_VENTAS),
                () -> new PanelClientes(sesion, this));
        item(actores, "Usuarios", UserSession::esAdmin,
                () -> new PanelUsuarios(sesion, this));

        DefaultMutableTreeNode sql = modulo(raiz, "Consulta SQL");
        item(sql, "Vistas", s -> true, () -> new PanelVistas(this));
        item(sql, "Procedimientos", s -> true, () -> new PanelProcedimientos(this));
        item(sql, "Funciones", s -> true, () -> new PanelFunciones(this));

        quitarModulosVacios(raiz);

        JTree arbol = new JTree(raiz);
        arbol.setRootVisible(false);
        arbol.setShowsRootHandles(true);
        arbol.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        for (int i = 0; i < arbol.getRowCount(); i++) {
            arbol.expandRow(i);
        }
        arbol.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) arbol.getLastSelectedPathComponent();
            if (nodo != null && nodo.isLeaf()) {
                String tarjeta = tarjetaPorNodo.get(nodo);
                if (tarjeta != null) {
                    cardLayout.show(tarjetas, tarjeta);
                }
            }
        });

        JScrollPane scrollNavegacion = new JScrollPane(arbol);
        scrollNavegacion.setPreferredSize(new Dimension(220, 0));
        scrollNavegacion.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        add(scrollNavegacion, BorderLayout.WEST);
        add(tarjetas, BorderLayout.CENTER);
        add(construirBarraEstado(), BorderLayout.SOUTH);

        seleccionarPrimeraHoja(arbol, raiz);
    }

    private DefaultMutableTreeNode modulo(DefaultMutableTreeNode padre, String titulo) {
        DefaultMutableTreeNode nodo = new DefaultMutableTreeNode(titulo);
        padre.add(nodo);
        return nodo;
    }

    private void item(DefaultMutableTreeNode modulo, String etiqueta,
                      Predicate<UserSession> puede, Supplier<JComponent> fabrica) {
        if (!puede.test(sesion)) {
            return;
        }
        String nombreTarjeta = "card-" + (contadorTarjetas++);
        tarjetas.add(fabrica.get(), nombreTarjeta);
        DefaultMutableTreeNode hoja = new DefaultMutableTreeNode(etiqueta);
        modulo.add(hoja);
        tarjetaPorNodo.put(hoja, nombreTarjeta);
    }

    private void quitarModulosVacios(DefaultMutableTreeNode raiz) {
        for (int i = raiz.getChildCount() - 1; i >= 0; i--) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) raiz.getChildAt(i);
            if (hijo.getChildCount() == 0) {
                raiz.remove(i);
            }
        }
    }

    private void seleccionarPrimeraHoja(JTree arbol, DefaultMutableTreeNode raiz) {
        DefaultMutableTreeNode primera = primeraHoja(raiz);
        if (primera != null) {
            TreePath path = new TreePath(primera.getPath());
            arbol.setSelectionPath(path);
            arbol.scrollPathToVisible(path);
        }
    }

    private DefaultMutableTreeNode primeraHoja(DefaultMutableTreeNode nodo) {
        if (nodo.isLeaf()) {
            return nodo;
        }
        for (int i = 0; i < nodo.getChildCount(); i++) {
            DefaultMutableTreeNode encontrada =
                    primeraHoja((DefaultMutableTreeNode) nodo.getChildAt(i));
            if (encontrada != null) {
                return encontrada;
            }
        }
        return null;
    }

    private JPanel construirBarraEstado() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        barra.add(etiquetaEstado);
        JButton cerrarSesion = new JButton("Cambiar usuario / Cerrar sesion");
        cerrarSesion.addActionListener(e -> cerrarSesion());
        barra.add(cerrarSesion);
        return barra;
    }

    private void cerrarSesion() {
        dispose();
        if (alCerrarSesion != null) {
            alCerrarSesion.run();
        }
    }

    private String titulo() {
        return "PIXEL ZONE - " + sesion.getUsuario().getNombreCompleto()
                + " (" + sesion.getUsuario().getNombrePerfil() + ")"
                + " - permisos: " + sesion.resumenPermisos();
    }

    public void setMensaje(String mensaje) {
        etiquetaEstado.setText(mensaje);
    }
}
