package edu.eci.arsw.datagramas;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TimeDatagramTest {

    @Test
    void clientRecoversWhenServerReturns() throws Exception {
        // Puerto fijo de prueba efímero:
        int port;
        try (var ss = new java.net.ServerSocket(0)) { port = ss.getLocalPort(); }

        // Server ON
        TimeServerUDP server = new TimeServerUDP(port);
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.submit(server);

        // Client with short periods/timeouts
        try (TimeClientUDP client = new TimeClientUDP("127.0.0.1", port, 100, 50)) {
            client.start();
            // Espera primera actualización
            waitUntil(() -> !client.getLastTime().equals("<sin datos>"), 1500);
            String t1 = client.getLastTime();

            // Apagar server -> cliente mantiene última hora
            server.shutdown();
            // Esperar algunos ciclos
            Thread.sleep(300);
            String t2 = client.getLastTime();
            assertEquals(t1, t2, "Debe mantener la última hora cuando no hay respuesta");

            // Prender server de nuevo
            TimeServerUDP server2 = new TimeServerUDP(port);
            es.submit(server2);
            waitUntil(() -> !client.getLastTime().equals(t2), 2000);
            String t3 = client.getLastTime();
            assertNotEquals(t2, t3, "Debe actualizarse cuando el servidor vuelve");
            server2.shutdown();
        } finally {
            server.shutdown();
            es.shutdownNow();
        }
    }

    private static void waitUntil(Callable<Boolean> cond, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (cond.call()) return;
            Thread.sleep(25);
        }
        fail("Timeout esperando condición");
    }
}
