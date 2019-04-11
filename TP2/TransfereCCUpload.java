import java.net.*;

class TransfereCCUpload extends Thread{
    AgenteUDP agente;
    private InetAddress addressDest;

    public TransfereCCUpload(AgenteUDP agent, InetAddress destip) throws UnknownHostException{
        agente = agent;
        this.addressDest = destip;
    }

    public void recebePDU(PDU p){}

    public void run(){
        System.out.println("PDU from Host: " + this.addressDest);
    }
}
