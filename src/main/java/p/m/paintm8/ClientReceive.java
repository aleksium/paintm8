package p.m.paintm8;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientReceive extends Thread {

    private final DatagramSocket sock;
    private final Painter painter;
    private final DatagramPacket receivePacket = new DatagramPacket(new byte[MsgCodec.MAX_MESSAGE_SIZE], MsgCodec.MAX_MESSAGE_SIZE);
    private final MsgCodec msgCodec = new MsgCodec();
    private boolean connected = false;
    private boolean stillGoing = true;

    public ClientReceive(DatagramSocket sock, Painter painter) {
        this.sock = sock;
        this.painter = painter;
    }

    public void login() {
        connected = false;
        if (sock != null) {
            connected = true;
        }
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

    @Override
    public void run() {
        while (stillGoing()) {
            if (connected) {
                try {
                    sock.receive(receivePacket);
                    decode();
                } catch (IOException io) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
        System.out.println("input thread terminated");
    }

    private void decode() {

        MsgCodec.MessageType msgType = msgCodec.mountDecodeBuffer(receivePacket);

        if (msgType == MsgCodec.MessageType.LINE) {
            painter.drawLines(msgCodec.decodeLines());
        } else if (msgType == MsgCodec.MessageType.STATUS_SERVER) {
            String text = msgCodec.decodeServerStatusMsg();
            if (text != null) {
                if (!text.contains("Ping from server")) {
                }
            }
        }
    }
}
