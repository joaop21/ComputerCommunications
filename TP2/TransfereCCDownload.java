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
        filename = file_name;
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
        try{
            String ola = "OLA";
            PDU p = new PDU(0, 0, 1024, false, false, false, true,ola.getBytes());
            agente.sendPDU(p,addressDest,7777);



            File file = new File(filename);

            //Create the file
            if (file.createNewFile()){
                System.out.println("File is created!");
            } else {
                System.out.println("File already exists.");
            }

            //Write Content
            FileWriter writer = new FileWriter(file);

            int contador = 0;
            while(contador < 57){
                PDU np = nextPDU();
                String data = new String(np.getData());
                writer.write(data);
            }
            writer.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
