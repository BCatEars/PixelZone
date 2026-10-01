package com.pixelzone.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.pixelzone.security.Permisos;
import com.pixelzone.session.UserSession;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Ventana principal. Navegacion en dos niveles: un {@link JTabbedPane} superior
 * por modulo (Caja, Inventario, Actores, Consulta SQL) y, dentro de cada
 * modulo, una lista lateral con {@link CardLayout}. Cada item solo se agrega si
 * la sesion tiene el permiso requerido (RBAC de interfaz).
 *
 * <p>Al mostrar un panel se llama {@link Recargable#recargar()} para no mostrar
 * datos obsoletos.</p>
 */
public class MainFrame extends JFrame {

    private final UserSession sesion;
    private final Runnable alCerrarSesion;
    private final JTabbedPane modulos = new JTabbedPane();
    private final JLabel etiquetaEstado = new JLabel("Sesion iniciada.");
    private boolean temaOscuro = false;

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

        ModuloPanel caja = new ModuloPanel();
        agregar(caja, "Punto de Venta", s -> s.tienePermiso(Permisos.GESTION_VENTAS),
                () -> new PanelPuntoDeVenta(sesion, this));
        agregar(caja, "Rentas", s -> s.tienePermiso(Permisos.GESTION_RENTAS),
                () -> new PanelRentas(sesion, this));
        agregar(caja, "Devoluciones de renta", s -> s.tienePermiso(Permisos.GESTION_RENTAS),
                () -> new PanelDevolucionesRenta(sesion, this));
        agregar(caja, "Apartados", s -> s.tienePermiso(Permisos.GESTION_VENTAS),
                () -> new PanelApartados(sesion, this));

        ModuloPanel inventario = new ModuloPanel();
        agregar(inventario, "Productos", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelProductos(sesion, this));
        agregar(inventario, "Ejemplares", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelEjemplares(sesion, this));
        agregar(inventario, "Compras a proveedor", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelCompras(sesion, this));
        agregar(inventario, "Compra de usados", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelCompraUsado(sesion, this));
        agregar(inventario, "Proveedores", s -> s.tienePermiso(Permisos.GESTION_INVENTARIO),
                () -> new PanelProveedores(sesion, this));

        ModuloPanel actores = new ModuloPanel();
        agregar(actores, "Clientes", s -> s.tienePermiso(Permisos.GESTION_VENTAS),
                () -> new PanelClientes(sesion, this));
        agregar(actores, "Usuarios", UserSession::esAdmin,
                () -> new PanelUsuarios(sesion, this));

        ModuloPanel consultas = new ModuloPanel();
        agregar(consultas, "Vistas", s -> true, () -> new PanelVistas(this));
        agregar(consultas, "Procedimientos", s -> true, () -> new PanelProcedimientos(this));
        agregar(consultas, "Funciones", s -> true, () -> new PanelFunciones(this));

        tab(modulos, "Caja", caja);
        tab(modulos, "Inventario", inventario);
        tab(modulos, "Actores", actores);
        tab(modulos, "Consulta SQL", consultas);

        modulos.addChangeListener(e -> {
            Component seleccionado = modulos.getSelectedComponent();
            if (seleccionado instanceof ModuloPanel modulo) {
                modulo.recargarActual();
            }
        });

        add(modulos, BorderLayout.CENTER);
        add(construirBarraEstado(), BorderLayout.SOUTH);
    }

    private void tab(JTabbedPane tabs, String titulo, ModuloPanel modulo) {
        if (!modulo.vacio()) {
            tabs.addTab(titulo, modulo);
        }
    }

    private void agregar(ModuloPanel modulo, String etiqueta, Predicate<UserSession> puede,
                         Supplier<JComponent> fabrica) {
        if (puede.test(sesion)) {
            modulo.agregar(etiqueta, fabrica.get());
        }
    }

    private JPanel construirBarraEstado() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        barra.add(etiquetaEstado);
        JButton tema = new JButton("Tema claro/oscuro");
        tema.addActionListener(e -> alternarTema());
        barra.add(tema);
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

    private void alternarTema() {
        try {
            UIManager.setLookAndFeel(temaOscuro ? new FlatLightLaf() : new FlatDarkLaf());
            temaOscuro = !temaOscuro;
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            setMensaje("No se pudo cambiar el tema: " + ex.getMessage());
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

    /**
     * Modulo: lista lateral de items + {@link CardLayout} con sus paneles.
     */
    private static final class ModuloPanel extends JPanel {

        private final CardLayout cardLayout = new CardLayout();
        private final JPanel tarjetas = new JPanel(cardLayout);
        private final DefaultListModel<String> modeloLista = new DefaultListModel<>();
        private final JList<String> lista = new JList<>(modeloLista);
        private final List<JComponent> componentes = new ArrayList<>();
        private int contador = 0;

        ModuloPanel() {
            setLayout(new BorderLayout());
            lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            lista.setFixedCellWidth(180);
            lista.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    mostrar();
                }
            });
            JScrollPane scroll = new JScrollPane(lista);
            scroll.setPreferredSize(new Dimension(190, 0));
            scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
            add(scroll, BorderLayout.WEST);
            add(tarjetas, BorderLayout.CENTER);
        }

        void agregar(String etiqueta, JComponent componente) {
            String nombre = "card-" + (contador++);
            tarjetas.add(componente, nombre);
            modeloLista.addElement(etiqueta);
            componentes.add(componente);
            if (modeloLista.getSize() == 1) {
                lista.setSelectedIndex(0);
            }
        }

        boolean vacio() {
            return modeloLista.isEmpty();
        }

        void mostrar() {
            int i = lista.getSelectedIndex();
            if (i < 0) {
                return;
            }
            cardLayout.show(tarjetas, "card-" + i);
            recargar(componentes.get(i));
        }

        void recargarActual() {
            int i = lista.getSelectedIndex();
            if (i >= 0 && i < componentes.size()) {
                recargar(componentes.get(i));
            }
        }

        private void recargar(JComponent componente) {
            if (componente instanceof Recargable recargable) {
                recargable.recargar();
            }
        }
    }
}
