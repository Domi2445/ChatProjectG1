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


      UDPSender sender = new UDPSender("localhost",7000);
      byte[] buffer = new byte[1024];

        while (true) {

            int bytesRead = microphone.read(buffer, 0, buffer.length);

            byte[] data = new byte[bytesRead];
            System.arraycopy(buffer,0,data,0 ,bytesRead);


          sender.send(data);
        }
    }
}