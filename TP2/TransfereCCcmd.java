import java.net.*;
import java.io.*;

public class TransfereCCcmd{

    public static void help(){
        System.out.println(
            "*****************************************************************" + "\n" +
            "*                                                               *" + "\n" +
            "*  -help : Displays the command information.                    *" + "\n" +
            "*  -put [file] : Makes the file available for download.         *" + "\n" +
            "*  -get [file] [address] : Downloads the file from an address.  *" + "\n" +
            "*                                                               *" + "\n" +
            "*****************************************************************"
        );
    }

    public static void get(String ficheiro, String address){
        try{
            File f = new File(ficheiro);
            new Thread(new TransfereCC(f)).run();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void put(String ficheiro){
        try{
            File f = new File(ficheiro);
            if(f.exists() && !f.isDirectory())
                new Thread(new TransfereCC(f)).run();
            else
                System.out.println("File doesn't exist.");

        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
            switch(args[0]){
                case "-help": // modus operandi
                        help();
                        break;
                case "-get": // download
                        get(args[1], args[2]);
                        break;
                case "-put": // upload
                        put(args[1]);
                        break;
                default:
                    System.out.println("Unknown Operation.");
                    break;
            }
    }
}
