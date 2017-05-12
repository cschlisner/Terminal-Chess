package net.schlisner.terminalchess;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.*;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

/**
 * Created by cschl_000 on 5/11/2017.
 */

public class PostOffice extends AsyncTask<String, Void, String> {

    private static final String Server = "http://http://138.197.213.251/";
    private static final String Chives = Server+"cgi-bin/Chives.py";
    private static final String Chesster = Server+"cgi-bin/Chesster.py";

    /**
     * Registers a new chess player with the server and returns the corresponding uuid
     * @return UUID of created player
     */
    public String register(){
        doInBackground(Chives, "action", "register");
        return responseBody; // should be just the uuid text
    }

    /**
     * Lists all game IDs in ascending order of time created (newest game first)
     * @return List of all games user is engaged in, by time created
     */
    public List<String> listGames(String uuid){
        List<String> gameIDList = new ArrayList<>();
        doInBackground(Chives, "action", "retrieveGames");

        return gameIDList;
    }

    private HttpResponse getResponse (String... params){
        HttpClient httpclient = HttpClients.createDefault();
        try {
            HttpPost httppost = new HttpPost(params[0]);
            List<NameValuePair> nameValuePairs = new ArrayList<>(params.length);
            for (int i = 2; i < params.length; i += 2)
                nameValuePairs.add(new BasicNameValuePair(params[i], params[i+1]));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            System.out.println("Sending post [ "+httppost.toString()+" ]");
            return httpclient.execute(httppost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            HttpResponse response = getResponse(params);
            HttpEntity responseEntity = response.getEntity();
            return EntityUtils.toString(responseEntity);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private String responseBody;
    @Override
    protected void onPostExecute(String result) {
        responseBody = result;
    }
}
