import java.net.*;
import java.io.*;

public class TransfereCC extends Thread {
    private AgenteUDP agente;
    private File fich;

    public TransfereCC(File f) throws SocketException,Exception{
        this.agente = new AgenteUDP(this);
        this.fich = f;
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
}
