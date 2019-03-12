import java.net.*;

public class TransfereCC extends Thread {
    private AgenteUDP agente;

    public TransfereCC() throws SocketException,Exception{
        this.agente = new AgenteUDP(this);
    }

    public void recebePDU(String data, InetAddress ipAddress, int port){
        // tranformar em pdu
        // alterar estado

        agente.sendPDU(data,ipAddress,port); // isto é meramente indicativo só para funcionar
    }

    public void run(){

        // inicializa server que recebe packets
        new Thread(agente).start();

    }

    public static void main(String[] args) {
            try{
                new Thread(new TransfereCC()).run();
            } catch(Exception e){
                e.printStackTrace();
            }
    }
}
