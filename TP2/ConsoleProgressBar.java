import java.util.concurrent.locks.*;
import java.text.DecimalFormat;

public class ConsoleProgressBar implements Runnable{
    Lock l = new ReentrantLock();
    Condition wait = l.newCondition();
    int progress = 0;
    int max_segments;

    public ConsoleProgressBar(int max){
        max_segments = max;
    }

    public void incrementProgress(){
        l.lock();
        try{
            progress++;
            wait.signal();
        } finally{
            l.unlock();
        }
    }

    public void run(){
        char[] animationChars = new char[]{'|', '/', '-', '\\'};

        while(progress <= max_segments){
            l.lock();
            try{
                wait.await();
		        float percentage = ((float)progress/(float)max_segments)*100;
		        DecimalFormat df = new DecimalFormat();
		        df.setMaximumFractionDigits(2);
                System.out.print("Downloading: " + df.format(percentage) + "% " + animationChars[progress % 4] + "\r");
		        if(progress == max_segments) break;
	        } catch(Exception e){
                // doing nothing
            } finally{
                l.unlock();
            }

        }

        System.out.println("Downloading: Done!          ");
    }
}
