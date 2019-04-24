import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

class ThreadDownload extends Thread{
    AgenteUDP agente;
    InetAddress addressDest;
    String filename;
    /*
    LinkedList is a doubly-linked list implementation of the List and Deque interfaces.
    LinkedList allows for constant-time insertions or removals using iterators, but only
    sequential access of elements. In other words, LinkedList can be searched forward and backward
    but the time it takes to traverse the list is directly proportional to the size of the list.
    */
    // LinkedList to process the PDUs
    LinkedList<PDU> received = new LinkedList<>();
    int segment_num;
    Lock l = new ReentrantLock();
    Condition empty  = l.newCondition();

    public ThreadDownload(AgenteUDP agent, String destip, String file_name) throws UnknownHostException{
        agente = agent;
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
        Método que define o início de uma conexão
        Como é o lado do cliente este é quem envia um SYN,
        espera um SYNACK
        e envia um ACK
    */
    void beginConnection(){
        // envia SYN
        PDU syn = new PDU(0, 1, new String(), true, false, false, false, new byte[0]);
        agente.sendPDU(syn,addressDest,7777);

        // recebe SYNACK
        while(true){
            PDU synack = nextPDU();
            if(synack.getSYN() == true && synack.getACK() == true){
                segment_num = Integer.valueOf(synack.getOptions());
                break;
            }
        }

        // envia ACK
        PDU ack = new PDU(2, 1, new String(), false, false, true, false, new byte[0]);
        agente.sendPDU(ack,addressDest,7777);
    }

    /*
        Método que define o fim de uma conexão
    */
    void endConnection(){

        int fin_seq_number;

        // Recebe FIN
        while(true){
            PDU fin = nextPDU();
            if(fin.getFIN() == true){
                fin_seq_number = fin.getSequenceNumber();
                break;
            }
        }

        // envia FINACK
        PDU finack = new PDU(fin_seq_number+1, 2, new String(), false, true, true, false, new byte[0]);
        agente.sendPDU(finack,addressDest,7777);

        // recebe ACK
        while(true){
            PDU ack = nextPDU();
            if(ack.getACK() == true)
                break;
        }
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

            int tam = parts.length;
            for(int i = 0 ; i < tam ; i++)
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
            beginConnection();

            int segment = 0;
            int expected_segment = 0;
            String file_parts[] = new String[segment_num];

            while(segment < segment_num){
                PDU np = nextPDU();
                String data = new String(np.getData());
                int seq_number = np.getSequenceNumber()/1024;

                if(seq_number > expected_segment){
                    PDU ack = new PDU(0, expected_segment*1024, new String(), false, false, true, false, new byte[0]);
                    agente.sendPDU(ack,addressDest,7777);
                } else{
                    expected_segment++;
                    segment++;
                }
                file_parts[seq_number] = data;
            }

            createFile(file_parts);

            endConnection();

            System.out.println("File was correctly Downloaded ...");
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
