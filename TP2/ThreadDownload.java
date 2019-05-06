import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

class ThreadDownload extends Thread{
    TransfereCC tfcc;
    AgenteUDP agente;
    Estado estado;
    InetAddress addressDest;
    String filename;
    LinkedList<PDU> received = new LinkedList<>();
    int segment_num;
    Lock l = new ReentrantLock();
    Condition empty  = l.newCondition();
    PDU ultimo_enviado;
    Thread cpbt;

    public ThreadDownload(AgenteUDP agent, String destip, String file_name, TransfereCC tfccn) throws UnknownHostException{
        tfcc = tfccn;
        agente = agent;
        estado = new Estado();
        // Determines the IP address of a host, given the host's name.
        addressDest = InetAddress.getByName(destip);
        filename = file_name;
    }

    /*
        Insere um PDU na lista ligada (ao fim por omissao) e acorda a thread
    possivelmente bloqueada.
    */
    public void recebePDU(PDU p){
        l.lock();
        try{
            //System.out.println("PDU from Host: " + this.addressDest + " FLAG: " + p.pdu() + " Seq Number: " + p.getSequenceNumber() + " Ack Number: " + p.getAckNumber());
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
            while(received.size() == 0){
                empty.await(estado.getTimeout(),TimeUnit.MILLISECONDS);
                // deu timeout
                if(received.size() == 0)
                    return null;
            }

            p = received.removeFirst();
            return p;
        } catch(InterruptedException e){
            e.printStackTrace();
        }finally{
            l.unlock();
        }
        return null;
    }


    public void messageTimeout(){
        System.out.println("Servers cannot be reached ...");
    }

    public void messageLosedConnection(){
        System.out.println("Connection lost ...");
    }

    /*
        Método que define o início de uma conexão
        Como é o lado do cliente este é quem envia um SYN,
        espera um SYNACK
        e envia um ACK
    */
    public int beginConnection(){
        // envia SYN
        estado.setInitialRandomSequenceNumber();
        estado.setReceiveWindow(55);
        PDU syn = new PDU(estado.getSequenceNumber(), 0, new String(), true, false, false, false, new byte[0]);
        agente.sendPDU(syn,addressDest,7777);
        estado.incrementSequenceNumber(1);

        // recebe SYNACK
        while(true){
            PDU synack = nextPDU();
            if(synack != null){
                if(synack.getSYN() == true && synack.getACK() == true){
                    segment_num = Integer.valueOf(synack.getOptions());
                    estado.setAckNumber(synack.getSequenceNumber()+1);
                    break;
                }
            } else{
                int timeout_count = estado.timeoutReceived();
                if(timeout_count == 3){
                    tfcc.interruptDownload();
                    messageTimeout();
                    return -1;
                }
                agente.sendPDU(syn,addressDest,7777);
            }
        }

        // envia ACK
        PDU ack = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), new String(), false, false, true, false, new byte[0]);
        ultimo_enviado = ack;
        agente.sendPDU(ack,addressDest,7777);

        estado.setFirstDataSequenceNumber(estado.getSequenceNumber());
        estado.setFirstDataAckNumber(estado.getAckNumber());
        estado.setNextState();
        return 0;
    }

    /*
        Método que define o fim de uma conexão
    */
    public int endConnection(PDU ultimo_enviado){

        // Recebe FIN
        while(true){
            PDU fin = nextPDU();
            if(fin != null){
                if(fin.getFIN() == true){
                    estado.setSequenceNumber(fin.getAckNumber());
                    estado.setAckNumber(fin.getSequenceNumber()+1);
                    break;
                }
            } else{
                int timeout_count = estado.timeoutReceived();
                if(timeout_count == 3){
                    cpbt.interrupt();
                    tfcc.interruptDownload();
                    messageTimeout();
                    return -1;
                }
                agente.sendPDU(ultimo_enviado, addressDest,7777);
            }
        }

        // envia FINACK
        PDU finack = new PDU(estado.getSequenceNumber(), estado.getAckNumber(), new String(), false, true, true, false, new byte[0]);
        agente.sendPDU(finack,addressDest,7777);

        // recebe ACK
        while(true){
            PDU ack = nextPDU();
            if(ack != null){
                if(ack.getACK() == true){
                    estado.setSequenceNumber(ack.getAckNumber());
                    estado.setAckNumber(ack.getSequenceNumber()+1);
                    break;
                }
            } else{
                int timeout_count = estado.timeoutReceived();
                if(timeout_count == 3){
                    cpbt.interrupt();
                    tfcc.interruptDownload();
                    messageTimeout();
                    return -1;
                }
                agente.sendPDU(finack,addressDest,7777);
            }
        }
        estado.setNextState();
        return 0;
    }

    /*
        Método que cria ficheiro
    */
    public void createFile(String[] parts){
        try{
            // Creates a new File instance by converting the given pathname string into an abstract pathname.
            File file = new File(filename);

            // Atomically creates a new, empty file named by this abstract pathname if and only if a file with this name does not yet exist.
            if (file.createNewFile()){
                System.out.println("File is created!");
            } else {
                System.out.println("File already exists. Information will be truncated.");
            }

            // Write Content: Constructs a FileWriter object given a File object.
            FileWriter writer = new FileWriter(file);

            //int tam = parts.length;
            for(int i = 0 ; i < segment_num ; i++)
                writer.write(parts[i]);

            // Closes the stream, flushing it first.
            writer.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
        Este método vai ter de ser otimizado
    */
    public void run(){
        try{
            int res = beginConnection();
            if(res == -1) return;

            ConsoleProgressBar cpb = new ConsoleProgressBar(segment_num);
            cpbt = new Thread(cpb);
            cpbt.start();

            int segment = 0;
            String file_parts[] = new String[segment_num];
            Arrays.fill(file_parts, "");
            int first_data_ack_number = estado.getFirstDataAckNumber();

            int retry = 0;
            while(segment < segment_num){
                PDU np = nextPDU();
                if(np != null){
                    int seq_number = (np.getSequenceNumber() - first_data_ack_number)/1024;

                    String data = new String(np.getData());
                    file_parts[seq_number] = data;

                    if(seq_number > segment){
                        retry++;
                        if(retry < 3){
                            System.out.println("Falta-me o " + seq_number);
                            PDU retransmit = new PDU(estado.getSequenceNumber(),first_data_ack_number + (segment * 1024), new String(), false, false, true, false, new byte[0]);
                            ultimo_enviado = retransmit;
                            agente.sendPDU(retransmit,addressDest,7777);
                        }
                    } else{
                        retry = 0;
                        while(segment < segment_num){
                            if(file_parts[segment] == "") break;
                            segment++;
                            cpb.incrementProgress();
                            estado.incrementSequenceNumber(1);
                        }
                        estado.setAckNumber(first_data_ack_number + segment*1024);
                        PDU pdu = new PDU(estado.getSequenceNumber(),estado.getAckNumber(), new String(), false, false, true, false, new byte[0]);
                        ultimo_enviado = pdu;
                        agente.sendPDU(pdu,addressDest,7777);
                    }
                } else{
                    int timeout_count = estado.timeoutReceived();
                    if(timeout_count == 3){
                        cpbt.interrupt();
                        tfcc.interruptDownload();
                        messageLosedConnection();
                        return;
                    }
                    agente.sendPDU(ultimo_enviado,addressDest,7777);
                }

            }

            res = endConnection(ultimo_enviado);
            if(res == -1) return;

            createFile(file_parts);

            System.out.println("File was correctly Downloaded ...");
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
