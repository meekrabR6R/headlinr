package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.mcsoxford.rss.RSSFault;
import org.mcsoxford.rss.RSSFeed;
import org.mcsoxford.rss.RSSItem;
import org.mcsoxford.rss.RSSReader;
import org.mcsoxford.rss.RSSReaderException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity {
    private final ArrayList<Integer> mCategories = new ArrayList<Integer>();
    private String rootUrl;
    private TextView mButton;
    private TextView mHeadLine;
    private TextView mSummary;
    private TextView mMetaData;
    private AdView mAdView;
    private ProgressBar mProgressBar;
    private Article mArticle;

    private static final String AD_UNIT_ID = "ca-app-pub-4547237027989566/2292472935";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().hide();
        Ion.getDefault(this).configure().setLogging("Headlinr", Log.DEBUG);

        if(savedInstanceState != null) {
            mArticle = new Article();

            String link = savedInstanceState.getString("link");
            String title = savedInstanceState.getString("title");
            String type = savedInstanceState.getString("type");
            String description = savedInstanceState.getString("description");
            String source = savedInstanceState.getString("source");
            String pubDate = savedInstanceState.getString("pubDate");

            mArticle.setProperties(source, title, link, description, pubDate);
        }

        rootUrl = getString(R.string.root_url);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mButton = (TextView) findViewById(R.id.button);
        mHeadLine = (TextView) findViewById(R.id.head_line);
        mHeadLine.setTypeface(null, Typeface.BOLD);
        mSummary = (TextView) findViewById(R.id.summary);
        mMetaData = (TextView) findViewById(R.id.metadata);

        setUpCategories();

        mAdView = (AdView) findViewById(R.id.banner);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mArticle != null) {

            outState.putString("link", mArticle.getLink());
            outState.putString("title", mArticle.getTitle().toString());
            outState.putString("type", mArticle.getType());
            outState.putString("description", mArticle.getDescription().toString());
            outState.putString("source", mArticle.source);
            outState.putString("pubDate", mArticle.pubDate);
        }
    }
    @Override
    public void onDestroy() {
        // Destroy the AdView.
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    private void setUpCategories() {
        Ion.with(this)
           .load(rootUrl + "categories.json")
           .asJsonArray()
           .setCallback(new FutureCallback<JsonArray>() {
               @Override
               public void onCompleted(Exception e, JsonArray result) {
                   if(e != null) {
                       return;
                   }
                   for(JsonElement entry : result) {
                       mCategories.add(entry.getAsJsonObject().get("category_id").getAsInt());
                   }

                   loadRandomArticle();
                   mButton.setOnClickListener(new View.OnClickListener() {
                       @Override
                       public void onClick(View v) {
                           mButton.setClickable(false);
                           loadRandomArticle();
                       }
                   });
               }
           });
    }

    private void loadRandomArticle() {
        mHeadLine.setVisibility(View.GONE);
        mSummary.setVisibility(View.GONE);
        mMetaData.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        Ion.with(this)
           .load(rootUrl + "categories/" + getRandomCategory() + "/articles.json")
           .asJsonObject()
           .setCallback(new FutureCallback<JsonObject>() {
               @Override
               public void onCompleted(Exception e, JsonObject result) {
                   if(e != null) {
                        Log.d("Articles", "Oops..");
                        return;
                    }
                    try {
                        JsonArray sources = result.getAsJsonArray("articles");
                        String description = result.getAsJsonPrimitive("description").getAsString();
                        JsonObject singleSource = sources.get(new Random().nextInt(sources.size())).getAsJsonObject();
                        String sourceUrl = singleSource.get("source_url").getAsString();

                        new RSSFeedTask().execute(sourceUrl, description, singleSource.get("source").getAsString());

                    } catch(JsonParseException error) {
                        Log.d("ParseError", error.getMessage());
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

    private class RSSFeedTask extends AsyncTask<String, String, Article> {
        protected Article doInBackground(String... strings) {
            RSSReader reader = new RSSReader();
            Article article = new Article();
            article.setMetaData(strings[1]);
            Thread.currentThread().setContextClassLoader(article.getClass().getClassLoader());
            try {
                RSSFeed feed = reader.load(strings[0]);

                List<RSSItem> items = feed.getItems();
                RSSItem item;

                if(items.size() >= 3)
                    item = items.get(new Random().nextInt(2));
                else if(items.size() >= 2)
                    item = items.get(new Random().nextInt(1));
                else if(items.size() == 0)
                    item = items.get(new Random().nextInt(0));
                else
                    return article;

                article.setProperties(strings[2], item.getTitle(), item.getLink().toString(),
                        item.getDescription(), item.getPubDate().toString());

            } catch(RSSReaderException e) {
                Log.d("RSS", e.getMessage());
            } catch(RSSFault e) {
                Log.d("RSS", e.getMessage());
            } catch(NullPointerException e) {
                Log.d("NullPointerException", "Stuff happened");
            } catch(IllegalArgumentException e) {
                Log.d("RSSFEED", "Feed is apparently 0 length");
            }
            return article;
        }

        protected void onPostExecute(Article article) {
            mArticle = article;
            if(article.getTitle() == null) {
                loadRandomArticle();
            } else {
                mProgressBar.setVisibility(View.GONE);
                mHeadLine.setVisibility(View.VISIBLE);
                mSummary.setVisibility(View.VISIBLE);
                mMetaData.setVisibility(View.VISIBLE);
                mHeadLine.setText(article.getUpperCaseTitle());

                mSummary.setText(article.getDescription());
                mMetaData.setText(article.getMetaData());

                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(article.getLink()));
                setClick(mHeadLine, intent);
                setClick(mSummary, intent);
                setClick(mMetaData, intent);

                mButton.setClickable(true);
            }
        }
    }

    private class Article {
        private String type, source, title, link, description, pubDate;

        public void setProperties(String source, String title, String link, String description, String pubDate) {
            this.source = source;
            this.title = title;
            this.link = link;
            this.description = description;
            this.pubDate = pubDate;
        }

        public Spanned getTitle() {
            if(title != null)
                return Html.fromHtml(title);
            else
                return null;
        }

        public String getUpperCaseTitle() {
            return title.toUpperCase();
        }

        public String getLink() {
            return link;
        }

        public Spanned getDescription() {
            String localDesc;
            if(description != null)
                localDesc = description.trim();
            else
                localDesc = "No summary";
            return removeImageSpanObjects(localDesc);
        }

        public String getType() {
            return type;
        }

        public void setMetaData(String description) {
            this.type = description;
        }

        public String getMetaData() {
            return type + " / " + source + "\n" + pubDate;
        }

        private Spanned removeImageSpanObjects(String inStr) {
            SpannableStringBuilder spannedStr = (SpannableStringBuilder) Html
                    .fromHtml(inStr.trim());
            Object[] spannedObjects = spannedStr.getSpans(0, spannedStr.length(),
                    Object.class);
            for (int i = 0; i < spannedObjects.length; i++) {
                if (spannedObjects[i] instanceof ImageSpan) {
                    ImageSpan imageSpan = (ImageSpan) spannedObjects[i];
                    spannedStr.replace(spannedStr.getSpanStart(imageSpan),
                            spannedStr.getSpanEnd(imageSpan), "");
                }
            }
            return spannedStr;
        }
    }

}
