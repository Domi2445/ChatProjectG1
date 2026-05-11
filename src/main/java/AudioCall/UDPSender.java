package AudioCall;
import java.net.*;
public class UDPSender

{


    private DatagramSocket socket;
    private InetAddress address;
    private int port ;


    public UDPSender(String ip,int port ) throws Exception
    {
        this.socket = new DatagramSocket();
        this.address =  InetAddress.getByName(ip);
        this.port = port ;
    }

    public void send(byte[] data) throws Exception

    {
            DatagramPacket packet = new DatagramPacket(data, data.length,address ,port);
            socket.send(packet);
    }

    public void close()
    {
            socket.close();
    }


	public void sendString(String text) throws Exception
	{
		byte[] data = text.getBytes();
		socket.send(new DatagramPacket(data, data.length, address, port));
	}
}
