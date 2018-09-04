import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Vector;

public class ClientSend extends Thread {
    private DatagramSocket sock;
    private InetAddress ip;
    private ClientData myLines;
    private boolean stillGoing = true;
    private boolean connected;
    public int nr;
    private ByteBuffer buffer;
    private MsgCodec msgCodec;
    DatagramPacket packet = null;
    private boolean cleanUp = false;
    private int snoozes = 400;
    String name = null;
    String reportText = null;
    boolean reportPending = false;

    public ClientSend(DatagramSocket sock, ClientData myLines) {
        this.sock = sock;
        ip = null;
        this.myLines = myLines;        
        connected = false;
        nr = 0;
        buffer = ByteBuffer.allocate(MsgCodec.MAX_MESSAGE_SIZE);
        msgCodec = new MsgCodec();
        packet = new DatagramPacket(buffer.array(), buffer.capacity());
        name = "Painter";
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

    public void fileReport(String text) {
        if (text != null) {
            synchronized (this) {
                if (!reportPending) {
                    reportPending = true;
                    reportText = text;
                }
            }
        }
    }

    private boolean report() {
        boolean doIt = false;
        synchronized (this) {
            doIt = reportPending;
        }
        if (doIt) {
            msgCodec.mountEncodeBuffer(buffer);
            if (msgCodec.encodeClientStatusMsg(reportText, false)) {
                sendToServer();
            } else {
                doIt = false;
            }
            synchronized (this) {
                reportPending = false;
            }
        }
        return doIt;
    }

    public void orderCleanUp() {
        synchronized (this) {
            cleanUp = true;
        }
    }

    private boolean dirty() {
        boolean dirty = false;
        synchronized (this) {
            if (cleanUp) {
                dirty = true;
                cleanUp = false;
            }
        }
        return dirty;
    }

    public void setMyName(String myName) {
        synchronized (this) {
            name = myName;
        }
    }

    private void sendToServer() {
        try {
            //System.out.println("ClientSend: Send " + buffer.limit() + " to server");
            packet.setData(buffer.array(), 0, buffer.limit());
            packet.setAddress(ip);
            packet.setPort(3174);
            sock.send(packet);
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
    }

    public void run() {
        while (stillGoing()) {
            if (connected) {
                Vector<VectorStatus> vectors = myLines.getAllLines();
                if (vectors.size() > 0) {
                    msgCodec.mountEncodeBuffer(buffer);
                    if (msgCodec.encodeLines(vectors, 0, vectors.size()) > 0) {
                        sendToServer();
                    } else {
                        System.out.println("ClientSend: Unable to encode");
                    }
                } else if (dirty()) {
                    cleanUp = false;
                    msgCodec.mountEncodeBuffer(buffer);
                    if (msgCodec.encodeCleanUpMsg()) {
                        sendToServer();
                    }
                } else if (report()) {
                } else {
                    try {
                        Thread.sleep(25); // Snooze for 25 millies
                        if (++snoozes > 400) {
                            snoozes = 0;
                            msgCodec.mountEncodeBuffer(buffer);
                            boolean okToSend = false;
                            synchronized (this) {
                                okToSend = msgCodec.encodeClientStatusMsg(name, true); 
                            }
                            if (okToSend) {
                                sendToServer();
                            }
                        }
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
            } else {
                try {
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
        System.out.println("output thread terminated");
    }
}
