import java.net.*;

public class TransfereCCcmd{

    public static void start(){
        try{
            Thread t = new Thread(new TransfereCC());
            t.setDaemon(true);
            t.run();

        } catch(Exception e){
            e.printStackTrace();
        }
    }




    public static void main(String[] args) {
            switch(args[0]){
                case "start":
                        start();
                        break;
                case "stop":
                        // pára o servidor
                        break;
                case "get":
                        // download
                        break;
                case "put":
                        // upload
                        break;
                default:
                    System.out.println("Dados Errados.");
                    break;
            }
    }
}
