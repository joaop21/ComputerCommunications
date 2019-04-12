import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;
import java.nio.file.Files;

class TransfereCCUpload extends Thread{
    AgenteUDP agente;
    InetAddress addressDest;
    File file;
    FileInputStream fis;
    LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty  = l.newCondition();

    public TransfereCCUpload(AgenteUDP agent, InetAddress destip, File fich) throws UnknownHostException, IOException{
        agente = agent;
        addressDest = destip;
        file = fich;
        fis = new FileInputStream(fich);
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

    public void sendFile(){
        // teste a 1024 bytes
        /**int file_length = file_byte.length;
        for(int i = 0 , seq = 0; i < file_length ; i+= 1024, seq++){
            byte[] data = new byte[1024];
            for(int j = 0 ; (j < 1024) || (i+j < file_length) ; j++)
                data[j] = file_byte[i + j];

            PDU p = new PDU(0, 0, 1024, false, false, false, true,file_byte);
            agente.sendPDU(p,addressDest,7777);
        }*/
        try{
            InputStreamReader isr = new InputStreamReader(fis);
            long file_length = file.length();
            char[] file_char = new char[(int)file_length];
            isr.read(file_char, 0, (int)file_length);

            char[] lidos = new char[1024];
            lidos[0] = file_char[0];
            int seq = 1;
            for(int i = 1; i < file_length ; i++){
                if(i%1024 != 0){
                    lidos[i%1024] = file_char[i];
                }else{
                    String data = new String(lidos);
                    PDU p = new PDU(seq, 0, 1024, false, false, false, true,data.getBytes());
                    agente.sendPDU(p,addressDest,7777);
                    seq++;
                    lidos = new char[1024];
                    lidos[0] = file_char[i];
                }
            }
            System.out.println(seq);
        } catch(Exception e){
            e.printStackTrace();
        }


    }

    public void run(){

        PDU p = nextPDU();
        sendFile();

    }
}
