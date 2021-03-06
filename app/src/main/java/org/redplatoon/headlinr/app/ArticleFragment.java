package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.redplatoon.headlinr.app.custom.ArticleWebView;
import org.redplatoon.headlinr.app.models.Article;

import it.sephiroth.android.library.easing.Cubic;
import it.sephiroth.android.library.easing.EasingManager;


/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ArticleFragment.OnArticleFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ArticleFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ArticleFragment extends Fragment {

    private static final String URL = "url";
    private static final String TITLE = "title";

    private String mUrl;
    private String mTitle;
    private OnArticleFragmentInteractionListener mListener;
    private ImageView mBack;
    private ImageView mShare;
    private LinearLayout mCustomActionBar;
    private int mScrollPosition = 0;
    private boolean mIsCustomActionBarVisible = true;
    private boolean mIsListeningToScroll = true;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ArticleFragment.
     */
    public static ArticleFragment newInstance(Article article) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(TITLE,article.getTitle().toString());
        args.putString(URL, article.getLink());
        fragment.setArguments(args);
        return fragment;
    }
    public ArticleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUrl = getArguments().getString(URL);
            mTitle = getArguments().getString(TITLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.article, container, false);

        mCustomActionBar = (LinearLayout) rootView.findViewById(R.id.share_bar);

        final ArticleWebView webView = (ArticleWebView) rootView.findViewById(R.id.article);
        webView.setBackgroundColor(getResources().getColor(R.color.background));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        final ProgressBar progress = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        final int webViewBackground = getResources().getColor(R.color.text);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.setBackgroundColor(webViewBackground);
                progress.setVisibility(View.GONE);
            }
        });

        webView.setOnTouchListener(new ArticleTouchListener());

        webView.loadUrl(mUrl);
        webView.setVisibility(View.VISIBLE);
        webView.setBackgroundColor(getResources().getColor(R.color.text));

        mBack = (ImageView) rootView.findViewById(R.id.back);
        mShare = (ImageView) rootView.findViewById(R.id.share);

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed();
            }
        });

        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(getActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.share);

                TextView facebook   = (TextView) dialog.findViewById(R.id.facebook);
                TextView twitter    = (TextView) dialog.findViewById(R.id.twitter);
                TextView email      = (TextView) dialog.findViewById(R.id.email);
                TextView sms        = (TextView) dialog.findViewById(R.id.sms);

                facebook.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onFacebookShareInteraction(mUrl);
                    }
                });

                twitter.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onTwitterShareInteraction(mTitle, mUrl);
                    }
                });

                email.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmail();
                    }
                });

                sms.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendSMS();
                    }
                });
                dialog.show();
            }
        });
        return rootView;
    }

    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onArticleFragmentBackInteraction();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnArticleFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void sendEmail() {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/html");
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mTitle + " sent from Headlinr for Android");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mUrl +"\nSent from Headlinr for Android");
        getActivity().startActivity(Intent.createChooser(emailIntent, "Sending email . . ."));
    }

    private void sendSMS() {
        Activity activity = getActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(activity); //Need to change the build to API 19

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, mUrl + "\nSent from Headlinr for Android");
            //Can be null in case that there is no default, then the user would be able to choose any app that support this intent.
            if (defaultSmsPackageName != null) {
                sendIntent.setPackage(defaultSmsPackageName);
            }
            activity.startActivity(sendIntent);

        }
        //For early versions, do what worked for you before.
        else {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:"));
            sendIntent.putExtra("sms_body", mUrl + "\nSent from Headlinr for Android");
            activity.startActivity(sendIntent);
        }
    }

    /**
     * Custom OnTouchListener for article touch
     */
    private class ArticleTouchListener implements View.OnTouchListener {
        private float lastY;
        private float diff;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                lastY = event.getY();
                return false;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                diff = (event.getY() - lastY);

                if ((diff > 200  || diff < -200) && mIsListeningToScroll) {
                    EasingManager manager = new EasingManager(new EasingManager.EasingCallback() {

                        @Override
                        public void onEasingValueChanged(double value, double oldValue) {
                            if((diff < -200 && mIsCustomActionBarVisible) || (diff > 200 && !mIsCustomActionBarVisible))
                                mCustomActionBar.setTranslationY(-(float) value);
                        }

                        @Override
                        public void onEasingStarted(double value) {
                            mIsListeningToScroll = false;
                        }

                        @Override
                        public void onEasingFinished(double value) {
                            if (diff < -200) {
                                mCustomActionBar.setTranslationY(-140.0f);
                                mIsCustomActionBarVisible = false;
                            } else {
                                mCustomActionBar.setTranslationY(0.0f);
                                mIsCustomActionBarVisible = true;
                            }

                            mIsListeningToScroll = true;
                        }
                    });

                    if (mIsCustomActionBarVisible)
                        manager.start(Cubic.class, EasingManager.EaseType.EaseOut, 0, 100, 200);
                    else
                        manager.start(Cubic.class, EasingManager.EaseType.EaseIn, 100, 0, 200);
                }

                return false;
            }

           return false;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment
     */
    public interface OnArticleFragmentInteractionListener {
        public void onArticleFragmentBackInteraction();
        public void onFacebookShareInteraction(String url);
        public void onTwitterShareInteraction(String title, String url);
    }

}
