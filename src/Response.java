import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

public class Response {

    private boolean success;
    private int responseCode;
    private String result;
    private String message;

    private JsonElement JSON;

    public Response(boolean success, int responseCode, String result, String message, String urlResponse) {

        this.success = success;
        this.responseCode = responseCode;
        this.result = result;
        this.message = message;
        try {
            this.JSON = url2JSON(urlResponse);
        } catch (IOException e) {
            System.out.println("Response creating error");
        }

    }

    public Response(boolean success, String result, String message, String urlResponse) {

        this.success = success;
        this.result = result;
        this.message = message;
        try {
            this.JSON = url2JSON(urlResponse.toString());
        } catch (IOException e) {
            System.out.println("Response creating error");
        }

    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public JsonElement url2JSON (String result) throws IOException {

        JsonParser parser = new JsonParser();
        return parser.parse(result);
    }

    public JsonElement getJSON() {
        return JSON;
    }

    @Override
    public String toString() {
        return result;
    }

}