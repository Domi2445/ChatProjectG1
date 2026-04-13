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


    UDPReciever  receiver = new UDPReciever(7000);


        while (true)
        {
                byte[] data = receiver.receiver();
                speakers.write(data,0 , data.length);
        }
    }
}