package p.m.paintm8;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.DatagramSocket;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class Paintm8Client {

    public static void main(String[] args) {
        ClientData myLines = new ClientData(false);
        Painter painter = new Painter(myLines);
        JMenuBar bar = new JMenuBar();
        
        try {
            DatagramSocket sock = new DatagramSocket();

            ClientSend out = new ClientSend(sock, myLines);
            ClientReceive in = new ClientReceive(sock, painter);

            JMenu connectionMenu = new JMenu("Connection");
            JMenuItem address = new JMenuItem("Address");
            address.setMnemonic('A');
            address.addActionListener((ActionEvent e) -> {
                String hostIp = JOptionPane.showInputDialog("Enter the address of the whiteboard: ");
                in.login();
                out.login(hostIp);
                JOptionPane.showMessageDialog(null, "Start drawing!", "Status", JOptionPane.INFORMATION_MESSAGE);
            });
            JMenu canvasMenu = new JMenu("Actions");

            connectionMenu.add(address);
            bar.add(connectionMenu);
            bar.add(canvasMenu);
            in.start();
            out.start();

            in.login();

            Thread.sleep(1000);

            out.login("127.0.0.1");
        } catch (IOException e) {
            System.out.println("Unable to create socket");
        } catch (InterruptedException e) {
            System.out.println(e);
        }

        JFrame frame = new JFrame("Paintm8 Client");

        frame.setJMenuBar(bar);
        
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);
        frame.setVisible(true);
        frame.add(painter, BorderLayout.NORTH);
        
        frame.setResizable(false);
        frame.setVisible(true);
        frame.pack();
        frame.validate();
    }
}
