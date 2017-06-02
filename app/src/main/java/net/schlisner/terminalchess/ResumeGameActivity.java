package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uniChess.Board;
import uniChess.Game;

public class ResumeGameActivity extends AppCompatActivity {

    private String uuid;
    ListView gameList;
    SwipeRefreshLayout srl;
    TextView netErr;
    SharedPreferences sharedPref;
    JSONArray savedGameArray = new JSONArray();
    List<JSONObject> savedGameJSONList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        Intent menuIntent = getIntent();
        uuid = menuIntent.getStringExtra("uuid");

        setContentView(R.layout.activity_resume_game);

        gameList = (ListView)findViewById(R.id.gameListView);

        gameList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), GameActivity.class)
                        .putExtra("opponent", "network")
                        .putExtra("uuid", uuid)
                        .putExtra("gameJSON", (gameList.getAdapter().getItem(position)).toString());
                startActivity(i);
            }
        });

        netErr = (TextView)findViewById(R.id.networkErrorTextView);
//        gameList.setEmptyView(pb);

        srl = (SwipeRefreshLayout)findViewById(R.id.swiperefresh);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String savedGameList = sharedPref.getString("savedGames", null);

        // if we have saved games, populate the list with the saved games before updating from network
        if (savedGameList  != null){
            System.out.println("Loading saved game data...");
            // games stored in json array
            try {
                savedGameArray = new JSONArray(savedGameList);
                savedGameJSONList = new ArrayList<>();
                for (int i = 0; i < savedGameArray.length(); ++i)
                    savedGameJSONList.add(savedGameArray.getJSONObject(i));
                ChessGameListAdapter cgla = new ChessGameListAdapter(getApplicationContext(), savedGameJSONList, uuid);
                gameList.setAdapter(cgla);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }
    @Override
    public void onResume(){
        super.onResume();
       refreshList();
    }

    // updates existing gameList with data from network
    private void refreshList(){
        System.out.println("updating games...");
        try {
            final SwipeRefreshLayout swipeRefreshLayout = srl;
            final Context appContext = this.getApplicationContext();
            PostOffice.listGamesJSON(uuid, new PostOffice.MailCallback() {
                @Override
                public void before() {
                    System.out.println("meme doot danks");
                    swipeRefreshLayout.setRefreshing(true);
                }

                @Override
                public void after(String response) {
                    swipeRefreshLayout.setRefreshing(false);
                    try {
                        JSONArray gamesJSON = new JSONArray(response);
                        SharedPreferences.Editor e = sharedPref.edit();
                        e.putString("savedGames", gamesJSON.toString());
                        e.commit();
                        List<JSONObject> gamesJSONList = new ArrayList<>();
                        for (int i = 0; i < gamesJSON.length(); ++i)
                            gamesJSONList.add(gamesJSON.getJSONObject(i));
                        ChessGameListAdapter cgla = new ChessGameListAdapter(appContext, gamesJSONList, uuid);
                        gameList.setAdapter(cgla);
                        System.out.println("Updated games from network");
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
