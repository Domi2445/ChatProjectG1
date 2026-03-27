package VideoCall;
import java.net.*;

public class UDPReciever

{
    public static void main(String[] args) throws Exception
    {

        DatagramSocket socket = new DatagramSocket(7000);
        byte[] buffer = new byte[1024]; // hier werden die eingehenden Daten gespeichert


        //Objekt das das eingehende Paket empfängt und die Daten in buffer speichert
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);

        System.out.println("Warte afu Daten");

        socket.receive(packet);



        // Rohe Bytes aus dem Paket nehmen und zurück in einen String umwandeln
        String message = new String(packet.getData(),0,packet.getLength());


        System.out.println("Empfanger: " + message);
        socket.close();
    }

}
