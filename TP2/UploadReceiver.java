class UploadReceiver implements Runnable{
    TransfereCC tfcc;
    ThreadUpload tup;
    AgenteUDP agente;
    Estado estado;

    public UploadReceiver(TransfereCC tfccn ,ThreadUpload tupn, Estado estadon, AgenteUDP agenten){
        tfcc = tfccn;
        tup = tupn;
        estado = estadon;
        agente = agenten;
    }

    public void run(){

        while(true){
            PDU p = tup.nextPDU();
            long now = System.currentTimeMillis();

            if(p != null){
                if(p.getACK() == true){
                    int index = (p.getAckNumber() - estado.getSequenceNumber() - 1)/1024;

                    // in case of receiving the confirmation of the last segment
                    if(index >= tup.validados.length){
                        for(index -= 1; index >= 0 ; index--)
                            tup.validados[index] = 5; // confirmed

                        tup.signalFinalAck();
                        estado.newAckReceived();
                        estado.setNextState();
                        return;
                    }

                    // checks if an ack has already been received
                    if(tup.validados[index] == 1){
                        System.out.println("PDU from Host: " + tup.addressDest + " FLAG: " + p.pdu() +
                                           " Seq Number: " + p.getSequenceNumber() + " Ack Number: " + p.getAckNumber());

                        estado.duplicatedAckReceived();

                        // retransmits PDU
                        PDU retransmit = tfcc.getPDU(index+1);
                        retransmit.incrementSequenceNumber(estado.getSequenceNumber());
                        retransmit.setAckNumber(p.getSequenceNumber()+1);
                        agente.sendPDU(retransmit,tup.addressDest,7777);

                    } else{
                        tup.validados[index] = 1; // one ack received

                        // validate the smaller PDUs
                        for(index -= 1; index >= 0 ; index--){
                            if(tup.validados[index] < 5){
                                if(tup.window < estado.getProperWindow()){
                                    tup.window++;
                                    estado.newAckReceived();
                                }
                            } else{
                                if(tup.validados[index] == 5) break;
                            }

                            tup.validados[index] = 5;
                        }

                        if(tup.window < estado.getProperWindow()) tup.window++;
                        estado.newAckReceived();
                    }
                }
            } else{
                int timeout_count = estado.timeoutReceived();
                if(timeout_count == 3)
                    tfcc.removeConnection(tup.addressDest);
            }
        }
    }
}
