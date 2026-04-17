package Server;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AudioRelayServer implements Runnable {
    private final int port;
    private final List<InetSocketAddress> clients = new CopyOnWriteArrayList<>();

    public AudioRelayServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("AudioRelayServer running on port " + port);
            byte[] buffer = new byte[2048];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                InetSocketAddress sender = new InetSocketAddress(
                        packet.getAddress(), packet.getPort()
                );

                // Register new client if not already in list
                if (!clients.contains(sender)) {
                    clients.add(sender);
                    System.out.println("New client connected: " + sender);
                }

                // Forward to everyone except sender
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
                for (InetSocketAddress client : clients) {
                    if (!client.equals(sender)) {
                        DatagramPacket forward = new DatagramPacket(
                                data, data.length,
                                client.getAddress(), client.getPort()
                        );
                        socket.send(forward);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}