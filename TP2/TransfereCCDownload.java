import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

class TransfereCCDownload extends Thread{
    AgenteUDP agente;
    InetAddress addressDest;
    String filename;
    LinkedList<PDU> received = new LinkedList<>();
    Lock l = new ReentrantLock();
    Condition empty  = l.newCondition();

    public TransfereCCDownload(AgenteUDP agent, String destip, String file_name) throws UnknownHostException{
        agente = agent;
        addressDest = InetAddress.getByName(destip);
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

        int contador = 0;
        while(contador < 57){
            PDU np = nextPDU();
            String data = new String(np.getData());
        }
    }
}
