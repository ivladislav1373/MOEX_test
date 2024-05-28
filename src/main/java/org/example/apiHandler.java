package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.*;
import org.json.JSONObject;

import java.io.*;
import java.util.*;


public class apiHandler {
    public static void main(String[] args) throws IOException {
        System.out.println(stockExist("YNX"));
        System.out.println(stockExist("YNDX"));
        System.out.println(stockExist("YNdX"));
        System.out.println(stockExist("YNDKS"));
    }

    public static boolean stockExist(String stock) {
        try (
                CloseableHttpClient httpClient = HttpClientBuilder.create().build()
        ) {
            StringBuilder params = new StringBuilder("?");
            params.append("iss.meta=off");
            params.append("&marketdata.columns=");
            params.append("&dataversion.columns=");
            params.append("&marketdata_yields.columns=");
            params.append("&securities.columns=SHORTNAME");
            String uri = String.format("https://iss.moex.com/iss/engines/stock/markets/shares/boards/tqbr/securities/%s.json%s", stock, params);
            final HttpUriRequest httpGET = new HttpGet(uri);
            CloseableHttpResponse resp = httpClient.execute(httpGET);
            HttpEntity entity = resp.getEntity();
            String res = convertStreamToString(entity.getContent());

            return new JSONObject(res).getJSONObject("securities").get("data").toString().length() > 2;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Map<String, String> getDataAboutStock(String stock) {
        Map<String, String> resMap = new HashMap<>();
        try (
                CloseableHttpClient httpClient = HttpClientBuilder.create().build()
        ) {
            StringBuilder params = new StringBuilder("?");
            params.append("iss.meta=off");
            params.append("&marketdata.columns=BID,LASTTOPREVPRICE");
            params.append("&securities.columns=PREVPRICE,SHORTNAME");
            String uri = String.format("https://iss.moex.com/iss/engines/stock/markets/shares/boards/tqbr/securities/%s.json%s", stock, params.toString());
            final HttpUriRequest httpGET = new HttpGet(uri);

            CloseableHttpResponse resp = httpClient.execute(httpGET);
            HttpEntity entity = resp.getEntity();
            String res = convertStreamToString(entity.getContent());

            JSONObject json = new JSONObject(res);

            String SECURITIES = json.getJSONObject("securities").get("data").toString();
            String MARKETDATA = json.getJSONObject("marketdata").get("data").toString();

            String[] SEC_split = SECURITIES.substring(2, SECURITIES.length() - 2).split(",");
            String[] MD_split = MARKETDATA.substring(2, MARKETDATA.length() - 2).split(",");

            resMap.put("PREVPRICE", SEC_split[0]); // Последняя цена последней сделки нормального периода предыдущего торгового дня
            resMap.put("SHORTNAME", SEC_split[1]); // Краткое наименование ценной бумаги
            resMap.put("BID", MD_split[0]); // Лучшая котировка на покупку (Спрос)
            resMap.put("LASTTOPREVPRICE", MD_split[1]); // Изменение цены последней сделки к последней цене предыдущего дня, %
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resMap;
    }

    public static String convertStreamToString(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
