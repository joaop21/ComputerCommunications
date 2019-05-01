import java.util.Random;

enum State {
  CONNECTING,
  ESTABLISHED,
  DISCONNECTING,
  CLOSED
}

public class Estado{
    private State estado;
    private int sequence_number;
    private int ack_number;
    private int first_data_ack_number;
    private int mss;
    private int receiveWindow;
    private int congestionWindow;
    private int timeout;

    public Estado(){
        this.estado = State.CONNECTING;
        this.sequence_number = 0;
        this.ack_number = 0;
        this.first_data_ack_number = 0;
        this.mss = 1024;
        this.receiveWindow = 0;
        this.congestionWindow = 0;
        this.timeout = 0;
    }

    public State getEstado(){return this.estado;}

    public int getSequenceNumber(){return this.sequence_number;}

    public int getAckNumber(){return this.ack_number;}

    public int getFirstDataAckNumber(){return this.first_data_ack_number;}

    public int getMSS(){return this.mss;}

    public int getReceiveWindow(){return this.receiveWindow;}

    public int getCongestionWindow(){return this.congestionWindow;}

    public int getTimeout(){return this.timeout;}


    public void setNextState(){
        switch(estado){
            case CONNECTING:
                estado = State.ESTABLISHED;
                break;
            case ESTABLISHED:
                estado = State.DISCONNECTING;
                break;
            case DISCONNECTING:
                estado = State.CLOSED;
                break;
        }
    }

    public void setSequenceNumber(int sn){this.sequence_number = sn;}

    public void incrementSequenceNumber(int isn){this.sequence_number += isn;}

    public void setAckNumber(int an){this.ack_number = an;}

    public void setFirstDataAckNumber(int fdan){this.first_data_ack_number = fdan;}

    public void setReceiveWindow(int rw){this.receiveWindow = rw;}

    public void setCongestionWindow(int cw){this.congestionWindow = cw;}

    /**
        Método que atribui um numero random ao número de sequência.
        TCP funciona da mesma maneira.
    */
    public void setInitialRandomSequenceNumber(){
        Random rand = new Random();
        this.sequence_number = rand.nextInt(50);
    }

}
