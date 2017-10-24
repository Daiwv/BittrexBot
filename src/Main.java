import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        // write your code here
        TelegramBot teleBot = null;
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            teleBot = new TelegramBot();
            telegramBotsApi.registerBot(new TelegramBot());
            System.out.println("Telegram Bot Started!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.out.println("Error Register Telegram Bot");
        }

        while (true) {

            try {

                teleBot.m_status = "Is Running!";
                Strategy myStrategy = new Strategy();
                myStrategy.run();

                float seconds = 60;
                float time = (seconds * 1.5f) * 1000;

                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                String curDate = dateFormat.format(date);
                System.out.println("last run time: " + curDate); //2016/11/16 12:08:43

                teleBot.m_LastUserBalance = myStrategy.getLastUserBalance();
                teleBot.m_LastUserSells = myStrategy.getLastUserSells();
                teleBot.m_LastUserBuye = myStrategy.getLastUserBuy();
                teleBot.m_lastStartTime = curDate;
                teleBot.m_status = "Run has been completed! Wait time";
                teleBot.m_tradeHistory = myStrategy.getTradeHistory();

                try {
                    Thread.sleep((long) time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } catch (Exception e){
                teleBot.m_status = "CRASH!!!!";
//                teleBot.sentAllert("CRASH!!!!" +  e.getStackTrace());
                System.out.println("CRASH!!!!");
            }
        }
    }
}
