package org.redplatoon.headlinr.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MoreFragment.OnMoreFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class MoreFragment extends Fragment implements View.OnClickListener {

    private OnMoreFragmentInteractionListener mListener;
    private ImageView mBack;
    private TextView mRateMe;
    private SharedPreferences mSettings;
    private Drawable mOriginalBg;
    private int mFilteredViewId;
    private boolean mShouldSetUpCategories = false;
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment MoreFragment.
     */
    public static MoreFragment newInstance() {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //fragment.setArguments(args);
        return fragment;
    }
    public MoreFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (getArguments() != null) {
          //  mParam1 = getArguments().getString(ARG_PARAM1);
           // mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.more, container, false);
        mRateMe = (TextView) rootView.findViewById(R.id.rate_me);
        mRateMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.redplatoon.headlinr.app")));//getActivity().getPackageName())));
            }
        });

        mBack = (ImageView) rootView.findViewById(R.id.more);
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonPressed();
            }
        });
        String filteredViewName = mSettings.getString("filter_view",null);
        if(filteredViewName != null) {
            TextView filteredView = getFilteredView(rootView, filteredViewName);
            mOriginalBg = filteredView.getBackground();
            mFilteredViewId = filteredView.getId();
            filteredView.setBackgroundColor(getActivity().getResources().getColor(R.color.clicked));
        }

        rootView.findViewById(R.id.us_news).setOnClickListener(this);
        rootView.findViewById(R.id.world_news).setOnClickListener(this);
        rootView.findViewById(R.id.top_news).setOnClickListener(this);
        rootView.findViewById(R.id.columnists).setOnClickListener(this);
        rootView.findViewById(R.id.offbeat).setOnClickListener(this);
        rootView.findViewById(R.id.general_news).setOnClickListener(this);
        rootView.findViewById(R.id.sports).setOnClickListener(this);
        rootView.findViewById(R.id.travel).setOnClickListener(this);
        rootView.findViewById(R.id.hobbies).setOnClickListener(this);
        rootView.findViewById(R.id.music).setOnClickListener(this);
        rootView.findViewById(R.id.sci_tech).setOnClickListener(this);
        rootView.findViewById(R.id.blogs).setOnClickListener(this);
        rootView.findViewById(R.id.lifestyle).setOnClickListener(this);
        rootView.findViewById(R.id.art).setOnClickListener(this);
        rootView.findViewById(R.id.health).setOnClickListener(this);
        rootView.findViewById(R.id.business).setOnClickListener(this);
        rootView.findViewById(R.id.politics).setOnClickListener(this);
        rootView.findViewById(R.id.videogames).setOnClickListener(this);
        rootView.findViewById(R.id.uni_news).setOnClickListener(this);

        return rootView;
    }

    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onMoreFragmentBackInteraction(mShouldSetUpCategories);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnMoreFragmentInteractionListener) activity;
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

    @Override
    public void onClick(View v) {
        //String filteredView = mSettings.getString("filter_view","");
        if(mFilteredViewId == 0) {
            SharedPreferences.Editor editor = mSettings.edit();
            ArrayList<Integer> filters = new ArrayList<Integer>();
            switch (v.getId()) {
                case R.id.us_news:
                    filters.add(7);
                    editor.putString("filter_view", "us_news");
                    break;
                case R.id.world_news:
                    filters.add(19);
                    editor.putString("filter_view", "world_news");
                    break;
                case R.id.top_news:
                    filters.add(26);
                    editor.putString("filter_view", "top_news");
                    break;
                case R.id.columnists:
                    filters.add(588);
                    editor.putString("filter_view", "columnists");
                    break;
                case R.id.offbeat:
                    filters.add(36);
                    editor.putString("filter_view", "offbeat");
                    break;
                case R.id.general_news:
                    filters.add(1168);
                    editor.putString("filter_view", "general_news");
                    break;
                case R.id.sports:
                    filters.add(1314);
                    filters.add(27);
                    editor.putString("filter_view", "sports");
                    break;
                case R.id.travel:
                    filters.add(23);
                    editor.putString("filter_view", "travel");
                    break;
                case R.id.hobbies:
                    filters.add(14);
                    editor.putString("filter_view", "hobbies");
                    break;
                case R.id.music:
                    filters.add(29);
                    editor.putString("filter_view", "music");
                    break;
                case R.id.sci_tech:
                    filters.add(30);
                    filters.add(16);
                    filters.add(8);
                    filters.add(15);
                    filters.add(28);
                    filters.add(10);
                    editor.putString("filter_view", "sci_tech");
                    break;
                case R.id.blogs:
                    filters.add(21);
                    filters.add(31);
                    editor.putString("filter_view", "blogs");
                    break;
                case R.id.lifestyle:
                    filters.add(5);
                    filters.add(6);
                    filters.add(17);
                    filters.add(25);
                    filters.add(20);
                    filters.add(34);
                    filters.add(4);
                    editor.putString("filter_view", "lifestyle");
                    break;
                case R.id.art:
                    filters.add(13);
                    editor.putString("filter_view", "art");
                    break;
                case R.id.health:
                    filters.add(11);
                    editor.putString("filter_view", "health");
                    break;
                case R.id.business:
                    filters.add(2);
                    filters.add(22);
                    editor.putString("filter_view", "business");
                    break;
                case R.id.politics:
                    filters.add(591);
                    filters.add(3);
                    editor.putString("filter_view", "politics");
                    break;
                case R.id.videogames:
                    filters.add(9);
                    editor.putString("filter_view", "videogames");
                    break;
                case R.id.uni_news:
                    filters.add(12);
                    editor.putString("filter_view", "uni_news");
                    break;
                default:
                    break;
            }
            mFilteredViewId = v.getId();
            editor.commit();

            mOriginalBg = v.getBackground();
            v.setBackgroundColor(getActivity().getResources().getColor(R.color.clicked));
            if (mListener != null) {
                mListener.onMoreFragmentFilterSelection(filters);
            }
        } else if(v.getId() == mFilteredViewId) {
            if(mOriginalBg != null)
                v.setBackgroundDrawable(mOriginalBg);
            SharedPreferences.Editor editor = mSettings.edit();
            editor.remove("filter_view");
            editor.commit();
            mFilteredViewId = 0;
            if (mListener != null) {
                mListener.onMoreFragmentFilterSelection(new ArrayList<Integer>());
            }
            mShouldSetUpCategories = true;
        } else {
            Toast.makeText(getActivity(), "Only one filter can be active.. :-/", Toast.LENGTH_LONG).show();
        }
    }

    private TextView getFilteredView(View vg, String filteredViewName) {
                if(filteredViewName.equals("us_news"))
                    return (TextView) vg.findViewById(R.id.us_news);
                else if(filteredViewName.equals("world_news"))
                    return (TextView) vg.findViewById(R.id.world_news);
                else if(filteredViewName.equals("top_news"))
                    return (TextView) vg.findViewById(R.id.top_news);
                else if(filteredViewName.equals("columnists"))
                    return (TextView) vg.findViewById(R.id.columnists);
                else if(filteredViewName.equals("offbeat"))
                    return (TextView) vg.findViewById(R.id.offbeat);
                else if(filteredViewName.equals("general_news"))
                    return (TextView) vg.findViewById(R.id.general_news);
                else if(filteredViewName.equals("sports"))
                    return (TextView) vg.findViewById(R.id.sports);
                else if(filteredViewName.equals("travel"))
                    return (TextView) vg.findViewById(R.id.travel);
                else if(filteredViewName.equals("hobbies"))
                    return (TextView) vg.findViewById(R.id.hobbies);
                else if(filteredViewName.equals("music"))
                    return (TextView) vg.findViewById(R.id.music);
                else if(filteredViewName.equals("sci_tech"))
                    return (TextView) vg.findViewById(R.id.sci_tech);
                else if(filteredViewName.equals("blogs"))
                    return (TextView) vg.findViewById(R.id.blogs);
                else if(filteredViewName.equals("lifestyle"))
                    return (TextView) vg.findViewById(R.id.lifestyle);
                else if(filteredViewName.equals("art"))
                    return (TextView) vg.findViewById(R.id.art);
                else if(filteredViewName.equals("health"))
                    return (TextView) vg.findViewById(R.id.health);
                else if(filteredViewName.equals("business"))
                    return (TextView) vg.findViewById(R.id.business);
                else if(filteredViewName.equals("politics"))
                    return (TextView) vg.findViewById(R.id.politics);
                else if(filteredViewName.equals("videogames"))
                    return (TextView) vg.findViewById(R.id.videogames);
                else
                    return (TextView) vg.findViewById(R.id.uni_news);

    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnMoreFragmentInteractionListener {
        public void onMoreFragmentFilterSelection(ArrayList<Integer> filters);
        public void onMoreFragmentBackInteraction(boolean shouldSetUpCategories);
    }

}
