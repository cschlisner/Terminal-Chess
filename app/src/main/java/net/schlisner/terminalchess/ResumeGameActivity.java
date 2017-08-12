package net.schlisner.terminalchess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Pair;
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
    final Activity activity = this;
    private static String uuid;
    ListView gameList;
    SwipeRefreshLayout srl;
    ProgressBar pb;
    TextView loadingMsg;
    TextView noGamesMsg;
    SharedPreferences sharedPref;
    JSONArray savedGameArray = new JSONArray();
    List<JSONObject> gamesJSONList;
    HandlerThread ht;
    Handler updateHandler;
    ChessGameListAdapter cgla;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        final ActionBar actionBar = getSupportActionBar();
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
                ActivityOptionsCompat opt = ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                        android.support.v4.util.Pair.create(view.findViewById(R.id.boardviewer_listicon), getString(R.string.transition_boardview)));
//                getWindow().setAllowReturnTransitionOverlap(false);
//                getWindow().setAllowEnterTransitionOverlap(false);
//                startActivity(i);
                ActivityCompat.startActivity(ResumeGameActivity.this, i, opt.toBundle());
            }
        });

        gamesJSONList = new ArrayList<>();
        cgla = new ChessGameListAdapter(getApplicationContext(), gamesJSONList, uuid);
        gameList.setAdapter(cgla);

        pb = (ProgressBar)findViewById(R.id.meme);
        pb.getIndeterminateDrawable().setColorFilter(Color.parseColor("#00740c"), PorterDuff.Mode.MULTIPLY);


        loadingMsg = (TextView)findViewById(R.id.loadingText);
        noGamesMsg = (TextView)findViewById(R.id.noGames);
//        gameList.setEmptyView(pb);

        srl = (SwipeRefreshLayout)findViewById(R.id.swiperefresh);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });

        sharedPref = this.getSharedPreferences("Dank Memes(c)",Context.MODE_PRIVATE);

        ht = new HandlerThread("networkupdate");
        ht.start();
        updateHandler = new Handler(ht.getLooper());
    }
    @Override
    public void onResume(){
        super.onResume();
        final String savedGameList = sharedPref.getString("savedGames", null);


        // if we have saved games, populate the list with the saved games before updating from network

        updateHandler.post(new Runnable() {
            @Override
            public void run() {
//                if (savedGameList  != null){
//                    System.out.println("Loading saved game data..."+savedGameList);
//                    // games stored in json array
//                    try {
//                        savedGameArray = new JSONArray(savedGameList);
//                        savedGameJSONList = new ArrayList<>();
//                        for (int i = 0; i < savedGameArray.length(); ++i)
//                            savedGameJSONList.add(savedGameArray.getJSONObject(i));
//                        final ChessGameListAdapter cgla = new ChessGameListAdapter(getApplicationContext(), savedGameJSONList, uuid);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                gameList.setAdapter(cgla);
//                            }
//                        });
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }
                refreshList();
            }
        });
        ChessUpdater.cancelAlarm(this);
    }

    @Override
    public void onPause(){
        super.onPause();
        ChessUpdater.setAlarm(this);
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

                }

                @Override
                public void after(String response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                    try {
                        long t1 = System.currentTimeMillis();
                        JSONArray gamesJSON = new JSONArray(response);
                        System.out.println(response);

                        cgla.clear();
                        for (int i = 0; i < gamesJSON.length(); ++i)
                            cgla.add(gamesJSON.getJSONObject(i));
                        cgla.notifyDataSetChanged();

                        SharedPreferences.Editor e = sharedPref.edit();
                        e.putString("savedGames", gamesJSON.toString());
                        e.apply();

                        if (gamesJSON.length() == 0)
                            noGamesMsg.setVisibility(View.VISIBLE);
                        else noGamesMsg.setVisibility(View.GONE);
                        System.out.println("Updated games from network: "+(System.currentTimeMillis()-t1));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pb.setVisibility(View.GONE);
                                loadingMsg.setVisibility(View.GONE);
                            }
                        });

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
