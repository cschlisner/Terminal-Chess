package net.schlisner.terminalchess;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.annotation.Obsolete;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.entity.BasicHttpEntity;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;
import uniChess.Game;
import uniChess.INetPlayer;
import uniChess.Player;

/**
 * Created by cschl_000 on 4/23/2017.
 */

public class HTTPNetworkPlayer<T> extends Player<T> implements INetPlayer {


    public HTTPNetworkPlayer(T id, Game.Color c){
        super(id, c);
    }

    @Override
    public String getMoveAN() {
        try {
            ServerGetRequest sq = new ServerGetRequest() {
                @Override
                public void processResult(String result){
                    System.out.println(result);
                }
            };
            sq.execute("http://www.google.com");
            HttpEntity e = sq.getResponseEntity();
            return EntityUtils.toString(e);
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public void sendMoveAN(String in) {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private abstract class ServerGetRequest extends AsyncTask<String, Void, String> {

        private HttpEntity responseEntity;
        private HttpResponse response;

        @Override
        protected String doInBackground(String... params){
            try {
                response = getResponse(params);
                responseEntity = response.getEntity();
                return EntityUtils.toString(responseEntity);
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
        private HttpResponse getResponse (String... params){
            HttpClient httpclient = HttpClients.createDefault();
            HttpGet httppost = new HttpGet(params[0]);
            try {
                List<NameValuePair> nameValuePairs = new ArrayList<>(params.length);
                for (int i = 2; i < params.length; i += 2)
                    nameValuePairs.add(new BasicNameValuePair(params[i - 1], params[i]));
//                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                return httpclient.execute(httppost);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            processResult(result);
        }
        abstract void processResult(String result);

        public HttpEntity getResponseEntity(){
            return responseEntity;
        }
    }
}