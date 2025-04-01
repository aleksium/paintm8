package p.m.paintm8;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JMenuBar;

public class StyledMenuBar extends JMenuBar {

    private final Color bgColor = new Color(59, 11, 48);
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }
}
