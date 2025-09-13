package edu.eci.arsw.datagramas;

import java.net.*;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class TimeClientUDP implements AutoCloseable {
    private final String host;
    private final int port;
    private final int periodMillis;
    private final int timeoutMillis;
    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<String> lastTime = new AtomicReference<>("<sin datos>");

    public TimeClientUDP(String host, int port, int periodMillis, int timeoutMillis) {
        this.host = host; this.port = port;
        this.periodMillis = periodMillis; this.timeoutMillis = timeoutMillis;
    }

    public static void main(String[] args) {
        String host = "127.0.0.1"; int port = 4445; int period = 5000; int timeout = 1500;
        for (String a : args) {
            if (a.startsWith("--host=")) host = a.substring(7);
            else if (a.startsWith("--port=")) port = Integer.parseInt(a.substring(7));
            else if (a.startsWith("--periodMs=")) period = Integer.parseInt(a.substring("--periodMs=".length()));
            else if (a.startsWith("--timeoutMs=")) timeout = Integer.parseInt(a.substring("--timeoutMs=".length()));
        }
        try (TimeClientUDP c = new TimeClientUDP(host, port, period, timeout)) {
            c.start();
            System.out.println("Consultando hora cada " + period + " ms (Ctrl+C para salir)");
            // bucle simple de demo
            while (true) {
                Thread.sleep(period);
                System.out.println("Hora: " + c.getLastTime());
            }
        } catch (Exception e) {
            System.err.println("Error cliente: " + e.getMessage());
        }
    }

    public void start() {
        ses.scheduleAtFixedRate(this::pollOnce, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    private void pollOnce() {
        byte[] req = new byte[1];
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(timeoutMillis);
            DatagramPacket p = new DatagramPacket(req, req.length, InetAddress.getByName(host), port);
            ds.send(p);
            byte[] buf = new byte[256];
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            ds.receive(resp);
            String time = new String(resp.getData(), 0, resp.getLength());
            lastTime.set(Objects.requireNonNullElse(time, lastTime.get()));
        } catch (Exception e) {
            // No actualización, mantener última
            System.out.println("sin actualización, manteniendo última hora");
        }
    }

    public String getLastTime() { return lastTime.get(); }

    @Override public void close() { ses.shutdownNow(); }
}
