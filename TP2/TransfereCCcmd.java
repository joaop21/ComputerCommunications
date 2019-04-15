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
            // Start a Download (file, destination IP).
            new Thread(new TransfereCC(ficheiro,address)).run();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void put(String ficheiro){
        try{
            // Creates a new File instance by converting the given pathname string into an abstract pathname.
            File f = new File(ficheiro);
            if(f.exists() && !f.isDirectory())
                // Start an Upload (file).
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
