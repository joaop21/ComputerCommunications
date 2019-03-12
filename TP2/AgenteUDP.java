import java.net.*;

public class AgenteUDP extends Thread {

    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256]; // valor a ver////////////////

    public AgenteUDP() throws SocketException{
        socket = new DatagramSocket(7777); // localhost machine port
    }

    public DatagramPacket receivePDU(){
        DatagramPacket packet = null;
        try{
            packet = new DatagramPacket(buf, buf.length); // cria dimensao de segmento a receber
            socket.receive(packet);

            String received = new String(packet.getData(), 0, packet.getLength()); // 0 é offset

            if (received.equals("end")){ //// perguntar ao stor
                running = false;
                // continue;
            }

        } catch(Exception e){
            e.printStackTrace();
        } finally{
            return packet;
        }
    }

    public void sendPDU(DatagramPacket packet){
        try{
            socket.send(packet);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void closeSocket(){
        socket.close();
    }

    public static void main(String[] args) {
        try{
            new Thread(new AgenteUDP()).run();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
