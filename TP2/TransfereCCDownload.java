import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

class TransfereCCDownload extends Thread{
    AgenteUDP agente;
    private InetAddress addressDest;
    LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty  = l.newCondition();

    public TransfereCCDownload(AgenteUDP agent, String destip) throws UnknownHostException{
        agente = agent;
        this.addressDest = InetAddress.getByName(destip);
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
        String ola = "OLA";
        PDU p = new PDU(0, 0, 1024, false, false, false, true,ola.getBytes());
        agente.sendPDU(p,addressDest,7777);

        while(true)
            nextPDU();
    }
}
