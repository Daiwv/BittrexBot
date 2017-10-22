import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class Strategy {

    private Bittrex m_Bittrex;
    private Map<Double, Trade>  m_SortedMapPairs;
    private Map<String, Double> m_UserBalance;
    private Map<String, String> m_UserOrders;
    private static final double minBuySum = 0.001;                                      //Минимальный размер ордера на покупку монет (в BTC)
    private static final double minBid = 0.000001;                                      //Минимальная цена за покупаемую монету (в BTC)
    private static final int amountCurrenciesToBuy = 5;                                 //Количество первых торгуемых пар монет из списка ранжируемых
    private static final double  fee = 0.0025;                                          //Комиссия биржи за сделку
    private static final double  profit = 0.01;                                         //Желаемый процент чистой прибыли с операции покупка/продажа
    private static final int amountCurrenciesToPlay = 25;                               // Количество валют играемых в целом.
    private static Map<String, Double> m_minTradeSizes = new HashMap<String, Double>(); // Карта всех валют и их minTradeSize
    private static int countCurrencies = 0;                                             // На каждом проходе - количество играемых валют в кошельке на данный момент
    private static double minCurrencyBTCValue = 0.00005;                                //минимальная est.btc. value для валюты, чтобы считать ее у вас в кошельке играемой! а не подаренной системой просто так.

    public JsonElement url2JSON (String url) throws IOException {
        String json = IOUtils.toString(new URL(url));
        JsonParser parser = new JsonParser();
        return parser.parse(json);
    }

    public void run(){
        m_Bittrex = new Bittrex();

        System.out.println("GET MARKEt SUM");
        m_SortedMapPairs = getMarketSums();                         //Список торгуемых монет, отсортированный по rank и задание m_minTradeSizes

        System.out.println("CLOSE OPEN ORDERS");
        closeUserOpenOrders();                                      //Закрытие всех открытых ордеров

        System.out.println("GET BALANCE");
        m_UserBalance = getUserBalance();                           //Запрос текущего баланса

        System.out.println("SELL CURRENCY");
        sellCurrency();                                             //Выставление ордеров на продажу имеющихся монет

        System.out.println("BUY CURRENCY");
        buy();                                                      //Выставление ордеров на покупку новых монет

    }

    public Response getUserOpenOrders(){
        return m_Bittrex.getOpenOrders();
    }

    public void closeUserOpenOrders(){

        Response response = getUserOpenOrders();                    //Запрос открытых ордеров
        JsonObject object = response.getJSON().getAsJsonObject();

        /// DEBUG @!@!!@!@
        JsonArray OpenOrders = object.getAsJsonArray("result");

        for (int i = 0; i < OpenOrders.size(); i++ ){
            JsonObject openOrder = OpenOrders.get(i).getAsJsonObject();
            String openOrderName = openOrder.get("OrderUuid").getAsString();//Получение ID открытых ордеров
            m_Bittrex.cancelOrder(openOrderName);                           //Закрытие ордеров по ID
        }

    }

    public Map <Double, Trade> getMarketSums() {
        JsonElement element = m_Bittrex.getMarketSummaries().getJSON();
        JsonObject object = element.getAsJsonObject();
        Boolean operation_result = object.get("success").getAsBoolean();

        Map <Double, Trade> SortedMap = new TreeMap<Double, Trade>(
                new Comparator<Double>()  {

                    @Override
                    public int compare(Double o1, Double o2) {
                        return o2.compareTo(o1);
                    }

                });

        if (operation_result) {
            JsonArray allPairs = object.getAsJsonArray("result");
            for (int i = 0; i < Math.min(allPairs.size(), 300); i++) {



                JsonObject pair = allPairs.get(i).getAsJsonObject();
                JsonObject market = pair.get("Market").getAsJsonObject();
                JsonObject summary = pair.get("Summary").getAsJsonObject();

                double minTradeSize = market.get("MinTradeSize").getAsDouble();
                String marketName = summary.get("MarketName").getAsString();
                double bid = summary.get("Bid").getAsDouble() + minTradeSize;
                double ask = summary.get("Ask").getAsDouble() - minTradeSize;
                double baseVolume = summary.get("BaseVolume").getAsDouble();

                double rank = ((ask - bid) / bid) * baseVolume;

                Trade trade = new Trade(marketName, bid, ask, baseVolume, minTradeSize);

//            System.out.println( MarketName + " Bid:" + " " + Bid.getAsString() + " Ask: " + Ask.getAsString() + " BaseVolume: " + BaseVolume.getAsString() + " Rank = " + rank);

                if (rank > 0 && bid > minTradeSize && trade.getPair1().contains("BTC"))
                    SortedMap.put(rank, trade);

            }
        } else {
            System.out.println(object.get("message"));
        }

        m_SortedMapPairs = SortedMap;
        return SortedMap;
    }

    public Map<String, Double> getUserBalance(){
        Response response = m_Bittrex.getBalances();
        JsonObject object = response.getJSON().getAsJsonObject();

        Map<String, Double> userBalance = new HashMap<String, Double>();

        JsonArray Currencies = object.getAsJsonArray("result");

        countCurrencies = 0;

        for (int i = 0; i < Currencies.size(); i++ ){
            JsonObject Currency = Currencies.get(i).getAsJsonObject();
            JsonObject Balance = Currency.get("Balance").getAsJsonObject();

            String CurrencyName = Balance.get("Currency").getAsString();
            Double Available = Balance.get("Available").getAsDouble();
            if (Available > 0 && Available > minCurrencyBTCValue) {
                userBalance.put(CurrencyName, Available);
                countCurrencies++;
            }


        }

        return userBalance;
    }

    public void sellCurrency(){

        Response response = m_Bittrex.getOrderHistory();
        JsonObject object = response.getJSON().getAsJsonObject();
        JsonArray history = object.getAsJsonArray("result");

        Map <String, Double> lastPrices = new HashMap<>();

        for (int i = 0; i < history.size(); i++ ){
            JsonObject order = history.get(i).getAsJsonObject();

            String orderName = order.get("Exchange").getAsString();
            Double orderPricePerUnit = order.get("PricePerUnit").getAsDouble();
//            Double orderSum = order.get("Price").getAsDouble() + order.get("Commission").getAsDouble();
            String OrderType = order.get("OrderType").getAsString();
            if (!lastPrices.containsKey(orderName) && (OrderType.contains("BUY"))){
                lastPrices.put(orderName, orderPricePerUnit);
            }
        }

        for (Map.Entry<String, Double> entry : m_UserBalance.entrySet())
        {
            if (!entry.getKey().contains("BTC") && entry.getValue() > 0) {
                System.out.println();
                System.out.println("Try to sell : " +  entry.getKey());
                double price = isGoodSell(entry.getKey(), lastPrices);
                if (price > 0) {
                    sellCurrency(entry.getKey(), price);
                }
                System.out.println();
            }
        }
    }

    public void sellCurrency(String Currency, double price){
        String url = "https://bittrex.com/api/v1.1/market/selllimit?apikey="
                + m_Bittrex.getApikey()
                + "&market="
                + "BTC-" + Currency
                + "&quantity="
                + m_UserBalance.get(Currency)
                + "&rate="
                + String.valueOf(price)
                + "&nonce="
                + EncryptionUtility.generateNonce();

        Response answer = url2Response(url);

        if (!answer.isSuccessful()){
            System.out.print(Currency + " " + answer.getMessage());
        } else {
            System.out.print(Currency + " has been order to sell with price:" + price);
        }
    }

    public double getCurAsk(String Currency){
        Currency = "BTC-" + Currency;
        Response response = m_Bittrex.getMarketSummary(Currency);
        JsonObject object = response.getJSON().getAsJsonObject();
        Boolean operation_result = object.get("success").getAsBoolean();
        double priceCurrentMinAsk = 0;
        if (operation_result) {
            JsonObject market = object.getAsJsonObject("result");
            priceCurrentMinAsk = market.get("Ask").getAsDouble();
        }
        return priceCurrentMinAsk;
    }

    public double getCurBid(String Currency){
        Currency = "BTC-" + Currency;
        Response response = m_Bittrex.getMarketSummary(Currency);
        JsonObject object = response.getJSON().getAsJsonObject();
        Boolean operation_result = object.get("success").getAsBoolean();
        double priceCurrentMaxBid = 0;
        if (operation_result) {
            JsonObject market = object.getAsJsonObject("result");
            priceCurrentMaxBid = market.get("Bid").getAsDouble();
        }
        return priceCurrentMaxBid;
    }


    public double getTrueAsk(String Currency){

        double disstancePercent = 0.20;
        try {

            double CurVal1 = getCurAsk(Currency);
            double CurVal2 = getCurAsk(Currency);
            double CurVal3 = getCurAsk(Currency);

            double Bid = getCurBid(Currency);

            double delta1 = Math.abs(Bid - CurVal1);
            double delta2 = Math.abs(Bid - CurVal2);
            double delta3 = Math.abs(Bid - CurVal3);

            if (delta1 <= delta2 && delta1 <= delta3)
                if (Math.abs((Bid - CurVal1)/CurVal1) < disstancePercent){
                    return askBid(CurVal1, Bid);
                }
            if (delta2 <= delta1 && delta2 <= delta3)
                if (Math.abs((Bid - CurVal2)/CurVal2) < disstancePercent)
                    return askBid(CurVal2, Bid);
            if (delta3 <= delta2 && delta3 <= delta1)
                if (Math.abs((Bid - CurVal3)/CurVal3) < disstancePercent)
                    return askBid(CurVal3, Bid);

        } catch (Exception e){
            return -1;
        }

        return -1;
    }

    public double askBid(double ask, double bid){
        System.out.println("Ask: " + ask + " Bid: " + bid);
        return ask;
    }

    public double isGoodSell(String Currency, Map <String, Double> lastPrices){
        double priceUserBuy = 1; /// текущий бид!
        try {
            priceUserBuy = lastPrices.get("BTC-" + Currency);
        } catch (Exception e){
            System.out.println("Can't find last price!");
        }
            double priceCurrentMinAsk;

                double minTradeSize = 0.00000001;

                try {
                    minTradeSize = m_minTradeSizes.get("BTC-" + Currency);
                }catch (Exception e){
                    minTradeSize = 0.00000002;
                }

                double ask = getTrueAsk(Currency);
                if (ask == -1) {
                    System.out.println("Error get Ask!");
                    return - 1;
                }
                priceCurrentMinAsk = ask - minTradeSize;

                double extra = fee * 2 + profit;

                System.out.print(" Ask = " + ask + "; minTradeSize = " + minTradeSize + "; with price estimated: " + priceCurrentMinAsk + "; bought: " + (priceUserBuy + priceUserBuy * extra));
                System.out.print("; profit = " + Math.round((((priceCurrentMinAsk  / (priceUserBuy + priceUserBuy * extra)) -1) * 100.0f)) + "% -> " + (priceCurrentMinAsk > (priceUserBuy + priceUserBuy * extra)) + " ")  ;

                if (priceCurrentMinAsk > priceUserBuy*(1 + extra) && Math.round((((priceCurrentMinAsk  / (priceUserBuy + priceUserBuy * extra)) -1) * 100.0f)) < 25){
                    return priceCurrentMinAsk;
                }
                else
                    return -1;
    }

    public void buy(){
        Response responce = m_Bittrex.getBalance("BTC");
        JsonObject object = responce.getJSON().getAsJsonObject();

        Boolean operation_result = object.get("success").getAsBoolean();

        if (operation_result) {

            JsonObject Currency = object.getAsJsonObject("result");

            String CurrencyName = Currency.get("Currency").getAsString();
            Double Available = Currency.get("Available").getAsDouble();

            int i = 0;

            if (m_UserBalance.size() > amountCurrenciesToPlay) return;

            for (Map.Entry<Double, Trade> entry : m_SortedMapPairs.entrySet()) {

                if (m_UserBalance.containsKey(entry.getValue().getPair2())) // если ее количества еще не достаточно для продажи, докупаю.!!! доделать
                    continue;

                System.out.println(String.valueOf(i) + " " + entry.getValue().getName() + " Bid:" + " " + entry.getValue().getBid() + " Ask: " + entry.getValue().getAsk() + " Rank = " + entry.getKey());

                Response response1 = m_Bittrex.getMarketSummary(entry.getValue().getName());
                JsonObject market = response1.getJSON().getAsJsonObject();

                Boolean operation_result1 = market.get("success").getAsBoolean();

                if (operation_result1) {

                    JsonObject MarketRes = market.getAsJsonObject("result");
                    double CurencyBid =  MarketRes.get("Bid").getAsDouble(); //entry.getValue().getBid();
                    double CurencyAsk =  MarketRes.get("Ask").getAsDouble(); //entry.getValue().getAsk();

                    double countCurrencyToBuy = minBuySum / CurencyBid;

                    if (minBuySum > Available) {
                        System.out.println("Not Enough BTC to buy something new");
                        return;
                    }

                    i++;

                    if (minBuySum < Available && CurencyBid > minBid) {

                        String url = "https://bittrex.com/api/v1.1/market/buylimit?apikey="
                                + m_Bittrex.getApikey()
                                + "&market="
                                + entry.getValue().getName()
                                + "&quantity="
                                + String.valueOf(countCurrencyToBuy)
                                + "&rate="
                                + String.valueOf(CurencyBid)
                                + "&nonce="
                                + EncryptionUtility.generateNonce();

                        Response answer = url2Response(url);

                        if (answer.isSuccessful()) {
                            Available -= minBuySum;
                        }

                        if (i > amountCurrenciesToBuy) return;
                    }
                }
            }
        }
    }

    public Response url2Response(String url){

        Response response = new Response(false, "", "", "");

        try {

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);

            request.addHeader("apisign", EncryptionUtility.calculateHash(m_Bittrex.getSecret(), url, "HmacSHA512")); // Attaches signature as a header

            HttpResponse httpResponse = client.execute(request);

            int responseCode = httpResponse.getStatusLine().getStatusCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

            StringBuffer resultBuffer = new StringBuffer();

            String line = "";

            while ((line = reader.readLine()) != null)

                resultBuffer.append(line);

            response = m_Bittrex.createResposeFromUrlResponse(resultBuffer.toString());

            response.setResponseCode(responseCode);

            return response;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
}
