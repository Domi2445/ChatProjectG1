package VideoCall;

import javax.sound.sampled.*;
import java.net.*;

public class AudioSender {
    public static void main(String[] args) throws Exception {

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);


        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();
        System.out.println("Mikrofon aktiv, sende Audio...");


        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName("127.0.0.1");

        byte[] buffer = new byte[1024];

        while (true) {

            int bytesRead = microphone.read(buffer, 0, buffer.length);


            DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, 7000);
            socket.send(packet);
        }
    }
}