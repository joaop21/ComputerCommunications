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
    State estado_atual;

    public Estado(){
        estado_atual = State.LISTEN;
    }

    State getNextState(){

        switch(estado_atual){
            case LISTEN:
                    estado_atual = State.SYN_SENT;
                    break;
            case SYN_SENT:
                    estado_atual = State.SYN_RECEIVED;
                    break;
            case SYN_RECEIVED:
                    estado_atual = State.ESTABLISHED;
                    break;
            case ESTABLISHED:
                    estado_atual = State.FIN_WAIT_1;
                    break;
            case FIN_WAIT_1:
                    estado_atual = State.FIN_WAIT_2;
                    break;
            case FIN_WAIT_2:
                    estado_atual = State.CLOSE_WAIT;
                    break;
            case CLOSE_WAIT:
                    estado_atual = State.CLOSING;
                    break;
            case CLOSING:
                    estado_atual = State.LAST_ACK;
                    break;
            case LAST_ACK:
                    estado_atual = State.TIME_WAIT;
                    break;
            case TIME_WAIT:
                    estado_atual = State.CLOSED;
                    break;
            case CLOSED:
                    estado_atual = State.CLOSED;
                    break;
        }

        return estado_atual;
    }

}
