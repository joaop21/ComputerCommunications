import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;

class TransfereCCDownload extends Thread{
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
    Lock l = new ReentrantLock();
    Condition empty  = l.newCondition();

    public TransfereCCDownload(AgenteUDP agent, String destip, String file_name) throws UnknownHostException{
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
            System.out.println("PDU from Host: " + this.addressDest);
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
        Este método vai ter de ser otimizado
    */
    public void run(){
        try{
            String ola = "OLA";
            PDU p = new PDU(0, 0, 1024, false, false, false, true,ola.getBytes());
            // AgenteUDP sends PDU
            agente.sendPDU(p,addressDest,7777);

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

            // devido a este ciclo, o ultimo segmento nao é escrito
            // temos ainda de passar no inicio de conexão quantos segmentos vamos enviar
            int contador = 0;
            while(contador > -1){
                PDU np = nextPDU();
                String data = new String(np.getData());
                writer.write(data);
                contador++;
            }
            // Closes the stream, flushing it first.
            writer.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
