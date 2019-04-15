import java.io.*;

public class PDU implements Serializable{
    private int sequence_number;
    private int ack_number;
    private int mss;
    private String options;
    private boolean syn;
    private boolean fin;
    private boolean ack;
    private boolean psh;
    private byte[] data;

    public PDU(int seq, int ack_n, int mss, String options, Boolean syn, Boolean fin, Boolean ack,Boolean psh, byte[] dt){
        this.sequence_number = seq;
        this.ack_number = ack_n;
        this.mss = mss;
        this.options = options;
        this.syn = syn;
        this.fin = fin;
        this.ack = ack;
        this.psh = psh;
        this.data = dt;
    }

    public int getSequenceNumber(){ return this.sequence_number; }

    public int getAckNumber(){ return this.ack_number; }

    public int getMSS(){ return this.mss; }

    public String getOptions(){ return this.options; }

    public Boolean getSYN(){ return this.syn; }

    public Boolean getFIN(){ return this.fin; }

    public Boolean getACK(){ return this.ack; }

    public Boolean getPSH(){ return this.psh; }

    public byte[] getData(){ return this.data; }

    /*
    Java provides a mechanism, called object serialization where an object can be represented as
    a sequence of bytes that includes the object's data as well as information about the object's
    type and the types of data stored in the object.

    After a serialized object has been written into a file, it can be read from the file and
    deserialized that is, the type information and bytes that represent the object and its data
    can be used to recreate the object in memory.
    */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(this);
        return out.toByteArray();
    }

}
