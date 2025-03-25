package p.m.paintm8;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ClientSend extends Thread {

    private final DatagramSocket sock;
    private InetAddress ip = null;
    private final ClientData clientData;
    private boolean stillGoing = true;
    private boolean connected = false;
    private final ByteBuffer buffer = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE);
    private MsgCodec msgCodec = msgCodec = new MsgCodec();
    DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.capacity());

    public ClientSend(DatagramSocket sock, ClientData myLines) {
        this.sock = sock;
        this.clientData = myLines;
    }

    public void done() {
        synchronized (this) {
            stillGoing = false;
        }
    }

    private boolean stillGoing() {
        boolean yes;
        synchronized (this) {
            yes = stillGoing;
        }
        return yes;
    }

    public void login(String ip) {
        connected = false;
        if (sock != null) {
            try {
                this.ip = InetAddress.getByName(ip);
                connected = true;
            } catch (UnknownHostException e) {
                System.out.println(e);
            }
        }
    }

    private void sendToServer() {
        try {
            packet.setData(buffer.array(), 0, buffer.limit());
            packet.setAddress(ip);
            packet.setPort(Environment.SERVER_PORT);
            sock.send(packet);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    @Override
    public void run() {
        int snoozes = 0;
        try {
            while (stillGoing()) {
                if (connected) {
                    var lines = clientData.getAllLines();
                    if (!lines.isEmpty()) {
                        msgCodec.mountEncodeBuffer(buffer);
                        if (msgCodec.encodeLines(lines, 0, lines.size()) > 0) {
                            sendToServer();
                        } else {
                            System.out.println("ClientSend: Unable to encode");
                        }
                    } else {
                        Thread.sleep(25); // Snooze for 25 millies
                        if (++snoozes > 400) {
                            snoozes = 0;
                            msgCodec.mountEncodeBuffer(buffer);
                            boolean okToSend;
                            synchronized (this) {
                                okToSend = msgCodec.encodeClientStatusMsg("I am here");
                            }
                            if (okToSend) {
                                sendToServer();
                            }
                        }
                        if (clientData.isWipeRequested()) {
                            msgCodec.mountEncodeBuffer(buffer);
                            boolean readyToSend = msgCodec.encodeWipeCommand();
                            if (readyToSend) {
                                clientData.setWipeRequested(false);
                                sendToServer();
                            }
                        }
                    }
                } else {
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        }
        System.out.println("output thread terminated");
    }
}
