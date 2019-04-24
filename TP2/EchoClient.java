import java.net.*;
import java.util.*;

public class EchoClient {
    private DatagramSocket socket;
    private InetAddress address;
    private InetAddress addressDest;

    private byte[] buf;

    public EchoClient() throws Exception{
        socket = new DatagramSocket();
        address = InetAddress.getByName(activeIP());
        addressDest = InetAddress.getByName("172.26.39.154");
    }

    /**
        Função que tira um IP duma interface ativa
    */
    public String activeIP(){
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                }
            }
            return ip;
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public String sendEcho(String msg) throws Exception{
        String ola = "OLA";
        PDU p = new PDU(0, 0, "",false, false, false, true,ola.getBytes());
        buf = p.serialize();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, addressDest, 7777);
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
