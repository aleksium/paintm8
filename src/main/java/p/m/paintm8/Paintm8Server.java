package p.m.paintm8;

import java.net.DatagramSocket;
import java.net.SocketException;
import javax.swing.JFrame;

public class Paintm8Server {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Paintm8 Server");

        try {
            ClientData clientData = new ClientData(true);
            DatagramSocket server = new DatagramSocket(Environment.SERVER_PORT);  
            Painter painter = new Painter(clientData);
          
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ServerReceive in = new ServerReceive(server, clientData, painter);
            ServerSend out = new ServerSend(server, clientData);

            in.start();
            System.out.println("Started Server Receive Thread");
            out.start();
            System.out.println("Started Server Send Thread");

            frame.add(painter);
            frame.setResizable(false);
            frame.setVisible(true);
            frame.pack();
            frame.validate();
        } catch (SocketException se) {
            System.out.println("Could not create Datagram socket\n");
        }
    }
}
