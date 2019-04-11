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
        PDU p = new PDU(0, 0, 1024, false, false, false, true,"OLA");
        agente.sendPDU(p,addressDest,7777);
    }
}
