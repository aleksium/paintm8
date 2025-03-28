package p.m.paintm8;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

class ServerReceive extends Thread {

    private final DatagramSocket server;
    private final Canvas p;
    private final byte data[] = new byte[MsgCodec.MAX_MESSAGE_SIZE];
    private final DatagramPacket receivePacket = new DatagramPacket(data, data.length);
    private final ClientData clientData;
    private boolean keepGoing = true;
    private final MsgCodec msgCodec = new MsgCodec();

    ServerReceive(DatagramSocket server, ClientData clientData, Canvas p) {
        this.server = server;
        this.clientData = clientData;
        this.p = p;
    }

    private boolean decode() {
        MsgCodec.MessageType msgType = msgCodec.mountDecodeBuffer(receivePacket);
        if (msgType != MsgCodec.MessageType.INVALID) {
            String ip = (String) receivePacket.getAddress().getHostAddress();
            int port = receivePacket.getPort();
            var index = clientData.updateRxStatus(ip, port);

            if (null != msgType) switch (msgType) {
                case LINE -> {
                    var lines = msgCodec.decodeLines();
                    lines.forEach(line -> line.c[4] = index);
                    clientData.addLines(lines);
                    p.drawLines(lines);
                }
                case STATUS_CLIENT -> {
                    String text = msgCodec.decodeClientStatusMsg();
                    if (text != null) {
                        clientData.updateRxStatus(ip, port);
                    }
                }
                case WIPE -> {
                    clientData.setWipeRequested(true);
                }
                default -> {
                    System.out.println("Received unsupported message");
                }
            }
        } else {
            System.out.println("Could not identify incoming message");
        }
        return msgCodec.getType() != MsgCodec.MessageType.INVALID;
    }

    public void lightsOut() {
        keepGoing = false;
    }

    @Override
    public void run() {
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
                }
            } catch (IOException io) {
                System.out.println(io.toString() + "\n");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }
}
