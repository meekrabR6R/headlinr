package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.FragmentManager;
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
import org.redplatoon.headlinr.app.models.Article;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements ArticleFragment.OnArticleFragmentInteractionListener {
    private final ArrayList<Integer> mCategories = new ArrayList<Integer>();
    private String rootUrl;
    private TextView mButton;
    private TextView mHeadLine;
    private TextView mSummary;
    private TextView mMetaData;
    private AdView mAdView;
    private TextView mFeedZilla;
    private ProgressBar mProgressBar;
    private Article mArticle;

    private static final String AD_UNIT_ID = "ca-app-pub-4547237027989566/2292472935";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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

        mFeedZilla = (TextView) findViewById(R.id.feedzilla);
        //Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.setData(Uri.parse(getString(R.string.feedzilla_url)));
        setClick(mFeedZilla, getString(R.string.feedzilla_url));

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
    public void onBackPressed() {
        super.onBackPressed();
        onArticleFragmentBackInteraction();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mArticle != null) {

            outState.putString("link", mArticle.getLink());
            outState.putString("title", mArticle.getTitle().toString());
            outState.putString("type", mArticle.getType());
            outState.putString("description", mArticle.getDescription().toString());
            outState.putString("source", mArticle.getSource());
            outState.putString("pubDate", mArticle.getPubDate());
        }
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onArticleFragmentBackInteraction() {
        getFragmentManager().popBackStack("article_fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mButton.setVisibility(View.VISIBLE);
        mAdView.setVisibility(View.VISIBLE);
        mFeedZilla.setVisibility(View.VISIBLE);
    }

    private void setUpCategories() {
        mProgressBar.setVisibility(View.VISIBLE);
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
                        int categoryId = entry.getAsJsonObject().get("category_id").getAsInt();
                        if(categoryId != 18)
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
                    if (e != null) {
                        Log.d("Articles", "Oops..");
                        return;
                    }
                    try {
                        JsonArray sources = result.getAsJsonArray("articles");
                        String description = result.getAsJsonPrimitive("description").getAsString();
                        JsonObject singleSource = sources.get(new Random().nextInt(sources.size())).getAsJsonObject();
                        String sourceUrl = singleSource.get("source_url").getAsString();

                        new RSSFeedTask().execute(sourceUrl, description, singleSource.get("source").getAsString());

                    } catch (JsonParseException error) {
                        Log.d("ParseError", error.getMessage());
                    }
                }
            });
    }

    private void setClick(View view, final String url) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //setContentView(R.layout.article);

                    ArticleFragment articleFragment = ArticleFragment.newInstance(url);
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.addToBackStack("article_fragment");
                    transaction.replace(R.id.main_view, articleFragment);
                    transaction.commit();
                    mButton.setVisibility(View.GONE);
                    mAdView.setVisibility(View.GONE);
                    mFeedZilla.setVisibility(View.GONE);
                } catch(Exception e) {
                    Log.d("URL", "Possibly malformed");
                    Toast.makeText(MainActivity.this, "The link appears to be broken. :(", Toast.LENGTH_LONG).show();
                    //intent.setData(Uri.parse("http://" + intent.getData().toString()));
                    //startActivity(intent);
                }
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
                else {
                    article.setIsInvalid(true);
                    return article;
                }
                article.setProperties(strings[2], item.getTitle(), item.getLink().toString(),
                        item.getDescription(), item.getPubDate().toString());

            } catch(RSSReaderException e) {
                Log.d("RSS", e.getMessage());
                article.setIsInvalid(true);
            } catch(RSSFault e) {
                Log.d("RSS", e.getMessage());
                article.setIsInvalid(true);
            } catch(NullPointerException e) {
                Log.d("NullPointerException", "Stuff happened");
                article.setIsInvalid(true);
            } catch(IllegalArgumentException e) {
                Log.d("RSSFEED", "Feed is apparently 0 length");
                article.setIsInvalid(true);
            }
            return article;
        }

        protected void onPostExecute(Article article) {
            mArticle = article;
            if(article.isInvalid()) {
                loadRandomArticle();
            } else {
                mProgressBar.setVisibility(View.GONE);
                mHeadLine.setVisibility(View.VISIBLE);
                mSummary.setVisibility(View.VISIBLE);
                mMetaData.setVisibility(View.VISIBLE);
                mHeadLine.setText(article.getUpperCaseTitle());

                mSummary.setText(article.getDescription());
                mMetaData.setText(article.getMetaData());

                //final Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent.setData(Uri.parse(article.getLink()));
                String url = article.getLink();
                setClick(mHeadLine, url);
                setClick(mSummary, url);
                setClick(mMetaData, url);

                mButton.setClickable(true);
            }
        }
    }
}