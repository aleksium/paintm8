import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;
import javax.swing.JApplet;
import javax.swing.JOptionPane;

public class MultiDrawClient extends JApplet {
    public static Painter p = null;
    public static ClientReceive in = null;
    public static ClientSend out = null;
    public static String hostIp = null;
    public static ShoutBox shoutBox = null;

    public static void main(String[] args) {
        MultiDrawClient theApplet = new MultiDrawClient();
        theApplet.startItAll();
        JFrame frame = new JFrame("WhiteBoard Terminal");
        frame.setContentPane(theApplet);

        frame.setLayout(new BorderLayout());
        frame.add(shoutBox, BorderLayout.SOUTH);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.add(p, BorderLayout.NORTH);

        frame.setResizable(false);
        frame.setVisible(true);
        frame.pack();
        frame.validate();
    }

    public void init() {
        startItAll();

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(p, BorderLayout.NORTH);
        panel.add(shoutBox, BorderLayout.SOUTH);

        getContentPane().add(panel);
        panel.validate();
        panel.setVisible(true);
    }

    public void destroy() {
        in.done();
        out.done();
    }

    private void askToConnect() {
        hostIp = JOptionPane.showInputDialog("Enter the address of the whiteboard: ");
        in.login();
        out.login(hostIp);
        JOptionPane.showMessageDialog(null, "Start drawing!", "Status", JOptionPane.INFORMATION_MESSAGE);
    }

    public void startItAll() {
        try {
            hostIp = "192.168.1.154";

            DatagramSocket sock = new DatagramSocket();
            ClientData myLines = new ClientData(false);

            p   = new Painter(myLines);

            out = new ClientSend(sock, myLines);
            shoutBox = new ShoutBox(out);
            shoutBox.setForeground(Color.LIGHT_GRAY);
            shoutBox.setBackground(Color.DARK_GRAY);
            in  = new ClientReceive(sock, p, shoutBox);

            JMenuBar bar = new JMenuBar();
            JMenu connectionMenu = new JMenu("Connection");
            JMenuItem address = new JMenuItem("Address");
            address.setForeground(Color.LIGHT_GRAY);
            address.setBackground(Color.DARK_GRAY);
            address.setMnemonic('A');
            address.addActionListener(
                    new ActionListener() 
                    {
                        public void actionPerformed(ActionEvent e) 
                        {
                            askToConnect();
                        }
                    });
            JMenu canvasMenu = new JMenu("Actions");

            JMenuItem cleanup = new JMenuItem("Wipe");
            cleanup.setForeground(Color.LIGHT_GRAY);
            cleanup.setBackground(Color.DARK_GRAY);
            JMenuItem nickName = new JMenuItem("Nick name");
            nickName.setForeground(Color.LIGHT_GRAY);
            nickName.setBackground(Color.DARK_GRAY);
            cleanup.setMnemonic('W');
            nickName.setMnemonic('N');
            cleanup.addActionListener(new ActionListener() 
            {    
                public void actionPerformed(ActionEvent e) 
                {
                    p.cleanUp();
                    out.orderCleanUp();
                }
            });
            nickName.addActionListener(new ActionListener() {    
                public void actionPerformed(ActionEvent e) {
                    String nickName = JOptionPane.showInputDialog("Nick name: ");
                    out.setMyName(nickName);
                }
            });     
            connectionMenu.add(address);
            canvasMenu.add(cleanup);
            canvasMenu.add(nickName);

            bar.add(connectionMenu);
            bar.add(canvasMenu);
            bar.setBackground(Color.DARK_GRAY);
            bar.setForeground(Color.LIGHT_GRAY);
            setJMenuBar(bar);
            in.start();
            out.start();

            in.login();
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                System.out.println(e);
            }

            out.login(hostIp);
        } catch (IOException e) {
            System.out.println("Was unable to create socket");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public MultiDrawClient() {
    }
}
