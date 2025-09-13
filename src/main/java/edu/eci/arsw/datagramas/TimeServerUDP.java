package edu.eci.arsw.datagramas;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class TimeServerUDP implements Runnable {
    private final int port;
    private DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public TimeServerUDP(int port) { this.port = port; }

    public static void main(String[] args) throws Exception {
        int port = 4445;
        for (String a : args) if (a.startsWith("--port=")) port = Integer.parseInt(a.substring(7));
        new TimeServerUDP(port).run();
    }

    public void shutdown() { running.set(false); if (socket != null) socket.close(); }

    @Override public void run() {
        try (DatagramSocket ds = new DatagramSocket(port)) {
            this.socket = ds;
            byte[] buf = new byte[256];
            while (running.get()) {
                DatagramPacket req = new DatagramPacket(buf, buf.length);
                try {
                    ds.receive(req);
                } catch (SocketException se) {
                    break; // socket cerrado
                }
                String time = Instant.now().toString();
                byte[] out = time.getBytes();
                DatagramPacket resp = new DatagramPacket(out, out.length, req.getAddress(), req.getPort());
                ds.send(resp);
            }
        } catch (IOException e) {
            System.err.println("TimeServerUDP error: " + e.getMessage());
        }
    }
}
