package org.example;


import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.example.GUI.FileChooser;

public class Main {
    public static void main(String[] args) {
        try {
            // Establecer el Look and Feel de Windows
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        
        // Se inicia una instancia del FileChooser para elegir el archivo JSON
        FileChooser fileChooser = new FileChooser();
        fileChooser.setVisible(true);

    }
}