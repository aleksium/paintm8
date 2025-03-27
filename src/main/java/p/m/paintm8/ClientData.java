package p.m.paintm8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientData {

    private final HashMap<String, ClientStatus> clients = new HashMap<>();
    private final List<Line> lines1 = new ArrayList(200);
    private final List<Line> lines2 = new ArrayList(200);
    private List<Line> in = lines1;
    private List<Line> out = lines2;
    private boolean phase1 = true;
    private PaintAccumulator paintAccumulator = null;
    private final AtomicBoolean wipeRequested = new AtomicBoolean();
    private final boolean isServer;

    ClientData(boolean isServer) {
        this.isServer = isServer;

        if (isServer) {
            paintAccumulator = new PaintAccumulator();
        }
    }

    public int updateRxStatus(String ip, int port) {
        if (!isServer) {
            return -1;
        }
        synchronized (this) {
            String ipport = ip + port;
            ClientStatus cs = clients.get(ipport);
            if (cs == null) {
                if (clients.size() <= Environment.MAX_NUMBER_OF_CLIENTS) {
                    var clientStatus = new ClientStatus(ip, port, clients.size()-1);
                    clientStatus.update();
                    clients.put(ipport, clientStatus);
                    pushAccumulatedPaint();
                    return clientStatus.getIndex();
                }
            } else {
                cs.update();
                return cs.getIndex();
            }
        }
        return 0;
    }

    private void dropDisconnects() {
        long currTime = System.currentTimeMillis();
        synchronized (this) {
            var itr = clients.entrySet().iterator();
            while (itr.hasNext()) {
                var entry = itr.next();
                if (currTime - entry.getValue().getLastSignOfLife() > 21000) {
                    itr.remove();
                }
            }
        }
    }

    public Collection<ClientStatus> getAllIPs() {
        return clients.values();
    }

    public void addLine(Line v) {
        synchronized (this) {
            in.add(v);
        }
        if (isServer) {
            paintAccumulator.addLines(List.of(v));
        }
    }

    public void addLines(List<Line> lines) {
        synchronized (this) {
            in.addAll(lines);
        }
        if (isServer) {
            paintAccumulator.addLines(lines);
        }
    }

    public List<Line> getAllLines() {
        synchronized (this) {
            if (phase1) {
                phase1 = false;
                lines2.clear();
                in = lines2;
                out = lines1;
            } else {
                phase1 = true;
                lines1.clear();
                in = lines1;
                out = lines2;
            }
        }
        return out;
    }
    
    public void setWipeRequested(boolean wipeRequested) {
        this.wipeRequested.set(wipeRequested);
        if (isServer) {
            paintAccumulator.clearLines();
        }
    }
    
    public boolean isWipeRequested() {
        return wipeRequested.get();
    }

    public int clientCount() {
        return clients.size();
    }

    private void pushAccumulatedPaint() {
        try {
            in.addAll(paintAccumulator.currentLines());
        } catch (NullPointerException e) {
            System.out.println("Internal error: Accumulator object has not been created");
        }
    }
}
