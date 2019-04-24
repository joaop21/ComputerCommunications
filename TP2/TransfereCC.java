import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.*;

public class TransfereCC extends Thread {
    AgenteUDP agente;
    private static final int MSS = 1024;
    final boolean upload;
    final boolean download;
    File fich;
    String filename;
    String destinationIP;
    Map<InetAddress,ThreadUpload> threads_upload = new HashMap<>();
    ThreadDownload tfd;
    Lock l = new ReentrantLock();
    Map<Integer,String> segmented_file = new HashMap<>();

    ////////////////////////// CONSTRUTORES //////////////////////////
    /*
        Construtor usado para quando é pretendido fazer upload
    */
    public TransfereCC(File f) throws SocketException,Exception{
        agente = new AgenteUDP(this);
        upload = true;
        download = false;
        fich = f;
        filename = f.getName();
        segmented_file = divideFile(fich);
        destinationIP = "";
    }

    /*
        Construtor usado quando é pretendido fazer download
    */
    public TransfereCC(String file, String destip) throws SocketException,Exception{
        this.agente = new AgenteUDP(this);
        this.upload = false;
        this.download = true;
        this.fich = null;
        this.filename = file;
        destinationIP = destip;
    }

    ////////////////////////// RECEIVE //////////////////////////
    /*
        Método que passa um array de byte para um objeto.
    */
    public Object deserializePDU(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    /*
        Método que recebe Datagramas provenientes do AgenteUDP e entrega nas
    threads responsáveis por um dado IP.
    */
    public void recebePDU(DatagramPacket dp){
        // Returns the data buffer.
        byte[] data = dp.getData();
        // Returns the raw IP address of this InetAddress object.
        InetAddress ipAddress = dp.getAddress();

        try{
            // Deserialize the datagram's data: bytes -> PDU
            PDU p = (PDU) deserializePDU(data);

            // check the value of the checksum
            long checksum = p.calculateChecksum();
            if(checksum != p.getChecksum()) {System.out.println("ERRO"); return;} // discards the PDU

            // It's a Upload ...
            if(this.upload == true){
                // Get thread associated with an IP address.
                ThreadUpload tup = threads_upload.get(ipAddress);

                // If thread doesn't exist ...
                if(tup == null){

                    // cria um novo tranfereCC
                    ThreadUpload ntup;
                    ntup = new ThreadUpload(agente,this,ipAddress);

                    // inicia thread
                    new Thread(ntup).start();

                    // insere no hashmap
                    l.lock();
                    threads_upload.put(ipAddress,ntup);
                    l.unlock();

                    // ThreadUpload processes the PDU.
                    ntup.recebePDU(p);
                } else{
                    // ThreadUpload processes the PDU.
                    tup.recebePDU(p);
                }
            } // It's a Download ...
            else{
                // ThreadDownload processes the PDU.
                tfd.recebePDU(p);
            }

        } catch(Exception e){
            e.printStackTrace();
        }

    }

    public void removeConnection(InetAddress ip){
        l.lock();
        try{
            threads_upload.remove(ip);
        } finally{
            l.unlock();
        }
    }

    ////////////////////////// RUN //////////////////////////
    public void run(){
        try{
            // inicializa server (AgenteUDP) que recebe packets
            Thread agent = new Thread(agente);
            agent.start();

            // It's a Download ...
            if(this.download == true){
                tfd = new ThreadDownload(agente,destinationIP,filename);
                new Thread(tfd).run();

                // Interrupts this thread.
                agente.closeAgent();
            }

        } catch(UnknownHostException e){
            e.printStackTrace();
        }
    }

    ////////////////////////// Concurrent Acess //////////////////////////
    public synchronized String getPartOfFile(int file_part){
        return segmented_file.get(file_part);
    }

    public synchronized int segmentNumber(){
        return segmented_file.size();
    }

    ////////////////////////// Auxiliar //////////////////////////
    /*
        Divide o ficheiro consoante o MSS e coloca-o num MAP<>
    */
    public Map<Integer,String> divideFile(File file){
        try{
            // Creates a FileInputStream by opening a connection to an actual file, the file named by the File object file in the file system.
            FileInputStream fis = new FileInputStream(file);
            // Creates an InputStreamReader that uses the default charset.
            InputStreamReader isr = new InputStreamReader(fis);
            // Returns the length of the file denoted by this abstract pathname.
            long file_length = file.length();
            char[] file_char = new char[(int)file_length];
            // Reads characters into a portion of an array.
            isr.read(file_char, 0, (int)file_length);

            char[] lidos;
            if(file_length < this.MSS)
                lidos = new char[(int) file_length];
            else lidos = new char[this.MSS];

            lidos[0] = file_char[0];
            int seq = 0;
            Map<Integer,String> file_map = new HashMap<>();
            for(int i = 1; i < file_length ; i++){
                if(i%this.MSS != 0){
                    lidos[i%this.MSS] = file_char[i];
                }else{
                    String data = new String(lidos);
                    file_map.put(seq,data);
                    seq+=this.MSS;

                    if(file_length-i < this.MSS)
                        lidos = new char[(int) file_length-i];
                    else lidos = new char[this.MSS];

                    lidos[0] = file_char[i];
                }
            }
            String data = new String(lidos);
            file_map.put(seq,data);

            return file_map;
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
