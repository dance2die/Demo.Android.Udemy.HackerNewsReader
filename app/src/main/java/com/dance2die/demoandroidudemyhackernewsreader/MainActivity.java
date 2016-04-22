package com.dance2die.demoandroidudemyhackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<>();
    Map<Integer, String> articleTitles = new HashMap<>();
    ArrayList<Integer> articleIds = new ArrayList<>();

    SQLiteDatabase articleDB;

    ArrayList<String> titles = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    ArrayList<String> urls = new ArrayList<>();

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while (data != -1){
                    char current = (char)data;
                    result += current;

                    data = reader.read();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("articleURL", urls.get(position));
                startActivity(i);

                Log.i("articleURL", urls.get(position));
            }
        });


        articleDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (" +
                "id INTEGER PRIMARY KEY," +
                "articleId INTEGER, " +
                "url VARCHAR, " +
                "title VARCHAR, " +
                "content VARCHAR)");


        DownloadTask task = new DownloadTask();
        try {
            String result = task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
            Log.i("Result", result);

            JSONArray jsonArray = new JSONArray(result);
            articleDB.execSQL("DELETE FROM articles");

            // top 20 results are OK.
            for (int i = 0; i <= 20; i++){
                String articleId = jsonArray.getString(i);
                String url = String.format("https://hacker-news.firebaseio.com/v0/item/%s.json?print=pretty", articleId);

                DownloadTask getArticleTask = new DownloadTask();
                String articleInfo = getArticleTask.execute(url).get();
                JSONObject jsonObject = new JSONObject(articleInfo);

                String articleTitle = jsonObject.getString("title");
                String articleURL = jsonObject.getString("url");

                articleIds.add(Integer.valueOf(articleId));
                articleTitles.put(Integer.valueOf(articleId), articleTitle);
                articleURLs.put(Integer.valueOf(articleId), articleURL);

                String sql = "INSERT INTO articles(articleId, url, title) VALUES (?, ?, ?)";
                SQLiteStatement statement = articleDB.compileStatement(sql);
                statement.bindString(1, articleId);
                statement.bindString(2, articleURL);
                statement.bindString(3, articleTitle);
                statement.execute();
            }

            Cursor c = articleDB.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);
            try {
                int articleIdIndex = c.getColumnIndex("articleId");
                int urlIndex = c.getColumnIndex("url");
                int titleIndex = c.getColumnIndex("title");

                titles.clear();
                urls.clear();

                c.moveToFirst();
                while (!c.isLast()) {
                    String articleTitle = c.getString(titleIndex);
                    titles.add(articleTitle);
                    urls.add(c.getString(urlIndex));

                    c.moveToNext();
                }

                arrayAdapter.notifyDataSetChanged();
            } finally {
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            articleDB.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
