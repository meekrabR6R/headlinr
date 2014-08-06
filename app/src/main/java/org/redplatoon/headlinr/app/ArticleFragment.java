package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import org.redplatoon.headlinr.app.custom.ArticleWebView;

import it.sephiroth.android.library.easing.EasingManager;
import it.sephiroth.android.library.easing.Sine;


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
    private String mUrl;
    private OnArticleFragmentInteractionListener mListener;
    private ImageView mBack;
    private LinearLayout mCustomActionBar;
    private int mScrollPosition = 0;
    private boolean mIsCustomActionBarVisible = true;
    private boolean mIsListeningToScroll = true;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment ArticleFragment.
     */
    public static ArticleFragment newInstance(String url) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
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

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed();
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
                            mCustomActionBar.setTranslationY(-(float) value);
                        }

                        @Override
                        public void onEasingStarted(double value) {
                            mIsListeningToScroll = false;
                            if (diff < -200) {
                                mCustomActionBar.setTranslationY(-1000.0f);
                            } else {
                                mCustomActionBar.setTranslationY(0.0f);
                            }

                        }

                        @Override
                        public void onEasingFinished(double value) {
                            if (diff < -200) {
                                mCustomActionBar.setTranslationY(-1000.0f);
                                mIsCustomActionBarVisible = false;
                            } else {
                                mCustomActionBar.setTranslationY(0.0f);
                                mIsCustomActionBarVisible = true;
                            }

                            mIsListeningToScroll = true;
                        }
                    });

                    // start the easing from 0 to 100
                    // using Cubic easeOut
                    // and a duration of 500ms
                    if (mIsCustomActionBarVisible)
                        manager.start(Sine.class, EasingManager.EaseType.EaseOut, 0, 1000, 250);
                    else
                        manager.start(Sine.class, EasingManager.EaseType.EaseIn, 1000, 0, 250);
                }

                return false;
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {

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
    }

}
