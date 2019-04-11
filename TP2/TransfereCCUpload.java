import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

class TransfereCCUpload extends Thread{
    AgenteUDP agente;
    private InetAddress addressDest;
    private LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty  = l.newCondition();

    public TransfereCCUpload(AgenteUDP agent, InetAddress destip) throws UnknownHostException{
        agente = agent;
        this.addressDest = destip;
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

    public PDU removePDU() throws InterruptedException{
        l.lock();
        PDU p;
        try{

            while(received.size() == 0)
                empty.await();

            p = received.removeFirst();
        } finally{
            l.unlock();
        }
        return p;
    }

    public void run(){

    }
}
