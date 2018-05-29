import java.util.HashMap;
import java.util.Vector;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;

class IpPort {
    private String ip_;
    private int port_;

    public IpPort(String ip, int port) {
        ip_ = ip;
        port_ = port;
    }

    public int getPort() {
        return port_;
    }

    public String getIp() {
        return ip_;
    }
}

class ClientStatus {
    public long update;
    public String ip;
    public int port;
    public String name;
    public int tag;

    public ClientStatus(String ip, int port, int tag) {
        this.ip = ip;
        this.port = port;
        this.tag = tag;
        update();
        setName("Michelangelo");
    }

    public void update() {
        this.update = System.currentTimeMillis();
    }

    public void setName(String name) {
        this.name = name + "-" + tag;
    }
}

public class ClientData {
    private HashMap<String, ClientStatus> clients_;
    private Vector<VectorStatus> lines1_;
    private Vector<VectorStatus> lines2_;
    private Vector<String> textMsgs1_;
    private Vector<String> textMsgs2_;
    private Vector<VectorStatus> in_;
    private Vector<VectorStatus> out_;
    private Vector<String> inTxt_;
    private Vector<String> outTxt_;
    private boolean phase1_ = true;
    private boolean textPhase1_ = true;
    private PaintAccumulator paintAccumulator_ = null;
    private boolean isServer_ = false;
    private int tag_ = 0;
    private boolean issueCleanUp_ = false;

    ClientData(boolean isServer) {
        isServer_ = isServer;
        clients_ = new HashMap<String, ClientStatus>();
        lines1_ = new Vector<VectorStatus>(200);
        lines2_ = new Vector<VectorStatus>(200);
        textMsgs1_ = new Vector<String>(20);
        textMsgs2_ = new Vector<String>(20);
        in_ = lines1_;
        out_ = lines2_;
        inTxt_ = textMsgs1_;
        outTxt_ = textMsgs2_;

        if (isServer_) {
            paintAccumulator_ = new PaintAccumulator();
        }
    }

    public void orderCleanUp() {
        synchronized (this) {
            issueCleanUp_ = true;
        }
    }

    public boolean cleanUp() {
        boolean tmp = false;
        synchronized (this) {
            if (issueCleanUp_) {
                issueCleanUp_ = false;
                tmp = true;
            }
        }
        return tmp;
    }

    public void updateName(String ip, int port, String name) {
        if (!isServer_) {
            return;
        }

        synchronized (this) {
            String ipport = ip + port;
            ClientStatus cs = clients_.get(ipport);
            if (cs != null) {
                cs.setName(name);
            }
        }
    }

    private String getAllNames() {
        String names = "Attendees: ";

        Set<Entry<String, ClientStatus>> s = clients_.entrySet();
        Iterator<Entry<String, ClientStatus>> i = s.iterator();
        while (i.hasNext())
        {
            Map.Entry<String, ClientStatus> p = i.next();
            names += "[" + ((ClientStatus) p.getValue()).name + "]";
            if (i.hasNext()) {
                names += ", ";
            }
        }
        return names;
    }

    public void updateRxStatus(String ip, int port)
    {
        if (!isServer_) {
            return;
        }

        synchronized (this) {
            String ipport = ip + port;
            ClientStatus cs = clients_.get(ipport);
            if (cs == null) {
                if (clients_.size() <= 10) {
                    clients_.put(ipport, new ClientStatus(ip, port, ++tag_));
                    pushAccumulatedPaint();
                    inTxt_.add(getAllNames());
                }
            } else {
                cs.update();
            }
        }
    }

    public Vector<IpPort> getAllIPs() {
        Vector<IpPort> ips = new Vector<IpPort>();
        long currTime = System.currentTimeMillis();
        Vector<String> deads = new Vector<String>();
        synchronized (this) {
            Set<Entry<String, ClientStatus>> s = clients_.entrySet();
            Iterator<Entry<String, ClientStatus>> i = s.iterator();
            while (i.hasNext()) {
                Map.Entry<String, ClientStatus> p = i.next();
                if (currTime - ((ClientStatus) p.getValue()).update < 21000) {
                    IpPort ipp = new IpPort(((ClientStatus) p.getValue()).ip, ((ClientStatus) p.getValue()).port);
                    ips.add(ipp);
                } else {
                    deads.add((String)p.getKey());
                }
            }

            Iterator<String> itr = deads.iterator();

            if (itr.hasNext()) {
                while (itr.hasNext()) {
                    String client = itr.next();
                    System.out.println("Removing "+ clients_.get(client).name);
                    clients_.remove(client);
                }
                inTxt_.add(getAllNames());
            }
        }

        return ips;
    }

    public void addLine(VectorStatus v) {
        synchronized (this) {
            in_.add(v);
            if (isServer_) {
                paintAccumulator_.addLine(v);
            }
        }
    }

    public void addLines(Vector<VectorStatus> lines) {
        synchronized (this) {
            in_.addAll(lines);
            if (isServer_) {
                for (int i = 0; i < lines.size(); ++i) {
                    paintAccumulator_.addLine(lines.get(i));
                }
            }
        }
    }

    public void addTextMsg(String ip, int port, String text) {
        String ipport = ip + port;

        synchronized (this) {
            ClientStatus cs = clients_.get(ipport);

            if (cs != null) {
                if (inTxt_.size() < 20) {
                    inTxt_.add("[" + cs.name + "]: " + text);
                }
            }
        }
    }

    public void cleanAccumulator() {
        if (isServer_) {
            synchronized (this) {
                paintAccumulator_.clearLines();
            }
        }
    }

    public Vector<String> getAllTextMsgs() {
        synchronized (this) {
            if (textPhase1_) {
                textPhase1_ = false;
                textMsgs2_.clear();
                inTxt_ = textMsgs2_;
                outTxt_ = textMsgs1_;
            } else {
                textPhase1_ = true;
                textMsgs1_.clear();
                inTxt_ = textMsgs1_;
                outTxt_ = textMsgs2_;
            }
        }
        return outTxt_;
    }

    public Vector<VectorStatus> getAllLines() {
        synchronized (this) {
            if (phase1_) {
                phase1_ = false;
                lines2_.clear();
                in_ = lines2_;
                out_ = lines1_;
            } else {
                phase1_ = true;
                lines1_.clear();
                in_ = lines1_;
                out_ = lines2_;
            }
        }
        return out_;
    }

    public int clientCount() {
        return  clients_.size();
    }

    private void pushAccumulatedPaint() {
        try {
            in_.addAll(paintAccumulator_.currentLines());
            System.out.println("adding acc lines: " + paintAccumulator_.currentLines().size());
        } catch (NullPointerException e) {
            System.out.println("Internal error: Accumulator object has not been created");
        }
    }

    public int getTag() {
        return tag_;
    }
}
