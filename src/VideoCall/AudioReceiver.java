package VideoCall;

import javax.sound.sampled.*;
import java.net.*;

 public class  AudioReceiver
{
    public static void main(String[] args) throws Exception
    {

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);


        SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
        speakers.open(format);
        speakers.start();
        System.out.println("Warte auf Audio...");


        DatagramSocket socket = new DatagramSocket(7000);
        byte[] buffer = new byte[1024];

        while (true) {

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);


            speakers.write(packet.getData(), 0, packet.getLength());
        }
    }
}