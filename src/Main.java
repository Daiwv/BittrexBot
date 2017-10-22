import java.io.IOException;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        // write your code here

        while (true) {

            try {
                Strategy myStrategy = new Strategy();
                myStrategy.run();

                float seconds = 60;
                float time = (seconds * 1.5f) * 1000;

                try {
                    Thread.sleep((long) time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e){
                System.out.println("CRASH!!!!");
            }
        }
    }
}
