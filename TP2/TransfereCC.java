import java.net.*;
import java.io.*;

public class TransfereCC extends Thread {
    AgenteUDP agente;
    private final boolean upload;
    private final boolean download;
    private File fich;
    private String filename;
    String destinationIP;
    //LinkedList<PDU> received = new LinkedList<>();

    public TransfereCC(File f) throws SocketException,Exception{
        agente = new AgenteUDP(this);
        this.upload = true;
        this.download = false;
        this.fich = f;
        this.filename = f.getName();
        destinationIP = "";
    }

    public TransfereCC(String file, String destip) throws SocketException,Exception{
        this.agente = new AgenteUDP(this);
        this.upload = false;
        this.download = true;
        this.fich = null;
        this.filename = file;
        destinationIP = destip;
    }

    public Object deserializePDU(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public void recebePDU(DatagramPacket dp){
        byte[] data = dp.getData();
        InetAddress ipAddress = dp.getAddress();
        int port = dp.getPort();

        System.out.println("Host: " + ipAddress + "  Port: " + port);

        try{
            PDU p = (PDU) deserializePDU(data);
            System.out.println("Data: " + p.getData());
        } catch(Exception e){
            e.printStackTrace();
        }



    }

    public void run(){
        try{
            // inicializa server que recebe packets
            Thread agent = new Thread(agente);
            agent.start();

            if(this.download == true)
                new Thread(new TransfereCCDownload(agente,destinationIP)).run();

            agent.interrupt();
        } catch(UnknownHostException e){
            e.printStackTrace();
        }

    }
}



/**
    Classe usada para quando é pretendido fazer download dum ficheiro
*/
class TransfereCCDownload extends Thread{
    AgenteUDP agente;
    private InetAddress addressDest;

    public TransfereCCDownload(AgenteUDP agent, String destip) throws UnknownHostException{
        agente = agent;
        this.addressDest = InetAddress.getByName(destip);
    }

    public void run(){
        PDU p = new PDU(0, 0, 1024, false, false, false, true,"OLA");
        agente.sendPDU(p,addressDest,7777);
    }
}
