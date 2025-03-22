package p.m.paintm8;

public class ClientStatus {

    private int index;
    private long lastSignOfLife;
    private final IpPort ipPort;

    public ClientStatus(String ip, int port, int index) {
        ipPort = new IpPort(ip, port);
        this.index = index;
    }

    public void update() {
        this.lastSignOfLife = System.currentTimeMillis();
    }

    public IpPort getIpPort() {
        return ipPort;
    }

    public long getLastSignOfLife() {
        return lastSignOfLife;
    }
    public int getIndex() {
        return index;
    }
}