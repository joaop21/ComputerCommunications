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
    Estado estado;
    InetAddress addressDest;
    LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty  = l.newCondition();
    volatile int window = 0;
    LinkedList<Integer> toRetransmit = new LinkedList<>();


    public ThreadUpload(AgenteUDP agent, TransfereCC tf,InetAddress destip) throws UnknownHostException, IOException{
        agente = agent;
        tfcc = tf;
        estado = new Estado();
        addressDest = destip;
    }

    /*
        Insere um PDU na lista ligada (ao fim por omissao) e acorda a thread
    possivelmente bloqueada.
    */
    public void recebePDU(PDU p){
        l.lock();
        try{
            System.out.println("PDU from Host: " + this.addressDest + " FLAG: " + p.pdu() + " Seq Number: " + p.getSequenceNumber());
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
        Envia ficheiro para o destino
    */
    public void dataTransfer(){

        int num_segment = tfcc.segmentNumber();
        Boolean validados[] = new Boolean[num_segment];
        Arrays.fill(validados, false);

        window = estado.getReceiveWindow();
        int segment = 0;
        while(validados.length < num_segment || segment < num_segment){
            while(window < estado.getReceiveWindow()){
                String data = tfcc.getPartOfFile(segment);
                PDU p = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), "",false, false, false, true, data.getBytes());
                agente.sendPDU(p,addressDest,7777);
                estado.incrementSequenceNumber(1024);
                segment++;
                window--;
            }
        }



        /*
        int num_segment = tfcc.segmentNumber();
        int segment = 0;

        for(int i = 0 ; i < num_segment ; i++ , segment += 1024){
            String data = tfcc.getPartOfFile(segment);
            PDU p = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), "",false, false, false, true, data.getBytes());
            agente.sendPDU(p,addressDest,7777);
            estado.incrementSequenceNumber(1024);

            try{
                sleep(1);
            } catch(Exception e){
                e.printStackTrace();
            }

            while(received.size() > 0){
                PDU r = nextPDU();
                if(r.getACK()){
                    String part = tfcc.getPartOfFile(r.getAckNumber());
                    PDU np = new PDU(r.getAckNumber(), r.getSequenceNumber()+1, "",false, false, false, true, data.getBytes());
                    agente.sendPDU(np,addressDest,7777);
                }
            }
        }*/

        estado.setNextState();
        endConnection(segment);

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

        estado.setFirstDataAckNumber(estado.getAckNumber());
        estado.setNextState();
    }

    /*
        Método que define o fim de uma conexão
    */
    void endConnection(int fin_seq_number){
        // envia FIN
        PDU fin = new PDU(fin_seq_number, 3, new String(), false, true, false, false, new byte[0]);
        agente.sendPDU(fin,addressDest,7777);

        // recebe FINACK
        while(true){
          PDU finack = nextPDU();
          if(finack.getFIN() == true && finack.getACK() == true){
              break;
            }
          }

          // envia ACK
          PDU ack = new PDU(fin_seq_number+2, 3, new String(), false, false, true, false, new byte[0]);
          agente.sendPDU(ack,addressDest,7777);
    }

    /*
        Este método vai ter de ser otimizado
    */
    public void run(){

        beginConnection();

        dataTransfer();

        tfcc.removeConnection(addressDest);

    }
}
