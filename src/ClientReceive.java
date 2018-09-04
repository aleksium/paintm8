import java.net.*;
import java.io.*;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import java.util.Vector;

public class ClientReceive extends Thread {
    private DatagramSocket sock;
    private Painter painter;
    private DatagramPacket receivePacket;
    private byte data[];
    private boolean connected;
    private boolean stillGoing = true;
    public int nr;
    private ByteBuffer bb;
    private Color drawColor = null;
    private MsgCodec msgCodec = null;
    private Vector<VectorStatus> vectors = null;
    private ShoutBox sb = null;

    public ClientReceive(DatagramSocket sock, Painter painter, ShoutBox sb)	{
        this.sock = sock;
        this.painter = painter;
        this.sb = sb;
        connected = false;
        data = new byte[MsgCodec.MAX_MESSAGE_SIZE];
        receivePacket = new DatagramPacket(data, data.length);
        nr = 0;
        bb = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE);
        drawColor = new Color(0.9f, 0.9f, 0.9f, 0.3f);
        msgCodec = new MsgCodec();
        vectors = new Vector<VectorStatus>();
    }

    public void login()	{
        connected = false;
        if (sock != null)
        {
            connected = true;
        }
    }

    public void done() {
        synchronized (this) {
            stillGoing = false;
        }
    }

    private boolean stillGoing() {
        boolean yes = true;
        synchronized (this) {
            yes = stillGoing;
        }
        return yes;
    }

    public void run() {
        while (stillGoing()) {
            if (connected) {
                try {
                    sock.receive(receivePacket);
                    decode();
                    // System.out.println("Vector received: " + v.c[0] + "," + v.c[1] + "," + v.c[2] + "," + v.c[3]);
                } catch (IOException io) {
                    //System.out.println(io.toString() + "\n");
                    try {
                        Thread.sleep(500);
                    } catch(InterruptedException e)	{
                        System.out.println(e);
                    }
                }
            } else {
                try	{
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
        System.out.println("input thread terminated");
    }

    private void decode() {
        try {
            bb.clear();
            bb.put(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (BufferUnderflowException e) {
            System.out.println(e);
        }

        MsgCodec.MessageType msgType = msgCodec.mountDecodeBuffer(bb);

        if (msgType == MsgCodec.MessageType.CLEANUP) {
            painter.cleanUp();
        } else if (msgType == MsgCodec.MessageType.LINE) {
            vectors.clear();
            msgCodec.decodeLines(vectors);
            painter.drawLines(vectors);
        } else if (msgType == MsgCodec.MessageType.STATUS_SERVER) {
            String text = msgCodec.decodeServerStatusMsg();
            if (text != null) {
                if (!text.contains("Ping from server")) {
                    sb.addStatusText(text);
                }
            }
        }
    }
}

