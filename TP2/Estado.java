import java.util.Random;

enum State {
  LISTEN,
  SYN_SENT,
  SYN_RECEIVED,
  ESTABLISHED,
  FIN_WAIT_1,
  FIN_WAIT_2,
  CLOSE_WAIT,
  CLOSING,
  LAST_ACK,
  TIME_WAIT,
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
        this.estado = State.LISTEN;
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

    public State getNextState(){

        switch(estado){
            case LISTEN:
                    estado = State.SYN_SENT;
                    break;
            case SYN_SENT:
                    estado = State.SYN_RECEIVED;
                    break;
            case SYN_RECEIVED:
                    estado = State.ESTABLISHED;
                    break;
            case ESTABLISHED:
                    estado = State.FIN_WAIT_1;
                    break;
            case FIN_WAIT_1:
                    estado = State.FIN_WAIT_2;
                    break;
            case FIN_WAIT_2:
                    estado = State.CLOSE_WAIT;
                    break;
            case CLOSE_WAIT:
                    estado = State.CLOSING;
                    break;
            case CLOSING:
                    estado = State.LAST_ACK;
                    break;
            case LAST_ACK:
                    estado = State.TIME_WAIT;
                    break;
            case TIME_WAIT:
                    estado = State.CLOSED;
                    break;
            case CLOSED:
                    estado = State.CLOSED;
                    break;
        }

        return estado;
    }

}
