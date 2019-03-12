import java.net.*;

public class TransfereCC extends Thread {
    private AgenteUDP agente;

    public TransfereCC() throws SocketException{
        this.agente = new AgenteUDP();
    }

    public void run(){

        DatagramPacket packet = agente.receivePDU();

        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        packet = new DatagramPacket(buf, buf.length, address, port);
        String received = new String(packet.getData(), 0, packet.getLength()); // 0 é offset
    }

    public static void main(String[] args) {
        try{
            new Thread(new AgenteUDP()).run();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
