import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.locks.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;

class TransfereCCUpload extends Thread{
    AgenteUDP agente;
    InetAddress addressDest;
    File file;
    FileInputStream fis;
    /*
    LinkedList is a doubly-linked list implementation of the List and Deque interfaces.
    LinkedList allows for constant-time insertions or removals using iterators, but only
    sequential access of elements. In other words, LinkedList can be searched forward and backward
    but the time it takes to traverse the list is directly proportional to the size of the list.
    */
    // LinkedList to process the PDUs
    LinkedList<PDU> received = new LinkedList<>();
    final Lock l = new ReentrantLock();
    final Condition empty  = l.newCondition();
    int mss;
    // Segmented File because of MSS
    Map<Integer,String> segmented_file = new HashMap<>();

    public TransfereCCUpload(AgenteUDP agent, InetAddress destip, File fich) throws UnknownHostException, IOException{
        agente = agent;
        addressDest = destip;
        file = fich;
        // Creates a FileInputStream by opening a connection to an actual file, the file named by the File object file in the file system.
        fis = new FileInputStream(fich);
        mss = 1024;
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
        Divide o ficheiro consoante o MSS e coloca-o num MAP
    */
    public void divideFile(){
        try{
            // Creates an InputStreamReader that uses the default charset.
            InputStreamReader isr = new InputStreamReader(fis);
            // Returns the length of the file denoted by this abstract pathname.
            long file_length = file.length();
            char[] file_char = new char[(int)file_length];
            // Reads characters into a portion of an array.
            isr.read(file_char, 0, (int)file_length);

            char[] lidos;
            if(file_length < mss)
                lidos = new char[(int) file_length];
            else lidos = new char[mss];

            lidos[0] = file_char[0];
            int seq = 0;
            for(int i = 1; i < file_length ; i++){
                if(i%mss != 0){
                    lidos[i%mss] = file_char[i];
                }else{
                    String data = new String(lidos);
                    segmented_file.put(seq,data);
                    seq+=mss;

                    if(file_length-i < mss)
                        lidos = new char[(int) file_length-i];
                    else lidos = new char[mss];

                    lidos[0] = file_char[i];
                }
            }
            String data = new String(lidos);
            segmented_file.put(seq,data);
            System.out.println(segmented_file.size());

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
        Envia ficheiro para o destino
    */
    public void sendFile(){

        int num_segment = segmented_file.size();
        for(int i = 0 , seq = 0 ; i < num_segment ; i++ , seq += mss){
            String data = segmented_file.get(seq);
            PDU p = new PDU(seq, 0, 1024, "",false, false, false, true, data.getBytes());
            // AgenteUDP sends PDU
            agente.sendPDU(p,addressDest,7777);
        }

    }

    /*
        Método que define o início de uma conexão
    */
    void beginConnection(){
        // Recebe SYN
        while(true){
            PDU syn = nextPDU();
            if(syn.getSYN() == true){
                mss = syn.getMSS();
                break;
            }
        }

        // divide ficheiro consoante o MSS
        divideFile();

        // envia SYNACK
        PDU synack = new PDU(1, 0, 1024, String.valueOf(segmented_file.size()), true, false, true, false, new byte[0]);
        agente.sendPDU(synack,addressDest,7777);

        // recebe ACK
        while(true){
            PDU ack = nextPDU();
            if(ack.getACK() == true)
                break;
        }
    }

    /*
        Este método vai ter de ser otimizado
    */
    public void run(){

        beginConnection();

        sendFile();

    }
}
