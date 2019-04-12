import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class TransfereCC extends Thread {
    AgenteUDP agente;
    final boolean upload;
    final boolean download;
    File fich;
    String filename;
    String destinationIP;
    Map<InetAddress,TransfereCCUpload> threads_upload = new HashMap<>();
    TransfereCCDownload tfd;

    ////////////////////////// CONSTRUTORES //////////////////////////
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

    ////////////////////////// RECEIVE //////////////////////////
    public Object deserializePDU(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public void recebePDU(DatagramPacket dp){
        byte[] data = dp.getData();
        InetAddress ipAddress = dp.getAddress();

        try{

            PDU p = (PDU) deserializePDU(data);

            if(this.upload == true){
                TransfereCCUpload tup = threads_upload.get(ipAddress);
                if(tup == null){

                    // cria um novo tranfereCC
                    TransfereCCUpload ntup;
                    ntup = new TransfereCCUpload(agente,ipAddress,this.fich);

                    // inicia thread
                    new Thread(ntup).start();

                    // insere no hashmap
                    threads_upload.put(ipAddress,ntup);
                    ntup.recebePDU(p);
                } else{
                    tup.recebePDU(p);
                }
            } else{
                tfd.recebePDU(p);
            }

        } catch(Exception e){
            e.printStackTrace();
        }

    }

    ////////////////////////// RUN //////////////////////////
    public void run(){
        try{
            // inicializa server que recebe packets
            Thread agent = new Thread(agente);
            agent.start();

            if(this.download == true){
                tfd = new TransfereCCDownload(agente,destinationIP,filename);
                new Thread(tfd).run();
            }

            agent.interrupt();
        } catch(UnknownHostException e){
            e.printStackTrace();
        }

    }
}
