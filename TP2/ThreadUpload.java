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
    final Condition empty  = l.newCondition();
    final Condition finalAck  = l.newCondition();
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
            System.out.println("PDU from Host: " + this.addressDest + " FLAG: " + p.pdu() +
                               " Seq Number: " + p.getSequenceNumber() + " Ack Number: " + p.getAckNumber());

            if(estado.getEstado() == TransferState.ESTABLISHED && p.getACK() == true){
                int index = (p.getAckNumber() - estado.getSequenceNumber() - 1)/1024;

                // in case of receiving the confirmation of the last segment
                if(index >= validados.length){
                    for(index -= 1; index >= 0 ; index--)
                        validados[index] = 5; // confirmed

                    finalAck.signal();
                    estado.setNextState();
                    return;
                }

                // checks if an ack has already been received
                if(validados[index] == 1){
                    // retransmits PDU
                    PDU retransmit = tfcc.getPDU(index+1);
                    retransmit.incrementSequenceNumber(estado.getSequenceNumber());
                    retransmit.setAckNumber(p.getSequenceNumber()+1);
                    agente.sendPDU(retransmit,addressDest,7777);
                    return;
                }

                validados[index] = 1; // one ack received
                // validate the smaller PDUs
                for(index -= 1; index >= 0 ; index--){

                    if(validados[index] == 1)
                        if(window < estado.getReceiveWindow()) window++;

                    validados[index] = 5;
                }


                if(window < estado.getReceiveWindow()) window++;
                return;
            }

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

    public int validatedSegments(){
        int res = 0;
        for(int i : validados)
            if(i == 5 || i == 1) res++;
        return res;
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


    /*
        Envia ficheiro para o destino
    */
    public void dataTransfer(){

        int data_segments = tfcc.numberOfPDUs();
        validados = new int[data_segments];
        Arrays.fill(validados, 0);

        while(pdu_number <= data_segments){
            if(window <= estado.getReceiveWindow() && window > 0){
                PDU packet = tfcc.getPDU(pdu_number);
                packet.incrementSequenceNumber(estado.getSequenceNumber());
                agente.sendPDU(packet,addressDest,7777);
                pdu_number++;
                window--;
            }
        }
        System.out.println("acabei transf");

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
