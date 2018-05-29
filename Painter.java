import java.util.Random;
import java.awt.event.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import java.util.*;

public class Painter extends JPanel {
    static public final int PIXEL_WIDTH = 700;
    static public final int PIXEL_HEIGHT = 450;
    public final int STATUS_BAR_LENGTH = PIXEL_WIDTH;
    public final int STATUS_BAR_HEIGHT = 20;
    private int x1, x2, y1, y2, tag = 0;
    private boolean drawing_ = false;
    BufferedImage textImage_ = null;
    BufferedImage panelImage_ = null;
    private boolean updateStatus_ = false;

    private Graphics2D txtg_ = null;
    private Graphics2D panelG_ = null;
    private ClientData myLines_;
    private Font statusFont_ = null;
    private Color textColor_ = null;
    private int superColor_ = 0;
    private Color backgroundColor_ = null;
    public static final int CLEANER_NR = 1000;
    private Vector<VectorStatus> lines_ = null;
    private BasicStroke fatStroke_ = null;
    private boolean clearItAlready_ = false;
    private int numLinesDrawn_ = 0;

    private void handleMouseDrag(MouseEvent e) {
        if (drawing_) {
            if (inBounds(e)) {
                x2 = e.getX();
                y2 = e.getY();
                VectorStatus line = new VectorStatus(x1,y1,x2,y2,tag);
                addVector(line);
                synchronized (this) {
                    lines_.add(line);
                    updateStatus_ = true;
                    ++numLinesDrawn_;
                }
                repaint();

                x1 = x2;
                y1 = y2;
            }
        }
    }

    public Painter(ClientData myLines)	{
        super();
        myLines_ = myLines;
        tag = (int)(new Random(System.currentTimeMillis()).nextFloat() * 18.0f);
        System.out.println("initialized with tag: " + tag);
        commonInit();
    }

    public void paint(Graphics g) {
        super.paint(g);
    }

    public Painter() {
        super();
        commonInit();
    }

    private void commonInit() {
        fatStroke_ = new BasicStroke(10);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        setPreferredSize (new Dimension (PIXEL_WIDTH, PIXEL_HEIGHT));
        lines_ = new Vector<VectorStatus>(200);
        textImage_ = new BufferedImage(STATUS_BAR_LENGTH, STATUS_BAR_HEIGHT, BufferedImage.TYPE_INT_RGB);
        panelImage_ = new BufferedImage(PIXEL_WIDTH, PIXEL_HEIGHT, BufferedImage.TYPE_INT_RGB);

        txtg_ = (Graphics2D)textImage_.getGraphics();
        panelG_ = (Graphics2D)panelImage_.getGraphics();

        int rgba = Color.HSBtoRGB((float)superColor_/18.0f, 0.9f, 0.7f) & 0x00ffffff;
        rgba |= (0x90 & 0xff) << 24;
        System.out.println("the rgba: " + rgba);
        panelG_.setColor(new Color(rgba, true));

        panelG_.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        panelG_.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        panelG_.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);

        // superColor = new Color(0.1f, 0.2f, 1.0f, 0.7f); // Blue
        // superColor = new Color(0.92f, 0.43f, 0.92f, 0.4f); // pink 

        textColor_ = new Color(0.0f, 0.0f, 0.0f, 1.0f);
        backgroundColor_ = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        statusFont_ = new Font(Font.SERIF, Font.PLAIN, 12);

        txtg_.setFont(statusFont_);
        txtg_.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        setStatusTxt("Welcome!");
        addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if (inBounds(e)) {
                            drawing_ = true;
                            x1 = e.getX();
                            y1 = e.getY();
                        }
                    }

                    public void mouseReleased(MouseEvent e) {
                        drawing_ = false;
                    }
                }
                );
        cleanUp();
        addMouseMotionListener(
                new MouseMotionAdapter() {
                    public void mouseDragged( MouseEvent e ) {
                        handleMouseDrag(e);
                    }
                });
    }

    public void cleanUp() {
        synchronized (this) {
            clearItAlready_ = true;
        }
        repaint();
    }

    public void sendCleaner() {
        VectorStatus cleaner = new VectorStatus(CLEANER_NR,CLEANER_NR,CLEANER_NR,CLEANER_NR, CLEANER_NR);
        for (int i = 0; i < 3; ++i) { // make sure the message is heard
            this.addVector(cleaner);
        }
    }

    private boolean inBounds(MouseEvent e) {
        return !((e.getX() >= PIXEL_WIDTH) || (e.getY() >= PIXEL_HEIGHT));
    }

    public void paintComponent(Graphics g) {
        synchronized (this) {
            if (clearItAlready_) {
                clearItAlready_ = false;
                panelG_.setColor(backgroundColor_);
                panelG_.fillRect(0,0,PIXEL_WIDTH, PIXEL_HEIGHT);
            } else {

                panelG_.setStroke(fatStroke_);
                int rgba = Color.HSBtoRGB((float)superColor_/18.0f, 0.5f, 0.5f) & 0x00ffffff;
                rgba |= (0x90 & 0xff) << 24;
                panelG_.setColor(new Color(rgba, true));
                for (int i = 0; i < lines_.size(); ++i) {
                    VectorStatus line = lines_.get(i);
                    setStatusTxt("super color is : " + (line.c[4]) + " and divided by 18 it becomes " + ((float)line.c[4] / 18.0f));
                    if (superColor_ != line.c[4]) {
                        superColor_ = line.c[4];

                        rgba = Color.HSBtoRGB((float)superColor_/18.0f, 0.5f, 0.5f) & 0x00ffffff;
                        rgba |= (0x90 & 0xff) << 24;
                        panelG_.setColor(new Color(rgba, true));
                    }
                    panelG_.drawLine(line.c[0], line.c[1], line.c[2], line.c[3]);
                }
                lines_.clear();
            }
            if (updateStatus_) {
                //setStatusTxt("You have drawn " + numLinesDrawn_ + " lines!");
            }
            g.drawImage(panelImage_, 0, 0, PIXEL_WIDTH, PIXEL_HEIGHT, null); 
            g.drawImage(textImage_, 0, 0, STATUS_BAR_LENGTH, STATUS_BAR_HEIGHT, null);
        }
    }


    public Dimension getPreferredSize()	{
        return new Dimension(PIXEL_WIDTH, PIXEL_HEIGHT);
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void drawLines(Vector<VectorStatus> additionalLines)	{
        synchronized (this) {
            lines_.addAll(additionalLines);
        }
        repaint();
    }

    public void addVector(final VectorStatus v)	{
        if (myLines_ != null) {
            myLines_.addLine(v);   
        }
    }

    public void refreshCanvas()	{
        repaint();
    }

    public void setColor(Color color) {
        //Graphics2D grr = (Graphics2D)this.getGraphics();
        //grr.setColor(color);
    }

    public void setStatusTxt(String status)	{
        updateStatus_ = false;
        txtg_.setColor(backgroundColor_);
        txtg_.fillRect(0,0, STATUS_BAR_LENGTH, STATUS_BAR_HEIGHT);
        txtg_.setColor(textColor_);
        if (status != null && status.length() > 0) {
            txtg_.drawString(status, 10, 15);
        }
    }
}
