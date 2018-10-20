package osrs.flipping.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import osrs.flipping.tool.exception.ApiUrlConnectionException;
import osrs.flipping.tool.exception.ApiUrlCreationException;

public class FlipTool {
    private static final String API_HOST_URL = "https://rsbuddy.com/exchange/summary.json";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";

    private String proxyAddress;
    private int proxyPort;
    private boolean useProxy;
    
    public FlipTool(){
        useProxy = false;
    }
    
    public FlipTool(String proxyAddress, int proxyPort){
        this.proxyAddress = proxyAddress;
        this.proxyPort = proxyPort;
        useProxy = true;
    }

    private JSONObject getItemList(String urlString, boolean useProxy) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new ApiUrlCreationException("Unable instantiate URL out of provided URL string: " + urlString, e);
        }

        HttpURLConnection connection = null;
        String line;
        StringBuffer responseBuffer = new StringBuffer();
        BufferedReader in;
        try {
            connection = useProxy ? openProxyConnection(url)
                    : (HttpURLConnection) url.openConnection();

            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();

            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            while ((line = in.readLine()) != null) {
                responseBuffer.append(line);
            }

        } catch (IOException e) {
            throw new ApiUrlConnectionException("Unable to connect to provided URL: " + url, e);
        } finally {
            closeConnection(connection);
        }

        return new JSONObject(responseBuffer.toString());
    }

    private void closeConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    private HttpURLConnection openProxyConnection(URL url) throws IOException {
        return (HttpURLConnection)url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyAddress, proxyPort)));
    }

    public List<GEItem> generateFlipList(int maxItemPrice, int minPercentMargin) throws Exception{

        List<GEItem> result = new ArrayList<>();
        JSONObject itemSummary = getItemList(API_HOST_URL, useProxy);
        
        for(String itemId : itemSummary.keySet()){
            JSONObject item = itemSummary.getJSONObject(itemId);
          
            int sellAverage = item.getInt("sell_average");
            int buyAverage = item.getInt("buy_average");
            int diff = Math.abs(sellAverage - buyAverage);
            double profitPercent;
            
            if(buyAverage <= maxItemPrice){
                
                profitPercent = (buyAverage > 0) ? (diff/buyAverage) * 100 : -1;
                
                if(profitPercent >= minPercentMargin){
                    GEItem geitem = new GEItem(item.getString("name"),
                                               item.getInt("id"),
                                               buyAverage,
                                               sellAverage,
                                               item.getInt("sp"),
                                               item.getBoolean("members"));
                    
                    result.add(geitem);
                
                }
            }
        }
        
        return result;
    }
    
}
