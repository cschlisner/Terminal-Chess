package net.schlisner.terminalchess;

import android.content.Context;
import android.content.Intent;
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
        netErr = (TextView)findViewById(R.id.networkErrorTextView);
//        gameList.setEmptyView(pb);

        srl = (SwipeRefreshLayout)findViewById(R.id.swiperefresh);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            }
        });
        refreshList();

    }
    @Override
    public void onResume(){
        super.onResume();
        refreshList();
    }

    private void refreshList(){
        try {
            srl.setRefreshing(true);
            final List<JSONObject> gamesJSON = PostOffice.listGamesJSON(uuid);
            ChessGameListAdapter adapter = new ChessGameListAdapter(getApplicationContext(), gamesJSON);
            gameList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(getApplicationContext(), GameActivity.class)
                            .putExtra("opponent", "network")
                            .putExtra("uuid", uuid)
                            .putExtra("gameJSON", gamesJSON.get(position).toString());
                    startActivity(i);
                }
            });
            gameList.setAdapter(adapter);
            srl.setRefreshing(false);
        } catch (Exception e){
            netErr.setVisibility(View.VISIBLE);
        }
    }
}
