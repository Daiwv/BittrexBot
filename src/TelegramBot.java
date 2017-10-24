import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

public class TelegramBot extends TelegramLongPollingBot {

    private Strategy m_strategy;

    public static String m_LastUserBalance = "";
    public static String m_LastUserSells = "";
    public static String m_LastUserBuye = "";
    public static String m_lastStartTime = "";
    public static String m_status = "not work";
    private static String chatID = "";
    private static Integer messageID = 0;
    public static String m_tradeHistory = "";


    @Override
    public String getBotUsername() {
        return "ipriisk_bot";
    }

    @Override
    public String getBotToken() {
        return "325450031:AAESOrh2C-HZ9wBI-JTj-Vyo_l38ciRg25U";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        String s = message.getChatId().toString();
        chatID = s;
        Integer s1 = message.getMessageId();
        messageID = s1;

        if (message != null && message.hasText()) {
            if (message.getText().equals("/help"))
                sendMsg(message, "Привет, я робот:\n" +
                        "Мои команды:\n" +
                        "/balance - баланс пользователя\n" +
                        "/sells - продажи на последней итерации \n" +
                        "/buys - покупки на последней итерации \n" +
                        "/time - последнее время запуска \n" +
                        "/status - состояние бота \n" +
                        "/history - история последних сделок \n");
            else if (message.getText().equals("/balance")){
                sendMsg(message, m_LastUserBalance);
            } else if (message.getText().equals("/sells")){
                sendMsg(message, m_LastUserSells);
            } else if (message.getText().equals("/buys")){
                sendMsg(message, m_LastUserBuye);
            } else if (message.getText().equals("/time")) {
                sendMsg(message, m_lastStartTime);
            } else if (message.getText().equals("/status")) {
                sendMsg(message, m_status);
            }else if (message.getText().equals("/history")) {
                sendMsg(message, m_tradeHistory);
            }
            else
                sendMsg(message, "Я не знаю что ответить на это, введи /help");
        }
    }

    private void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        String s = message.getChatId().toString();
        chatID = s;
        sendMessage.setChatId(s);
        Integer s1 = message.getMessageId();
        messageID = s1;
        sendMessage.setReplyToMessageId(s1);
        sendMessage.setText(text);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void setStrategy(Strategy strategy){
        m_strategy = strategy;
    }

    public void setLastStartTime( String s ){
        m_lastStartTime = s;
    }

    public void sentAllert(  String s ){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatID);
        sendMessage.setReplyToMessageId(messageID);
        sendMessage.setText(s);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}