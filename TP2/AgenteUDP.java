import java.io.*;
import java.net.*;

class AgenteUDP implements Runnable{
    TransfereCC transfCC;
    DatagramSocket serverSocket ;

    public AgenteUDP(TransfereCC tfcc) throws Exception{
        transfCC = tfcc;
        // Constructs a datagram socket and binds it to the specified port on the local host machine.
        serverSocket = new DatagramSocket(7777);
    }

    public synchronized void sendPDU(PDU segment,InetAddress IPAddress, int port){
        try{
            // Serialize the PDU: PDU -> bytes
            byte[] sendData = segment.serialize();
            // Constructs a datagram packet for sending packets of length 'length' to the specified port number on the specified host.
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            // Sends a datagram packet from this socket.
            serverSocket.send(sendPacket);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    void closeAgent(){
        serverSocket.close();
    }

    /**
    * Isto funciona como um server que recebe packets
    */
    public void run(){
        try{
            while(true){
                byte[] receiveData = new byte[65527]; // tamanho maximo para dados
                // Constructs a DatagramPacket for receiving packets of length 'length'.
                DatagramPacket receivedPacket = new DatagramPacket(receiveData, receiveData.length);
                // Receives a datagram packet from this socket.
                serverSocket.receive(receivedPacket);
                // transfCC processes the datagram packet.
                transfCC.recebePDU(receivedPacket);
            }
        } catch(SocketException e){
            System.out.println("Closing channel ...");
        } catch(Exception e){
            e.printStackTrace();
        }finally{
            serverSocket.close();
        }
    }
}
