import java.net.*;

class TransfereCCDownload extends Thread{
    AgenteUDP agente;
    private InetAddress addressDest;

    public TransfereCCDownload(AgenteUDP agent, String destip) throws UnknownHostException{
        agente = agent;
        this.addressDest = InetAddress.getByName(destip);
    }

    public void run(){
        PDU p = new PDU(0, 0, 1024, false, false, false, true,"OLA");
        agente.sendPDU(p,addressDest,7777);
    }
}
