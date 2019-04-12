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

    public synchronized void sendPDU(PDU segment,InetAddress IPAddress, int port){
        try{
            sendData = segment.serialize();
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

                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivedPacket);

                transfCC.recebePDU(receivedPacket);

            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            serverSocket.close();
        }
    }
}
