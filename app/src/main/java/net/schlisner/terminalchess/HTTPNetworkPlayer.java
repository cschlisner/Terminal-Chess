package net.schlisner.terminalchess;

import android.os.AsyncTask;

import com.google.common.hash.HashCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.annotation.Obsolete;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
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

public class HTTPNetworkPlayer<T> extends INetPlayer<T> {

    String game_uuid="";

    public HTTPNetworkPlayer(T id, Game.Color c){
        super(id, c);
        this.game_uuid = checkIn();


    }

    @Override
    public String getMoveAN() {
//        final StringBuilder sb = new StringBuilder();
//        try {
//            HTTPGet sq = new HTTPGet() {
//                @Override
//                public void processResult(String result){
//                    System.out.println(result);
//                    sb.append(result);
//                }
//            };
//            sq.execute("http://www.google.com");
//        }catch (Exception e){
//            return null;
//        }
//        return sb.toString();
        return "";
    }

    @Override
    public void sendMoveAN(String in) {
        final StringBuilder sb = new StringBuilder();
        try {
            HTTPThread sq = new HTTPThread() {
                @Override
                public void processResult(String result){
                    System.out.println("Response from server: "+result);
                }
            };
            sq.execute("http://138.197.213.251/cgi-bin/Chesster.py", "post",
                    "move", in,
                    "uuid", this.getID().toString(),
                    "game", this.game_uuid);
        }catch (Exception e){
        }
    }

    public String checkIn(){
        final StringBuilder sb = new StringBuilder();
        try {
            HTTPThread sq = new HTTPThread() {
                @Override
                public void processResult(String result){
                }
            };
            sq.execute("http://138.197.213.251/cgi-bin/Chives.py", "post",
                    "action", "checkin");
            return sb.toString();
        }catch (Exception e){
        }
        return "";
    }

    public HTTPNetworkPlayer<String> findOpponent(){
        // find geospacially closest online user and return Player object to represent them
        return new HTTPNetworkPlayer<>((UUID.randomUUID()).toString(), Game.getOpposite(this.color));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private abstract class HTTPThread extends NetworkThread<String> {
        @Override
        protected String backgroundTask(String... params){
            try {
                HttpResponse response = getResponse(params);
                HttpEntity responseEntity = response.getEntity();
                return EntityUtils.toString(responseEntity);
            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }
        private HttpResponse getResponse (String... params){
            HttpClient httpclient = HttpClients.createDefault();
            try {
                switch (params[1]){
                    case "get":
                        HttpGet httpget = new HttpGet(params[0]);
                        return httpclient.execute(httpget);
                    case "post":
                        HttpPost httppost = new HttpPost(params[0]);
                        List<NameValuePair> nameValuePairs = new ArrayList<>(params.length);
                        for (int i = 2; i < params.length; i += 2)
                            nameValuePairs.add(new BasicNameValuePair(params[i], params[i+1]));
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        System.out.println("Sending post [ "+httppost.toString()+" ]");
                        return httpclient.execute(httppost);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            processResult(result);
        }
        protected abstract void processResult(String result);
    }
}