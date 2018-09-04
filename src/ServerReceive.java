import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;

class ServerReceive extends Thread
{
    private DatagramSocket server;
    private Painter p;
    private byte data[];
    private DatagramPacket receivePacket;
    private ClientData clientData;
    private boolean keepGoing;
    public int nr;
    private ByteBuffer bb;
    private MsgCodec msgCodec;
    private Vector<VectorStatus> vectors;
    private static String wipeOffTxt = "wiped the board";

    ServerReceive(DatagramSocket server, ClientData clientData, Painter p) {
        this.server = server;
        this.clientData = clientData;
        this.p = p;
        keepGoing = true;

        data = new byte[MsgCodec.MAX_MESSAGE_SIZE];
        receivePacket = new DatagramPacket(data, data.length);
        nr = 0;
        bb = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE);
        msgCodec = new MsgCodec();
        vectors = new Vector<VectorStatus>();
    }

    private boolean decode() {
        try {
            bb.clear();
            bb.put(receivePacket.getData(), 0, receivePacket.getLength());
        } catch (BufferUnderflowException e) {
            System.out.println(e);
        }

        MsgCodec.MessageType msgType = msgCodec.mountDecodeBuffer(bb);
        if (msgType != MsgCodec.MessageType.INVALID) {
            String ip = (String) receivePacket.getAddress().getHostAddress();
            int port = receivePacket.getPort();
            clientData.updateRxStatus(ip, port);

            if (msgType == MsgCodec.MessageType.CLEANUP) {
                p.cleanUp();
                clientData.cleanAccumulator();
                clientData.orderCleanUp();
                clientData.addTextMsg(ip, port, wipeOffTxt);
            } else if (msgType == MsgCodec.MessageType.LINE) {
                vectors.clear();
                msgCodec.decodeLines(vectors);
                p.drawLines(vectors);
                clientData.addLines(vectors);
            }
            else if ((msgType == MsgCodec.MessageType.STATUS_CLIENT) || 
                    (msgType == MsgCodec.MessageType.TEXT)) {
                String text = msgCodec.decodeClientStatusMsg();
                if (text != null) {
                    if (msgType == MsgCodec.MessageType.STATUS_CLIENT) {
                        clientData.updateName(ip, port, text);
                    } else {
                        clientData.addTextMsg(ip, port, text);
                    }
                }
            }
        }
        else {
            System.out.println("ServerReceive: Could not identify incoming message");
        }
        return msgCodec.getType() != MsgCodec.MessageType.INVALID;
    }

    public void lightsOut() {
        keepGoing = false;
    }

    public void run() {
        String txt = null;
        int lastClientCount = -1;

        while (keepGoing) {
            // Cycle all clients
            try {
                server.receive(receivePacket);

                if (receivePacket.getLength() > 0) {
                    decode();
                } else {
                    System.out.println("No payload in received message...");
                }
                int currClientCount = clientData.clientCount();
                if (lastClientCount != currClientCount) {
                    lastClientCount = currClientCount;
                    txt = "Number of connected clients: " + currClientCount;
                    p.setStatusTxt(txt);
                }
            } 
            catch (IOException io) {
                System.out.println(io.toString() + "\n");
                try	{
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }
}
