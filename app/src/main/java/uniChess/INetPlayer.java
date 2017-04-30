package uniChess;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.annotation.Obsolete;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

/**
 * Created by cschl_000 on 4/23/2017.
 */

public abstract class INetPlayer<T> extends Player<T>{
    public INetPlayer(T id, Game.Color c){
        super(id, c);
    }
    public abstract String getMoveAN();
    public abstract void sendMoveAN(String in);

    protected abstract class NetworkThread<T extends Object> extends AsyncTask<T, Void, String> {
        @Override
        protected String doInBackground(T... params){
            return backgroundTask(params);
        }
        @Override
        protected void onPostExecute(String result) {
            processResult(result);
        }
        protected abstract void processResult(String result);
        protected abstract String backgroundTask(T... params);
    }
}
