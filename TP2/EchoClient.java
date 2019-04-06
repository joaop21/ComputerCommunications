import java.net.*;

public class EchoClient {
    private DatagramSocket socket;
    private InetAddress address;

    private byte[] buf;

    public EchoClient() throws Exception{
        socket = new DatagramSocket();
        address = InetAddress.getLocalHost();
    }

    public String sendEcho(String msg) throws Exception{
        PDU p = new PDU(7777, 7777, address.getHostAddress(), "192.168.1.65", "DATA", "teste de pdus");
        buf = p.serialize();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 7777);
        socket.send(packet);
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    public void socketClose() {
        socket.close();
    }

    public static void main(String[] args) {
        try{
            EchoClient ec = new EchoClient();
            String res = ec.sendEcho("ola!");
            System.out.println(res + "\n");
            ec.socketClose();
        } catch(Exception e){}
    }
}
