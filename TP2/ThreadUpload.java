import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

class ThreadUpload extends Thread{
    AgenteUDP agente;
    TransfereCC tfcc;
    Estado estado = new Estado();
    InetAddress addressDest;
    LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty = l.newCondition();
    final Condition finalAck = l.newCondition();
    volatile int window = 1;
    volatile int validados[];
    int pdu_number = 0;


    public ThreadUpload(AgenteUDP agent, TransfereCC tf,InetAddress destip) throws UnknownHostException, IOException{
        agente = agent;
        tfcc = tf;
        addressDest = destip;
    }

    /*
        Insere um PDU na lista ligada (ao fim por omissao) e acorda a thread
    possivelmente bloqueada.
    */
    public void recebePDU(PDU p){
        l.lock();
        try{
            // Appends the specified element to the end of this list.
            received.add(p);
            empty.signal();
        } finally{
            l.unlock();
        }
    }

    /*
        Vai à lista ligada caso esta tenha elementos (senao bloqueia), e retira
    o próximo PDU  analisar.
    */
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

    /*
        Método que define o início de uma conexão
    */
    void beginConnection(){
        // Recebe SYN
        while(true){
            PDU syn = nextPDU();
            if(syn.getSYN() == true){
                // inicia com um nº se sequencia random
                estado.setInitialRandomSequenceNumber();
                // sequence recebido é o numero de ack a enviar no pdu seguinte
                estado.setAckNumber(syn.getSequenceNumber()+1);
                estado.setReceiveWindow(syn.getReceiveWindow());
                break;
            }
        }

        // envia SYNACK
        PDU synack = tfcc.getPDU(pdu_number);
        synack.incrementSequenceNumber(estado.getSequenceNumber());
        synack.setAckNumber(estado.getAckNumber());
        agente.sendPDU(synack,addressDest,7777);
        pdu_number++;

        // recebe ACK
        while(true){
            PDU ack = nextPDU();
            if(ack.getACK() == true){
                estado.setAckNumber(ack.getSequenceNumber()+1);
                break;
            }
        }

        estado.setNextState();
    }


    public void signalFinalAck(){
        l.lock();
        finalAck.signal();
        l.unlock();
    }

    /*
        Envia ficheiro para o destino
    */
    public void dataTransfer(){
        int data_segments = tfcc.numberOfPDUs();
        validados = new int[data_segments];
        Arrays.fill(validados, -1);

        new Thread(new UploadReceiver(tfcc,this, estado, agente)).start();

        while(pdu_number <= data_segments){
            if(window <= estado.getProperWindow() && window > 0){
                PDU packet = tfcc.getPDU(pdu_number);
                packet.incrementSequenceNumber(estado.getSequenceNumber());
                estado.incrementAckNumber(1);
                packet.setAckNumber(estado.getAckNumber());
                agente.sendPDU(packet,addressDest,7777);
                pdu_number++;
                window--;
            }

            try{
                Thread.sleep(1);
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        l.lock();
        try{
            finalAck.await();
        } catch(Exception e){
            e.printStackTrace();
        } finally{ l.unlock();}

    }


    /*
        Método que define o fim de uma conexão
    */
    void endConnection(){
        // envia FIN
        PDU fin = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), new String(), false, true, false, false, new byte[0]);
        agente.sendPDU(fin,addressDest,7777);

        // recebe FINACK
        while(true){
            PDU finack = nextPDU();
            if(finack.getFIN() == true && finack.getACK() == true){
                estado.setSequenceNumber(finack.getAckNumber());
                estado.setAckNumber(finack.getSequenceNumber()+1);
                break;
            }
        }

        // envia ACK
        PDU ack = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), new String(), false, false, true, false, new byte[0]);
        agente.sendPDU(ack,addressDest,7777);
    }

    /*
        Este método vai ter de ser otimizado
    */
    public void run(){

        beginConnection();

        dataTransfer();

        endConnection();

        tfcc.removeConnection(addressDest);

    }
}