
import com.formdev.flatlaf.FlatDarkLaf;
import ui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ScrollBar.width", 8);
            UIManager.put("TabbedPane.selectedBackground", new Color(0x2D2D3F));
        } catch (Exception e) {
            System.err.println("FlatLaf failed to load, falling back to default.");
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}