package br.com.servidor;

import javax.swing.SwingUtilities;

public class ServidorMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServidorGUI().setVisible(true);
        });
    }
}
