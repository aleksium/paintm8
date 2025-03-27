package p.m.paintm8;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;

public final class Canvas extends JPanel {

    private final Color[] COLOR_PALETTE = new Color[]{Color.YELLOW, Color.YELLOW, Color.ORANGE, Color.MAGENTA, Color.RED, Color.MAGENTA, Color.YELLOW, Color.WHITE, Color.LIGHT_GRAY, Color.BLACK};

    private final BufferedImage panelImage = new BufferedImage(Environment.CANVAS_WIDTH, Environment.CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final Graphics2D panel = panelImage.createGraphics();

    private final ClientData outgoingPaint;

    private int x1 = 0;
    private int x2 = 0;
    private int y1 = 0;
    private int y2 = 0;

    private boolean drawing = false;

    public Canvas(ClientData outgoingPaint) {
        super(true);

        this.outgoingPaint = outgoingPaint;

        panel.setStroke(new BasicStroke(5));
        panel.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground();
        addHandlers();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Rectangle box = graphics.getClipBounds();
        
        graphics.drawImage(panelImage, box.x, box.y,
                box.x + box.width, box.y + box.height,
                box.x, box.y,
                box.x + box.width, box.y + box.height,
                null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Environment.CANVAS_WIDTH, Environment.CANVAS_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void drawLines(List<Line> additionalLines) {
        for (var line : additionalLines) {
            var index = line.c[4];
            if (index < 0 || index >= COLOR_PALETTE.length) {
                continue;
            }
            panel.setColor(COLOR_PALETTE[index]);

            panel.drawLine(line.c[0], line.c[1], line.c[2], line.c[3]);
        }
        repaint(determineBoundingBox(additionalLines));
    }

    private void addHandlers() {
        addMouseListener(
                new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (inBounds(e)) {
                    drawing = true;
                    x1 = e.getX();
                    y1 = e.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawing = false;
            }
        });

        addMouseMotionListener(
                new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDrag(e);
            }
        });
    }

    public void drawBackground() {
        panel.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        var random = new Random(123);
        var gray1 = Color.getHSBColor(0, 0, 0.35f);
        var gray2 = Color.getHSBColor(0, 0, 0.38f);
        for (int w = 0; w < Environment.CANVAS_WIDTH; w++) {
            for (int h = 0; h < Environment.CANVAS_HEIGHT; h++) {
                panel.setColor(random.nextBoolean() ? gray1 : gray2);
                panel.drawRect(w, h, 1, 1);
            }
        }
        panel.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));

        repaint();
    }

    private Rectangle determineBoundingBox(Collection<Line> lines) {
        Rectangle rectangle = null;
        if (lines == null || lines.isEmpty()) {
            return new Rectangle();
        }
        for (var line : lines) {
            if (rectangle == null) {
                rectangle = new Rectangle(new Point(line.c[0], line.c[1]));
                rectangle.add(new Point(line.c[2], line.c[3]));
            } else {
                rectangle.add(new Point(line.c[0], line.c[1]));
                rectangle.add(new Point(line.c[2], line.c[3]));
            }
        }
        rectangle.grow(3, 3);

        return rectangle;
    }

    private void handleMouseDrag(MouseEvent e) {
        if (!drawing) {
            return;
        }
        if (inBounds(e)) {
            x2 = e.getX();
            y2 = e.getY();

            panel.setColor(Color.YELLOW);
            panel.drawLine(x1, y1, x2, y2);
            Line line = new Line(x1, y1, x2, y2, 0);
            outgoingPaint.addLine(line);

            repaint(determineBoundingBox(Collections.singleton(line)));

            x1 = x2;
            y1 = y2;
        }
    }

    private boolean inBounds(MouseEvent e) {
        return e.getX() < Environment.CANVAS_WIDTH && e.getY() < Environment.CANVAS_HEIGHT;
    }
}
