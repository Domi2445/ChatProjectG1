package VideoCall;

import javax.sound.sampled.*;
import java.net.*;

public class AudioCall
{

    public static void start(String partnerIp) throws Exception
    {

        // Tools for sending and receiving
        UDPSender sender = new UDPSender(partnerIp, 7000);
        UDPReciever receiver = new UDPReciever(7000);

        // Thread 1 - captures your mic and sends it to your partner
        new Thread(() -> {
            try
            {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                System.out.println("Mic active, sending audio...");

                byte[] buffer = new byte[1024];
                while (true)
                {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);
                    sender.send(data);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();

        // Thread 2 - receives your partner's audio and plays it
        new Thread(() -> {
            try
            {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();
                System.out.println("Waiting for audio...");

                while (true)
                {
                    byte[] data = receiver.receiver();
                    speakers.write(data, 0, data.length);
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }).start();
    }
    public static void main(String[] args) throws Exception {
        start("127.0.0.1");
    }
}