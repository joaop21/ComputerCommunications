import java.io.*;

public class PDU implements Serializable{
    private int sequence_number;
    private int ack_number;
    private String options;
    private boolean syn;
    private boolean fin;
    private boolean ack;
    private boolean psh;
    private int receiveWindow;
    private long checksum;
    private byte[] data;

    public PDU(int seq, int ack_n, String options, Boolean syn, Boolean fin, Boolean ack,Boolean psh, byte[] dt){
        this.sequence_number = seq;
        this.ack_number = ack_n;
        this.options = options;
        this.syn = syn;
        this.fin = fin;
        this.ack = ack;
        this.psh = psh;
        this.checksum = 0;
        this.data = dt;
        // Window máxima = 66560 / 1024 = 65 - 10(precaução) = 55
        // 66560 é o valor do buffer de chegada no AgenteUDP
        this.receiveWindow = 55;
    }

    public PDU(PDU np){
        this.sequence_number = np.getSequenceNumber();
        this.ack_number = np.getAckNumber();
        this.options = np.getOptions();
        this.syn = np.getSYN();
        this.fin = np.getFIN();
        this.ack = np.getACK();
        this.psh = np.getPSH();
        this.checksum = np.getChecksum();
        this.data = np.getData();
        this.receiveWindow = np.getReceiveWindow();
    }

    public int getSequenceNumber(){ return this.sequence_number; }

    public int getAckNumber(){ return this.ack_number; }

    public String getOptions(){ return this.options; }

    public Boolean getSYN(){ return this.syn; }

    public Boolean getFIN(){ return this.fin; }

    public Boolean getACK(){ return this.ack; }

    public Boolean getPSH(){ return this.psh; }

    public int getReceiveWindow(){ return this.receiveWindow; }

    public long getChecksum(){ return this.checksum; }

    public byte[] getData(){ return this.data; }

    public void incrementSequenceNumber(int increment){this.sequence_number += increment;}

    public void setAckNumber(int nack){this.ack_number = nack;}

    public void setChecksum(long check){ this.checksum = check; }


    public byte[] serialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(this);
        return out.toByteArray();
    }

    public long calculateChecksum() {
        byte[] buf = this.data;
        int length = buf.length;
        int i = 0;
        long sum = 0;
        long data;

        // Handle all pairs
        while (length > 1) {
            data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
            sum += data;

            // 1's complement carry bit correction in 16-bits (detecting sign extension)
            if ((sum & 0xFFFF0000) > 0) {
                sum = sum & 0xFFFF;
                sum += 1;
            }
            i += 2;
            length -= 2;
        }

        // Handle remaining byte in odd length buffers
        if (length > 0) {
          // Corrected to include @Andy's edits and various comments on Stack Overflow
          sum += (buf[i] << 8 & 0xFF00);
          // 1's complement carry bit correction in 16-bits (detecting sign extension)
          if ((sum & 0xFFFF0000) > 0) {
            sum = sum & 0xFFFF;
            sum += 1;
          }
        }

        // Final 1's complement value correction to 16-bits
        sum = ~sum;
        sum = sum & 0xFFFF;
        return sum;

    }

    public String pdu(){

        if(syn==true && ack==false) return "SYN";
        if(syn==true && ack==true) return "SYNACK";
        if(fin==true && ack==false) return "FIN";
        if(fin==true && ack==true) return "FINACK";
        if((syn==false && ack==true) || (fin==false && ack==true)) return "ACK";
        return "DATA";
    }

    public PDU clone(){
        return new PDU(this);
    }

}
