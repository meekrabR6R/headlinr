package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.LoginButton;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity implements ArticleFragment.OnArticleFragmentInteractionListener,
                                                      MoreFragment.OnMoreFragmentInteractionListener {
    private final ArrayList<Integer> mCategories = new ArrayList<Integer>();
    private SharedPreferences mSettings;
    private String rootUrl;
    private TextView mButton;
    private TextView mHeadLine;
    private TextView mSummary;
    private TextView mMetaData;
    private AdView mAdView;
    private TextView mFeedZilla;
    private ProgressBar mProgressBar;
    private Article mArticle;
    private UiLifecycleHelper mUiHelper;
    private LoginButton mHiddenButton;
    private ImageView mMore;
    private String mTwitterAccessToken;
    private String mTwitterApiKey;
    private String mTwitterSecret;
    private boolean mShouldSetUpCategories = false;

    private static final String AD_UNIT_ID = "ca-app-pub-4547237027989566/2292472935"; //should move

    private Session.StatusCallback mCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getActionBar().hide();
        Ion.getDefault(this).configure().setLogging("Headlinr", Log.DEBUG);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        mUiHelper = new UiLifecycleHelper(this, mCallback);
        mUiHelper.onCreate(savedInstanceState);
        mHiddenButton = (LoginButton) findViewById(R.id.hidden_fb_login_button);

        mTwitterApiKey = getString(R.string.twitter_api_key);
        mTwitterSecret = getString(R.string.twitter_app_secret);

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

        mMore = (ImageView) findViewById(R.id.more);
        mMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MoreFragment moreFragment = MoreFragment.newInstance();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.addToBackStack("more_fragment");
                transaction.replace(R.id.main_view, moreFragment);
                transaction.commit();
                mButton.setVisibility(View.GONE);
                mAdView.setVisibility(View.GONE);
                mFeedZilla.setVisibility(View.GONE);
            }
        });

        rootUrl = getString(R.string.root_url);

        mFeedZilla = (TextView) findViewById(R.id.feedzilla);
        Article feedZilla = new Article();
        feedZilla.setLink(getString(R.string.feedzilla_url));
        setClick(mFeedZilla, feedZilla);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mUiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Log.e("Activity", String.format("Error: %s", error.toString()));
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Log.i("Activity", "Success!");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
        mUiHelper.onResume();
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        mUiHelper.onPause();
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

        mUiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        mUiHelper.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onArticleFragmentBackInteraction() {
        getFragmentManager().popBackStack("article_fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mButton.setVisibility(View.VISIBLE);
        mAdView.setVisibility(View.VISIBLE);
        mFeedZilla.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFacebookShareInteraction(String url) {
        if (FacebookDialog.canPresentShareDialog(getApplicationContext(),
                FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
            // Publish the post using the Share Dialog
            FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
                    .setLink(url)
                    .build();
            mUiHelper.trackPendingDialogCall(shareDialog.present());

        } else {
            Log.d("Fallback", "Face");
            publishFeedDialog(url);
        }
    }

    @Override
    public void onMoreFragmentFilterSelection(ArrayList<Integer> filters) {
        SharedPreferences.Editor editor = mSettings.edit();
        if(filters.size() > 0) {
            mCategories.clear();
            mCategories.addAll(filters);

            Set<String> filterStrings = new HashSet<String>();
            for (Integer filter : filters)
                filterStrings.add(String.valueOf(filter));

            editor.putStringSet("filters", filterStrings);
            editor.commit();
            //setUpCategories();
        } else {
            editor.remove("filters");
            editor.commit();
            //setUpCategories();
        }
    }

    @Override
    public void onMoreFragmentBackInteraction(boolean shouldSetUpCategories) {
        getFragmentManager().popBackStack("more_fragment", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mButton.setVisibility(View.VISIBLE);
        mAdView.setVisibility(View.VISIBLE);
        mFeedZilla.setVisibility(View.VISIBLE);

        mShouldSetUpCategories = shouldSetUpCategories;
    }

    @Override
    public void onTwitterShareInteraction(String title, String url) {
        String message = title + " from @HeadlinrAndroid";


        String twitterUrl = "http://www.twitter.com/intent/tweet?url="+url+"&text="+message;
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(twitterUrl));
        startActivity(i);
    }

    private void setUpCategories() {
        mProgressBar.setVisibility(View.VISIBLE);
        Set<String> filterSet = mSettings.getStringSet("filters", null);
        if(filterSet != null) {
            for (String filter : filterSet)
                mCategories.add(Integer.parseInt(filter));

            setUpButton();

        } else {
            Ion.with(this)
                    .load(rootUrl + "categories.json")
                    .asJsonArray()
                    .setCallback(new FutureCallback<JsonArray>() {
                        @Override
                        public void onCompleted(Exception e, JsonArray result) {
                            if (e != null) {
                                return;
                            }
                            for (JsonElement entry : result) {
                                int categoryId = entry.getAsJsonObject().get("category_id").getAsInt();
                                if (categoryId != 18)
                                    mCategories.add(entry.getAsJsonObject().get("category_id").getAsInt());
                            }
                            setUpButton();
                        }
                    });
        }
    }

    //Ugly little bitch..
    private void setUpButton() {
        loadRandomArticle();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton.setClickable(false);
                if(mShouldSetUpCategories)
                    setUpCategories();
                else
                    loadRandomArticle();
            }
        });
    }

    private void loadRandomArticle() {
        mHeadLine.setVisibility(View.GONE);
        mSummary.setVisibility(View.GONE);
        mMetaData.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mMore.setClickable(false);
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

    private void setClick(View view, final Article article) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ArticleFragment articleFragment = ArticleFragment.newInstance(article);
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.addToBackStack("article_fragment");
                    //transaction.setCustomAnimations(R.animator.slide_in,R.animator.slide_out)
                    transaction.replace(R.id.main_view, articleFragment);
                    transaction.commit();
                    mButton.setVisibility(View.GONE);
                    mAdView.setVisibility(View.GONE);
                    mFeedZilla.setVisibility(View.GONE);
                } catch(Exception e) {
                    Log.d("URL", "Possibly malformed");
                    Toast.makeText(MainActivity.this, "The link appears to be broken. :(", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private int getRandomCategory() {
        return mCategories.get((new Random()).nextInt(mCategories.size()));
    }

    private void publishFeedDialog(String url) {
        Bundle params = new Bundle();
        //params.putString("name", "Headlinr for Android");
        params.putString("link", url);
        if(Session.getActiveSession().isOpened()) {
            WebDialog feedDialog = (
                    new WebDialog.FeedDialogBuilder(this,
                            Session.getActiveSession(),
                            params))
                    .setOnCompleteListener(new OnCompleteListener() {

                        @Override
                        public void onComplete(Bundle values,
                                               FacebookException error) {
                            if (error == null) {
                                // When the story is posted, echo the success
                                // and the post Id.
                                final String postId = values.getString("post_id");
                                if (postId != null) {
                                    Toast.makeText(MainActivity.this,
                                            "Story posted!",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Generic, ex: network error
                                Toast.makeText(MainActivity.this.getApplicationContext(),
                                        "Error posting story",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                    })
                    .build();
            feedDialog.show();
        } else {
            Log.d("Facebook", "Inactive");
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.fb_login);
            LoginButton authButton = (LoginButton) dialog.findViewById(R.id.fb_login_button);
            authButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHiddenButton.performClick();
                }
            });
            dialog.show();
        }
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state.isOpened()) {
            Log.i("Facebook", "Logged in...");
        } else if (state.isClosed()) {
            Log.i("Facebook", "Logged out...");
        }
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
                //String url = article.getLink();
                setClick(mHeadLine, article);
                setClick(mSummary, article);
                setClick(mMetaData, article);

                mButton.setClickable(true);
                mMore.setClickable(true);

            }
        }
    }
}