package it.comi.a24client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DiscoveryClient {

    public static final int DISCOVERY_PORT = 42124;
    public static final int DEFAULT_TIMEOUT_MS = 900;

    private static final String PROBE_REQUEST = "24CLOCKS_DISCOVER_V1";
    private static final String RESPONSE_PREFIX = "24CLOCKS_MASTER_V1";

    public static class DiscoveryResult {
        public final String host;
        public final int port;

        public DiscoveryResult(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    public static DiscoveryResult discover() {
        return discover(DISCOVERY_PORT, DEFAULT_TIMEOUT_MS);
    }

    public static DiscoveryResult discover(int discoveryPort, int timeoutMs) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(0));
            socket.setSoTimeout(timeoutMs);

            byte[] probe = PROBE_REQUEST.getBytes("UTF-8");
            DatagramPacket request = new DatagramPacket(
                    probe,
                    probe.length,
                    InetAddress.getByName("255.255.255.255"),
                    discoveryPort
            );
            socket.send(request);

            byte[] responseBuf = new byte[256];
            DatagramPacket response = new DatagramPacket(responseBuf, responseBuf.length);
            socket.receive(response);

            String payload = new String(response.getData(), 0, response.getLength(), "UTF-8").trim();
            if (!payload.startsWith(RESPONSE_PREFIX)) {
                return null;
            }

            String resolvedHost = response.getAddress().getHostAddress();
            int resolvedPort = 23;

            String[] fields = payload.split(";");
            for (String field : fields) {
                String trimmed = field.trim();
                if (trimmed.startsWith("IP=")) {
                    String candidateHost = trimmed.substring(3).trim();
                    if (!candidateHost.isEmpty()) {
                        resolvedHost = candidateHost;
                    }
                } else if (trimmed.startsWith("PORT=")) {
                    try {
                        resolvedPort = Integer.parseInt(trimmed.substring(5).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            return new DiscoveryResult(resolvedHost, resolvedPort);
        } catch (IOException ignored) {
            return null;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
