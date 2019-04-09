import java.io.*;
import java.net.*;

class AgenteUDP implements Runnable{
    TransfereCC transfCC;
    DatagramSocket serverSocket ;
    byte[] receiveData = new byte[65527]; // tamanho maximo para dados
    byte[] sendData = new byte[65527];

    public AgenteUDP(TransfereCC tfcc) throws Exception{
        transfCC = tfcc;
        serverSocket = new DatagramSocket(7777);
    }

    public Object deserializePDU(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public void sendPDU(String sentence,InetAddress IPAddress, int port){
        try{
            sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
    * Isto funciona como um server que recebe packets
    */
    public void run(){
        try{
            while(true){
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                byte[] data = receivePacket.getData();
                InetAddress ipAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();

                PDU p = (PDU) deserializePDU(data);

                transfCC.recebePDU(p);

                System.out.println("MSG:" + "\n" + ipAddress + " " + port + "\n" + p.getData() + "\n");

            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            serverSocket.close();
        }
    }
}
