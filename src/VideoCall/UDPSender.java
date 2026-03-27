package VideoCall;
import java.net.*;
public class UDPSender

{


        public static void main(String[] args) throws Exception
        {
            DatagramSocket socket = new DatagramSocket();
            String message = "Hallo per UDP!";
            byte[] data = message.getBytes();

            InetAddress address = InetAddress.getByName("127.0.0.1");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, 7000);

            socket.send(packet);
            System.out.println("Gesendet: " + message);
            socket.close();
        }

}
