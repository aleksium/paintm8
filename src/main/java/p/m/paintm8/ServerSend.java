package p.m.paintm8;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

class ServerSend extends Thread
{
    private final DatagramSocket socket;
    private final ClientData clientData;
    private final ByteBuffer buffer = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE); // 256 integers
    private final MsgCodec msgCodec = new MsgCodec();
    private final DatagramPacket packet = new DatagramPacket(buffer.array(), buffer.limit());
    private int tick = 0;

    ServerSend(DatagramSocket socket, ClientData clientData) {
        this.socket = socket;
        this.clientData = clientData;
    }
    
    @Override
    public void run() {
        while (true) {
            List<Line> lines = clientData.getAllLines();
            if (!lines.isEmpty()) {
                distributeLines(lines);
            } else {
                try {
                    Thread.sleep(25);
                    if (tick++ > 400) {
                        tick = 0;
                        clientData.dropDeads();
                        msgCodec.mountEncodeBuffer(buffer);
                        if (msgCodec.encodeServerStatusMsg("Ping from server")) {
                            sendToAll(true);
                        }
                    }
                } catch(InterruptedException e) {
                    System.out.println(e);
                }
                if (clientData.isWipeRequested()) {
                    msgCodec.mountEncodeBuffer(buffer);
                    if (msgCodec.encodeWipeCommand()) {
                        sendToAll();
                        clientData.setWipeRequested(false);
                    }
                }
            }
        }
    }

    private void sendToAll() {
        sendToAll(false);
    }
    
    private void sendToAll(boolean giveStatus) {
        var clients = clientData.getAllIPs();
        for (var client : clients) {
            try {
                IpPort ip = client.getIpPort();
                InetAddress addr = InetAddress.getByName(ip.ip());
                packet.setAddress(addr);
                packet.setPort(ip.port());
                packet.setData(buffer.array(), 0, buffer.limit());
                socket.send(packet);
            } catch (UnknownHostException e) {
                System.out.println("Unknown host " + e);
            } catch (IOException e) {
                System.out.println("IOException: " + e);
            }
        }
        if (giveStatus) {
            System.out.println("-------------------");
            System.out.println("Painters:");
            for (var client : clients) {
                System.out.println(client);
            }
            System.out.println("-------------------");
        }
    }

    private void distributeLines(List<Line> vs) {
        int index = 0;
        while (index < vs.size()) {
            int size = vs.size() - index;
            if (size > MsgCodec.MAX_LINES) {
                size = MsgCodec.MAX_LINES;
            }
            msgCodec.mountEncodeBuffer(buffer);
            msgCodec.encodeLines(vs, index, size);

            index += size;
            sendToAll();
        }
    }
}
