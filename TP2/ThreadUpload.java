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
    volatile int window = 0;
    volatile int validados[];
    LinkedList<Integer> toRetransmit = new LinkedList<>();


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

            System.out.println("PDU from Host: " + this.addressDest + " FLAG: " + p.pdu() + " Seq Number: " + p.getSequenceNumber() + " Ack Number: " + p.getAckNumber());

            // In case that the transfer is being done
            if(estado.getEstado() == TransferState.ESTABLISHED && p.getACK() == true){
                int index = (p.getAckNumber() - estado.getFirstDataSequenceNumber())/1024;

                // in case of receiving the confirmation of the last segment
                if(index >= validados.length){
                    for(index -= 1; index >= 0 ; index--)
                        validados[index] = 5; // confirmed

                    estado.setNextState();
                    return;
                }

                // checks if an ack has already been received
                if(validados[index] == 1){
                    if(!toRetransmit.contains(p.getAckNumber()))
                        toRetransmit.add(p.getAckNumber());
                } else{
                    validados[index] = 1; // one ack received
                    index--;
                    for(; index >= 0 ; index--)
                        validados[index] = 5; // confirmed
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
        Envia ficheiro para o destino
    */
    public void dataTransfer(){

        int num_segment = tfcc.segmentNumber();
        validados = new int[num_segment];
        Arrays.fill(validados, 0);

        window = estado.getReceiveWindow();
        int segment = 0;
        while(segment < num_segment){
            if(window <= estado.getReceiveWindow() && window > 0){
                String data = tfcc.getPartOfFile(estado.getSequenceNumber()-estado.getFirstDataSequenceNumber());
                PDU p = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), new String(),false, false, false, true, data.getBytes());
                agente.sendPDU(p,addressDest,7777);
                estado.incrementSequenceNumber(1024);
                estado.incrementAckNumber(1);
                segment++;
                window--;
            }

            while(toRetransmit.size() > 0){
                System.out.println("tou preso no retransmit");
                if(window <= estado.getReceiveWindow() && window > 0){
                    int rn = toRetransmit.getFirst();
                    System.out.println(rn + "\n" + estado.getFirstDataAckNumber() + "\n" + (rn-estado.getFirstDataAckNumber()));
                    String data = tfcc.getPartOfFile(rn-estado.getFirstDataAckNumber());
                    PDU p = new PDU(estado.getFirstDataSequenceNumber() + rn, estado.getAckNumber(), "",false, false, false, true, data.getBytes());
                    agente.sendPDU(p,addressDest,7777);
                    window--;
                }
            }
        }

        while(validatedSegments() < num_segment){
            while(toRetransmit.size() > 0){
                System.out.println("tou preso");
                if(window <= estado.getReceiveWindow() && window > 0){
                    int rn = toRetransmit.getFirst();
                    String data = tfcc.getPartOfFile(rn-estado.getFirstDataSequenceNumber());
                    PDU p = new PDU(estado.getFirstDataSequenceNumber() + rn, estado.getAckNumber(), "",false, false, false, true, data.getBytes());
                    agente.sendPDU(p,addressDest,7777);
                    window--;
                }
            }
        }

        System.out.println("acabei");
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
        PDU synack = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), String.valueOf(tfcc.segmentNumber()), true, false, true, false, new byte[0]);
        agente.sendPDU(synack,addressDest,7777);
        estado.incrementSequenceNumber(1);

        // recebe ACK
        while(true){
            PDU ack = nextPDU();
            if(ack.getACK() == true){
                estado.setAckNumber(ack.getSequenceNumber()+1);
                break;
            }
        }

        estado.setFirstDataAckNumber(estado.getAckNumber()-1);
        estado.setFirstDataSequenceNumber(estado.getSequenceNumber());
        estado.setNextState();
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
