import java.util.*;
import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

class ServerSend extends Thread
{
    private DatagramSocket socket;
    private ClientData clientData;
    public int nr;
    private ByteBuffer buffer;
    private MsgCodec msgCodec;
    private DatagramPacket packet;
    private int tick = 0;

    ServerSend(DatagramSocket socket, ClientData clientData) {
        this.socket = socket;
        this.clientData = clientData;
        nr = 0;
        buffer = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE); // 256 integers
        msgCodec = new MsgCodec();
        packet = new DatagramPacket(buffer.array(), buffer.limit());
    }

    private void sendToAll() {
        Vector<IpPort> ips = clientData.getAllIPs();

        Iterator<IpPort> itr = ips.iterator();

        while(itr.hasNext()) {
            try {
                IpPort ip = (IpPort)itr.next();
                InetAddress addr = InetAddress.getByName(ip.getIp());
                packet.setAddress(addr);
                packet.setPort(ip.getPort());
                packet.setData(buffer.array(), 0, buffer.limit());
                socket.send(packet);
            } catch (UnknownHostException e) {
                System.out.println("Unknown host " + e);
            } catch (IOException e) {
                System.out.println("IOException: " + e);
            }
        }
    }

    private void distributeLines(Vector<VectorStatus> vs) {
        int size = vs.size();
        int index = 0;
        while (index < vs.size()) {
            size = vs.size() - index;
            if (size > MsgCodec.MAX_LINES) {
                size = MsgCodec.MAX_LINES;
            }
            msgCodec.mountEncodeBuffer(buffer);
            int kake = msgCodec.encodeLines(vs, index, size);

            index += size;
            sendToAll();
        }
    }

    public void run() {
        boolean op = false;
        while (true) {
            op = false;
            Vector<VectorStatus> lines = clientData.getAllLines();
            Vector<String> text = clientData.getAllTextMsgs();
            if (lines.size() > 0) {
                distributeLines(lines);
                op = true;
            }
            if (clientData.cleanUp()) {
                msgCodec.mountEncodeBuffer(buffer);
                if (msgCodec.encodeCleanUpMsg()) {
                    sendToAll();
                    op = true;
                }
            }
            if (text.size() > 0) {
                Iterator<String> itr = text.iterator();
                op = true;
                while(itr.hasNext()) {
                    msgCodec.mountEncodeBuffer(buffer);
                    String txt = (String)itr.next();
                    if (msgCodec.encodeServerStatusMsg(txt)) {
                        sendToAll();
                    }
                }
            }
            if (!op) {
                try {
                    Thread.sleep(25);
                    if (++tick > 400) {
                        tick = 0;
                        msgCodec.mountEncodeBuffer(buffer);
                        if (msgCodec.encodeServerStatusMsg("Ping from server")) {
                            sendToAll();
                        }
                    }
                } catch(InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }
}
