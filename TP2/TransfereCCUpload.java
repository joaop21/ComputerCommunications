import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;
import java.nio.file.Files;

class TransfereCCUpload extends Thread{
    AgenteUDP agente;
    InetAddress addressDest;
    File file;
    byte[] file_byte;
    LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty  = l.newCondition();

    public TransfereCCUpload(AgenteUDP agent, InetAddress destip, File fich) throws UnknownHostException, IOException{
        agente = agent;
        addressDest = destip;
        file = fich;
        file_byte = Files.readAllBytes(file.toPath());
    }

    public void recebePDU(PDU p){
        l.lock();
        try{
            System.out.println("PDU from Host: " + this.addressDest);
            received.add(p);
            empty.signal();
        } finally{
            l.unlock();
        }
    }

    public PDU nextPDU(){
        l.lock();
        PDU p;
        try{

            while(received.size() == 0)
                empty.await();

            p = received.removeFirst();

            return p;
        } catch(InterruptedException e){
            e.printStackTrace();
        }finally{
            l.unlock();
        }
        return null;
    }

    public void run(){

        while(true){
            PDU p = nextPDU();
        }

    }
}
