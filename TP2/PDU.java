import java.io.*;

public class PDU implements Serializable{
    private int sourcePort;
    private int destinationPort;
    private String sourceIP;
    private String destinationIP;
    private String type; // SYN,FYN,ACK,DATA...
    private String data;

    public PDU(int sp, int dp, String sip, String dip, String tp, String dt){
        this.sourcePort = sp;
        this.destinationPort = dp;
        this.sourceIP = sip;
        this.destinationIP = dip;
        this.type = tp;
        this.data = dt;
    }

    public int getSourcePort(){ return this.sourcePort; }

    public int getDestinationPort(){ return this.destinationPort; }

    public String getSourceIP(){ return this.sourceIP; }

    public String getDestinationIP(){ return this.destinationIP; }

    public String getType(){ return this.type; }

    public String getData(){ return this.data; }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(this);
        return out.toByteArray();
    }

}
