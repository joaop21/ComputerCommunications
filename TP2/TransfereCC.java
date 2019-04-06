import java.net.*;
import java.io.*;

public class TransfereCC extends Thread {
    private AgenteUDP agente;
    private File fich;
    private String filename;

    public TransfereCC(File f) throws SocketException,Exception{
        this.agente = new AgenteUDP(this);
        this.fich = f;
        filename = f.getName();
    }

    public void recebePDU(PDU p){
        // tranformar em pdu
        // alterar estado
        /**try{
            agente.sendPDU(p.getData(),InetAddress.getByName(p.getSourceIP()),p.getSourcePort());
        } catch(Exception e){
            e.printStackTrace();
        }*/
    }

    public void run(){

        // inicializa server que recebe packets
        new Thread(agente).start();

    }
}
