import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.*;

public class TransfereCC extends Thread {
    AgenteUDP agente;
    final boolean upload;
    final boolean download;
    File fich;
    String filename;
    String destinationIP;
    Map<InetAddress,TransfereCCUpload> threads_upload = new HashMap<>();
    Lock l = new ReentrantLock();
    TransfereCCDownload tfd;

    ////////////////////////// CONSTRUTORES //////////////////////////
    /*
        Construtor usado para quando é pretendido fazer upload
    */
    public TransfereCC(File f) throws SocketException,Exception{
        agente = new AgenteUDP(this);
        this.upload = true;
        this.download = false;
        this.fich = f;
        this.filename = f.getName();
        destinationIP = "";
    }

    /*
        Construtor usado quando é pretendido fazer download
    */
    public TransfereCC(String file, String destip) throws SocketException,Exception{
        this.agente = new AgenteUDP(this);
        this.upload = false;
        this.download = true;
        this.fich = null;
        this.filename = file;
        destinationIP = destip;
    }

    ////////////////////////// RECEIVE //////////////////////////
    /*
        Método que passa um array de byte para um objeto.
    */
    public Object deserializePDU(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    /*
        Método que recebe Datagramas provenientes do AgenteUDP e entrega nas
    threads responsáveis por um dado IP.
    */
    public void recebePDU(DatagramPacket dp){
        // Returns the data buffer.
        byte[] data = dp.getData();
        // Returns the raw IP address of this InetAddress object.
        InetAddress ipAddress = dp.getAddress();

        try{
            // Deserialize the datagram's data: bytes -> PDU
            PDU p = (PDU) deserializePDU(data);

            // It's a Upload ...
            if(this.upload == true){
                // Get thread associated with an IP address.
                TransfereCCUpload tup = threads_upload.get(ipAddress);

                // If thread doesn't exist ...
                if(tup == null){

                    // cria um novo tranfereCC
                    TransfereCCUpload ntup;
                    ntup = new TransfereCCUpload(agente,this,ipAddress,this.fich);

                    // inicia thread
                    new Thread(ntup).start();

                    // insere no hashmap
                    l.lock();
                    threads_upload.put(ipAddress,ntup);
                    l.unlock();

                    // TransfereCCUpload processes the PDU.
                    ntup.recebePDU(p);
                } else{
                    // TransfereCCUpload processes the PDU.
                    tup.recebePDU(p);
                }
            } // It's a Download ...
            else{
                // TransfereCCDownload processes the PDU.
                tfd.recebePDU(p);
            }

        } catch(Exception e){
            e.printStackTrace();
        }

    }

    public void removeConnection(InetAddress ip){
        l.lock();
        try{
            threads_upload.remove(ip);
        } finally{
            l.unlock();
        }
    }

    ////////////////////////// RUN //////////////////////////
    public void run(){
        try{
            // inicializa server (AgenteUDP) que recebe packets
            Thread agent = new Thread(agente);
            agent.start();

            // It's a Download ...
            if(this.download == true){
                tfd = new TransfereCCDownload(agente,destinationIP,filename);
                new Thread(tfd).run();
            }

            // Interrupts this thread.
            agente.closeAgent();

        } catch(UnknownHostException e){
            e.printStackTrace();
        }

    }
}
