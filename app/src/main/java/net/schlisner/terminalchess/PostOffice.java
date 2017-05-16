package net.schlisner.terminalchess;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.json.*;

import cz.msebera.android.httpclient.Header;
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

public class PostOffice {

    // Addresses of our friends
    private static final String Server = "http://138.197.213.251/";
    private static final String Chives = Server + "cgi-bin/Chives.py";
    private static final String Chesster = Server + "cgi-bin/Chesster.py";
    private static final int NETWORK_TIMEOUT_SECS = 5;
    /**
     * Registers a new chess player with the server and returns the corresponding uuid
     *
     * @return UUID of created player
     */
    public String register() throws Exception{
        MailSend registerPlayer = new MailSend();
        String meme = registerPlayer.execute(Chives, "action", "register").get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);
        return meme; // should be just the uuid text
    }

    /**
     * Checks player into lobby where it will be matched with another player
     *
     * @return List of all games user is engaged in, by time created
     */
    public void checkIn(String uuid) {
        MailSend checkIn = new MailSend();
        checkIn.execute(Chives, "action", "checkin");
    }

    /**
     * Checks player into lobby, looks for new game including player, returns game id
     *
     * @return game id of newly created game after checking player into lobby
     */
    public String joinNewGame(String uuid) throws Exception{
        int gameCount = listGames(uuid).size();
        MailSend checkIn = new MailSend();
        checkIn.execute(Chives, "action", "checkin");
        long t1 = System.currentTimeMillis();
        while ((System.currentTimeMillis() - t1) < NETWORK_TIMEOUT_SECS*1000){
            List<String> games = listGames(uuid);
            if (games.size() > gameCount){
                return games.get(games.size()-1);
            }
        }
        throw new Exception("Network timeout");
    }


    /**
     * Lists all game IDs in ascending order of time created (newest game first)
     *
     * @return List of all games user is engaged in, by time created
     */
    public List<String> listGames(String uuid) throws Exception{
        List<String> gameIDList = new ArrayList<>();
        MailSend getGameList = new MailSend();
        String response = getGameList.execute(Chives, "action", "retrieveGames").get(NETWORK_TIMEOUT_SECS, TimeUnit.SECONDS);

        // idfk
        String[] games = response.split("grhijuagrhiarhirghirhioragih ");

        return gameIDList;
    }

    private class MailSend extends AsyncTask<String, Void, String> {
        /**
         * Make an HTTP POST request
         *
         * @param params first parameter will be assumed to be the URL, the following parameters will
         *               be treated as key-value pairs to be sent in the request body
         * @return
         */
        private HttpResponse getResponse(String... params) {
            HttpClient httpclient = HttpClients.createDefault();
            try {
                HttpPost httppost = new HttpPost(params[0]);
                System.out.println("Sending post [ " + httppost.toString() + " ]");
                List<NameValuePair> nameValuePairs = new ArrayList<>(params.length);
                System.out.print("With Keys [ ");
                for (int i = 1; i < params.length; i += 2) {
                    nameValuePairs.add(new BasicNameValuePair(params[i], params[i + 1]));
                    System.out.format("%s=%s ", params[i], params[i+1]);
                }
                System.out.println("]");
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                System.out.print("With Headers [ ");
                for (Header h : httppost.getAllHeaders())
                    System.out.println(h.getName()+"::"+h.getValue()+" ");
                System.out.println("]");
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
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}