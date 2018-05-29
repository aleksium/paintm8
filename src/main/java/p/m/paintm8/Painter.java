package p.m.paintm8;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;

public class Painter extends JPanel {

    public static final int PIXEL_WIDTH = 700;
    public static final int PIXEL_HEIGHT = 450;

    public final int STATUS_BAR_LENGTH = PIXEL_WIDTH;
    public final int STATUS_BAR_HEIGHT = 20;

    private final BufferedImage panelImage = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final Graphics2D panel = panelImage.createGraphics();
    private Rectangle windowOfChange = new Rectangle(0, 0, PIXEL_WIDTH, PIXEL_HEIGHT);

    private final ClientData outgoingPaint;

    private int x1 = 0;
    private int x2 = 0;
    private int y1 = 0;
    private int y2 = 0;

    private boolean drawing = false;

    public Painter(ClientData outgoingPaint) {
        super(true);

        this.outgoingPaint = outgoingPaint;

        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        setPreferredSize(new Dimension(PIXEL_WIDTH, PIXEL_HEIGHT));

        panel.setColor(Color.ORANGE);
        panel.setStroke(new BasicStroke(5));

        addHandlers();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        // graphics.drawImage(panelImage, 0, 0, PIXEL_WIDTH, PIXEL_HEIGHT, null);
        graphics.drawImage(panelImage, windowOfChange.x, windowOfChange.y, 
                windowOfChange.x + windowOfChange.width, windowOfChange.y + windowOfChange.height,
                windowOfChange.x, windowOfChange.y, 
                windowOfChange.x + windowOfChange.width, windowOfChange.y + windowOfChange.height,
                null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PIXEL_WIDTH, PIXEL_HEIGHT);
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
            panel.drawLine(line.c[0], line.c[1], line.c[2], line.c[3]);
        }
        windowOfChange = ascertainWindowOfChange(additionalLines);
        repaint(windowOfChange);
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

    private Rectangle ascertainWindowOfChange(Collection<Line> lines) {
        var rectangle = new Rectangle();

        for (var line : lines) {
            rectangle.add(new Point(line.c[0], line.c[1]));
            rectangle.add(new Point(line.c[2], line.c[3]));
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

            panel.drawLine(x1, y1, x2, y2);

            Line line = new Line(x1, y1, x2, y2);
            outgoingPaint.addLine(line);

            windowOfChange = ascertainWindowOfChange(Collections.singleton(line));

            repaint(windowOfChange);

            x1 = x2;
            y1 = y2;
        }
    }

    private boolean inBounds(MouseEvent e) {
        return e.getX() < PIXEL_WIDTH && e.getY() < PIXEL_HEIGHT;
    }
}
