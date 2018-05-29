package p.m.paintm8;

public class ClientStatus {

    private long lastSignOfLife;
    private final IpPort ipPort;

    public ClientStatus(String ip, int port) {
        ipPort = new IpPort(ip, port);
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
}