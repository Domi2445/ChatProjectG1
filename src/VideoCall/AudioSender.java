package VideoCall;

import javax.sound.sampled.*;
import java.net.*;


public class AudioSender

{
    public  static void main(String[] args) throws Exception
    {
        AudioFormat format = new Audiformat(16000,16,1,true,false);// audio qualität stellen
        DataLine.Info = new DataLine.Info(TargetDataLine.class,format);



    }
}