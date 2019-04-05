import java.io.*;
import java.net.*;

class AgenteUDP implements Runnable{

    TransfereCC transfCC;
    DatagramSocket serverSocket ;
    byte[] receiveData = new byte[1024];
    byte[] sendData = new byte[1024];

    public AgenteUDP(TransfereCC tfcc) throws Exception{
        transfCC = tfcc;
        serverSocket = new DatagramSocket(7777);
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

                String data = new String( receivePacket.getData());
                InetAddress ipAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();

                transfCC.recebePDU(data,ipAddress,port);

                System.out.println("MSG:" + "\n" + ipAddress + " " + port + "\n");

                sendPDU(data,ipAddress,port);

            }
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            serverSocket.close();
        }
    }
}
