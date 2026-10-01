package com.pixelzone.ui;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Combo editable con filtro incremental. Conserva la lista completa y muestra
 * solo los items cuya representacion ({@code displayFn}) contiene el texto
 * tecleado (case-insensitive).
 *
 * <ul>
 *   <li>Enter selecciona el primer item filtrado.</li>
 *   <li>Escape limpia el filtro.</li>
 *   <li>Al perder foco se restaura el texto del item seleccionado.</li>
 *   <li>{@link #setItems(List)} y {@link #preseleccionar(Predicate)} conservan la
 *       lista completa y no disparan filtrado.</li>
 * </ul>
 *
 * <p>Los cambios programaticos ({@link #setSelectedItem}, {@link #setSelectedIndex},
 * {@code setItems}, {@code preseleccionar}) se ejecutan con {@code ignorarFiltro}
 * activo e invalidan cualquier filtrado pendiente: si no, al elegir una fila el
 * modelo quedaba reducido al item seleccionado y ya no se podia cambiar a otro.</p>
 */
public class ComboBuscable<T> extends JComboBox<T> {

    private final Function<T, String> displayFn;
    private final List<T> todos = new ArrayList<>();
    private final DefaultComboBoxModel<T> modelo = new DefaultComboBoxModel<>();
    private final DocumentListener listener;
    private boolean filtroPendiente = false;
    private boolean ignorarFiltro = false;
    private int generacion = 0;

    public ComboBuscable(Function<T, String> displayFn) {
        this.displayFn = displayFn;
        setModel(modelo);
        setEditable(true);
        setMaximumRowCount(15);
        setRenderer(new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value == null ? "" : ComboBuscable.this.displayFn.apply((T) value));
                return this;
            }
        });

        listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        };
        editor().getDocument().addDocumentListener(listener);
        editor().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    seleccionarPrimero();
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    limpiarFiltro();
                    e.consume();
                }
            }
        });
        editor().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                sincronizarEditor();
            }
        });
    }

    /** Reemplaza la lista completa conservando la seleccion si sigue presente. */
    public void setItems(List<T> items) {
        T previo = getSelectedItem();
        todos.clear();
        if (items != null) {
            todos.addAll(items);
        }
        programatico(() -> {
            modelo.removeAllElements();
            for (T item : todos) {
                modelo.addElement(item);
            }
            if (previo != null && todos.contains(previo)) {
                modelo.setSelectedItem(previo);
            } else if (modelo.getSize() > 0) {
                modelo.setSelectedItem(modelo.getElementAt(0));
            }
            sincronizarEditorSinListener();
        });
    }

    /** Restaura la lista completa y selecciona el primer item que cumpla el filtro. */
    public void preseleccionar(Predicate<T> filtro) {
        T objetivo = null;
        for (T item : todos) {
            if (filtro.test(item)) {
                objetivo = item;
                break;
            }
        }
        if (objetivo == null) {
            return;
        }
        final T seleccion = objetivo;
        programatico(() -> {
            modelo.removeAllElements();
            for (T item : todos) {
                modelo.addElement(item);
            }
            modelo.setSelectedItem(seleccion);
            sincronizarEditorSinListener();
        });
    }

    @Override
    public void setSelectedItem(Object anObject) {
        programatico(() -> super.setSelectedItem(anObject));
    }

    @Override
    public void setSelectedIndex(int anIndex) {
        programatico(() -> super.setSelectedIndex(anIndex));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getSelectedItem() {
        return (T) modelo.getSelectedItem();
    }

    private void filtrar() {
        if (ignorarFiltro || filtroPendiente) {
            return;
        }
        filtroPendiente = true;
        int miGeneracion = ++generacion;
        SwingUtilities.invokeLater(() -> {
            filtroPendiente = false;
            if (miGeneracion != generacion) {
                return; // un cambio programatico invalido este filtrado
            }
            aplicarFiltro();
        });
    }

    private void aplicarFiltro() {
        JTextField editor = editor();
        String texto = editor.getText();
        T actual = getSelectedItem();
        programatico(() -> {
            modelo.removeAllElements();
            for (T item : todos) {
                if (coincide(item, texto)) {
                    modelo.addElement(item);
                }
            }
            if (actual != null && coincide(actual, texto)) {
                modelo.setSelectedItem(actual);
            } else if (modelo.getSize() > 0) {
                modelo.setSelectedItem(modelo.getElementAt(0));
            }
            editor.setText(texto);
            editor.setCaretPosition(texto.length());
        });
        if (modelo.getSize() > 0 && isShowing()) {
            showPopup();
        }
    }

    private void seleccionarPrimero() {
        if (modelo.getSize() > 0) {
            programatico(() -> {
                modelo.setSelectedItem(modelo.getElementAt(0));
                sincronizarEditorSinListener();
            });
            hidePopup();
        }
    }

    private void limpiarFiltro() {
        setItems(new ArrayList<>(todos));
    }

    private void sincronizarEditor() {
        programatico(this::sincronizarEditorSinListener);
    }

    private void sincronizarEditorSinListener() {
        T actual = getSelectedItem();
        String texto = actual == null ? "" : displayFn.apply(actual);
        editor().setText(texto);
    }

    /**
     * Ejecuta un cambio programatico sin disparar filtrado e invalida cualquier
     * filtrado que hubiera quedado pendiente en el EDT.
     */
    private void programatico(Runnable cambio) {
        boolean previo = ignorarFiltro;
        ignorarFiltro = true;
        generacion++;
        try {
            cambio.run();
        } finally {
            ignorarFiltro = previo;
        }
    }

    private boolean coincide(T item, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String representacion = displayFn.apply(item);
        return representacion != null
                && representacion.toLowerCase().contains(texto.trim().toLowerCase());
    }

    private JTextField editor() {
        return (JTextField) getEditor().getEditorComponent();
    }
}
