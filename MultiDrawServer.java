import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.*;
import javax.swing.*;
import java.net.*;

public class MultiDrawServer {
    static public int port = 3174;
    static public Painter p = null;

    public MultiDrawServer() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater (new Runnable () {
            public void run () {
                JFrame frame = null;
                ClientData clientData;
                try	{
                    clientData = new ClientData(true);
                    DatagramSocket server = new DatagramSocket(port);
                    System.out.println("Done preparing clients data structure");
                    p = new Painter();
                    System.out.println("Done creating Painter object");

                    frame = new JFrame ("MultiDraw Server");
                    JMenuBar bar = new JMenuBar();

                    JMenu canvasMenu = new JMenu("Canvas");
                    JMenuItem cleanup = new JMenuItem("Clean up");
                    cleanup.setMnemonic('C');
                    cleanup.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            p.cleanUp();
                        }
                    });

                    canvasMenu.add(cleanup);
                    bar.add(canvasMenu);
                    bar.setBackground(Color.BLACK);
                    bar.setForeground(Color.LIGHT_GRAY);
                    frame.setJMenuBar(bar);
                    frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);

                    ServerReceive in = new ServerReceive(server, clientData, p);
                    ServerSend out = new ServerSend(server, clientData);

                    in.start();
                    System.out.println("Started Server Receive Thread");
                    out.start();
                    System.out.println("Started Server Send Thread");

                    frame.add (p);
                    frame.setResizable (false);
                    frame.setVisible (true);
                    frame.pack ();
                    frame.validate();
                } catch (SocketException se) {
                    System.out.println("Could not create Datagram socket\n");
                }
            }
        });
    }
}
