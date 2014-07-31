package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends Activity {
    private final ArrayList<Integer> mCategories = new ArrayList<Integer>();
    private String rootUrl;
    private TextView mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().hide();
        Ion.getDefault(this).configure().setLogging("Headlinr", Log.DEBUG);
        rootUrl = getString(R.string.root_url);
        mButton = (TextView) findViewById(R.id.button);
        setUpCategories();
    }

    private void setUpCategories() {
        Ion.with(this)
           .load(rootUrl + "categories.json")
           .asJsonArray()
           .setCallback(new FutureCallback<JsonArray>() {
               @Override
               public void onCompleted(Exception e, JsonArray result) {
                   if(e != null) {
                       Log.d("Categories", e.getMessage());
                       return;
                   }
                   for(JsonElement entry : result) {
                       mCategories.add(entry.getAsJsonObject().get("category_id").getAsInt());
                   }

                   loadRandomArticle();
                   mButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           loadRandomArticle();
                       }
                   });
               }
           });
    }

    private void loadRandomArticle() {
        Ion.with(this)
           .load(rootUrl + "categories/" + getRandomCategory() + "/articles.json")
           .asJsonObject()
           .setCallback(new FutureCallback<JsonObject>() {
               @Override
               public void onCompleted(Exception e, JsonObject result) {
                    if(e != null) {
                        Log.d("Articles", e.getMessage());
                        return;
                    }
                    try {
                        JsonArray articles = result.getAsJsonArray("articles");
                        String description = result.getAsJsonPrimitive("description").getAsString();

                        JsonObject singleArticle = articles.get(new Random().nextInt(articles.size())).getAsJsonObject();
                        Article article = new Gson().fromJson(singleArticle, Article.class);
                        article.setMetaData(description);

                        TextView headLine = (TextView) findViewById(R.id.head_line);
                        TextView summary = (TextView) findViewById(R.id.summary);
                        TextView metaData = (TextView) findViewById(R.id.metadata);

                        headLine.setText(article.getUpperCaseTitle());
                        summary.setText(article.getFormattedSummary());
                        metaData.setText(article.getMetaData());

                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(article.url));

                        setClick(headLine, intent);
                        setClick(summary, intent);
                        setClick(metaData, intent);

                    } catch(Exception error) {
                        Log.d("ParseError", e.getMessage());
                    }
               }
           });
    }

    private void setClick(View view, final Intent intent) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(intent);
            }
        });
    }

    private int getRandomCategory() {
        return mCategories.get((new Random()).nextInt(mCategories.size()));
    }

    private class Article {
        @SerializedName("source_url")
        String sourceUrl;
        @SerializedName("publish_date")
        String publishDate;
        private String type;
        private String source, summary, title;
        String url;

        public String getUpperCaseTitle() {
            return title.toUpperCase();
        }

        public String getFormattedSummary() {
            return summary.replace("\n", " ").replace("\r", " ");
        }

        public void setMetaData(String description) {
            this.type = description;
        }

        public String getMetaData() {
            return type + " / " + source + " / " + publishDate.substring(0,16);
        }
    }

}
