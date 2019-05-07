import java.util.Random;

public class Estado{
    private TransferState estado;
    private int sequence_number;
    private int first_data_seq_number;
    private int ack_number;
    private int first_data_ack_number;
    private int mss;
    private int receiveWindow;
    private CongestionControlPhase cc_phase;
    private int dupAckCount;
    private int ssthresh;
    private float congestionWindow;
    private long timeout; // milissegundos
    private long estimatedRTT;
    private long devRTT;
    private int timeout_count;

    public Estado(){
        this.estado = TransferState.CONNECTING;
        this.sequence_number = 0;
        this.first_data_seq_number = 0;
        this.ack_number = 0;
        this.first_data_ack_number = 0;
        this.mss = 1024;
        this.receiveWindow = 1;
        this.cc_phase = CongestionControlPhase.SlowStart;
        this.dupAckCount = 0;
        this.ssthresh = 64;
        this.congestionWindow = 1;
        this.timeout = 3000;
        this.estimatedRTT = 3000;
        this.devRTT = 5;
        this.timeout_count = 0;
    }

    public TransferState getEstado(){return this.estado;}

    public int getSequenceNumber(){return this.sequence_number;}

    public int getFirstDataSequenceNumber(){return this.first_data_seq_number;}

    public int getAckNumber(){return this.ack_number;}

    public int getFirstDataAckNumber(){return this.first_data_ack_number;}

    public int getMSS(){return this.mss;}

    public int getReceiveWindow(){return this.receiveWindow;}

    public int getCongestionWindow(){return (int)this.congestionWindow;}

    public long getTimeout(){return this.timeout;}


    public void setNextState(){
        switch(estado){
            case CONNECTING:
                estado = TransferState.ESTABLISHED;
                break;
            case ESTABLISHED:
                estado = TransferState.DISCONNECTING;
                break;
            case DISCONNECTING:
                estado = TransferState.CLOSED;
                break;
        }
    }

    public void setSequenceNumber(int sn){this.sequence_number = sn;}

    public void setFirstDataSequenceNumber(int fdsn){this.first_data_seq_number = fdsn;}

    public void incrementSequenceNumber(int isn){this.sequence_number += isn;}

    public void setAckNumber(int an){this.ack_number = an;}

    public void setFirstDataAckNumber(int fdan){this.first_data_ack_number = fdan;}

    public void incrementAckNumber(int an){this.ack_number += an;}

    public void setReceiveWindow(int rw){this.receiveWindow = rw;}

    public void setCongestionWindow(int cw){this.congestionWindow = cw;}

    public void receiveEstimatedRTT(long sampleRTT){
        this.estimatedRTT = (long) ((1-0.125)*(double)this.estimatedRTT + (0.125*(double)sampleRTT));
        this.devRTT = (long) ((1-0.25)*this.devRTT + 0.25*Math.abs(sampleRTT - this.estimatedRTT));
        this.timeout = this.estimatedRTT + 4*this.devRTT;
    }

    /**
        Método que atribui um numero random ao número de sequência.
        TCP funciona da mesma maneira.
    */
    public void setInitialRandomSequenceNumber(){
        Random rand = new Random();
        this.sequence_number = rand.nextInt(1000);
    }

    /**
        Método que retorna a menor janela, considerando a janela de congestão e
    a janela de fluxo
    */
    public int getProperWindow(){
        return Math.min((int)this.congestionWindow, this.receiveWindow);
    }


    /**
        Método que controla a congestão quando é recebido um novo Ack
    */
    public void newAckReceived(){
        this.timeout_count = 0;
        switch(this.cc_phase){
            case SlowStart:
                this.congestionWindow++;
                if(this.congestionWindow >= this.ssthresh) this.cc_phase = CongestionControlPhase.CongestionAvoidance;
                this.dupAckCount = 0;
                break;
            case CongestionAvoidance:
                this.congestionWindow += 1/(Math.floor(this.congestionWindow));
                this.dupAckCount = 0;
                break;
            case FastRecovery:
                this.congestionWindow = this.ssthresh;
                this.dupAckCount = 0;
                this.cc_phase = CongestionControlPhase.CongestionAvoidance;
                break;
        }
    }

    /**
        Método que controla a congestão quando é recebido um Ack Duplicado
    */
    public void duplicatedAckReceived(){
        this.timeout_count = 0;
        switch(this.cc_phase){
            case SlowStart:
                this.dupAckCount++;
                if(this.dupAckCount == 3){
                    this.ssthresh = ((int)this.congestionWindow)/2;
                    this.congestionWindow = this.ssthresh + 3;
                    this.cc_phase = CongestionControlPhase.FastRecovery;
                }
                break;
            case CongestionAvoidance:
                this.dupAckCount++;
                if(this.dupAckCount == 3){
                    this.ssthresh = ((int)this.congestionWindow)/2;
                    this.congestionWindow = this.ssthresh + 3;
                    this.cc_phase = CongestionControlPhase.FastRecovery;
                }
                break;
            case FastRecovery:
                this.congestionWindow++;
                break;
        }
    }

    /**
        Método que controla a congestão quando ocorre um timeout
    */
    public int timeoutReceived(){
        this.timeout_count++;
        this.ssthresh = ((int)this.congestionWindow)/2;
        this.congestionWindow = 1;
        this.dupAckCount = 0;
        this.cc_phase = CongestionControlPhase.SlowStart;
        return timeout_count;
    }

}

enum CongestionControlPhase{
    SlowStart,
    CongestionAvoidance,
    FastRecovery
}
