import java.util.concurrent.locks.*;

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
                System.out.print("Processing: " + (max_segments%progress) + "% " + animationChars[progress % 4] + "\r");
            } catch(Exception e){
                e.printStackTrace();
            } finally{
                l.unlock();
            }

        }

        System.out.println("Processing: Done!          ");
    }
}
