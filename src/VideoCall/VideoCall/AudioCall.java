package VideoCall;

import javax.sound.sampled.*;
import java.net.*;

public class AudioCall
{

    private volatile boolean running = false ;
    private Thread senderThread;
    private Thread recieverThread;


    public  void start(String partnerIp,int sendProt , int recieverPort) throws Exception
    {
        running = true ;


        // Tools for sending and receiving
        UDPSender sender = new UDPSender(partnerIp, sendProt);
        UDPReciever receiver = new UDPReciever(recieverPort);






        // Thread 1 - captures your mic and sends it to your partner
        senderThread = new Thread(() -> {
            try
            {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();
                System.out.println("Mic active, sending audio...");

                byte[] buffer = new byte[1024];
                while (running)
                {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);
                    sender.send(data);
                }
                microphone.stop();
                microphone.close();
                sender.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        // Thread 2 - receives your partner's audio and plays it
        recieverThread = new Thread(() -> {
            try
            {
                AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);
                speakers.open(format);
                speakers.start();
                System.out.println("Waiting for audio...");

                while (running)
                {
                    byte[] data = receiver.receiver();
                    speakers.write(data, 0, data.length);
                }

                speakers.stop();
                speakers.close();
                receiver.close();

            } catch (Exception e)
            {
                e.printStackTrace();
            }
        });
        senderThread.start();
        recieverThread.start();

    }
    public void stop() {
        running = false;
    }
}