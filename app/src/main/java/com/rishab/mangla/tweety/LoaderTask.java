package com.rishab.mangla.tweety;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Hashtable;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by rishabmangla on 9/7/15.
 */
//AsyncTask to load all the tweets
//adapter is updated onProgressUpdate
public class LoaderTask extends AsyncTask<String, twitter4j.Status, Void> {

    TweetsAdapter mAdapter;
    public Hashtable<String, twitter4j.Status> mTweetsList = new Hashtable<String, twitter4j.Status>();
    private SharedPreferences tweetPrefs;
    TextView loading;
    /**twitter object*/
    private Twitter timelineTwitter;
    private String LOG_TAG = "LoaderTask";

    public LoaderTask(Application application, TextView loading, TweetsAdapter adapter) {
        mAdapter = adapter;
        this.loading = loading;
        tweetPrefs = application.getSharedPreferences(TweetLineActivity.TWEET_PREF, 0);
        Log.i("rishab","LoaderTask ");
    }

    @Override
    protected Void doInBackground(String[] params) {
        String userToken = tweetPrefs.getString("user_token", null);
        String userSecret = tweetPrefs.getString("user_secret", null);

        Configuration twitConf = new ConfigurationBuilder()
                .setOAuthConsumerKey(TweetLineActivity.TWIT_KEY)
                .setOAuthConsumerSecret(TweetLineActivity.TWIT_SECRET)
                .setOAuthAccessToken(userToken)
                .setOAuthAccessTokenSecret(userSecret)
                .build();
        //instantiate new twitter
        timelineTwitter = new TwitterFactory(twitConf).getInstance();

        try
        {
            //retrieve the new home tweetline tweets as a list
            List<twitter4j.Status> homeTimeline = timelineTwitter.getHomeTimeline();
            Log.i("rishab","homeTimeline.size() "  + homeTimeline.size());
            //iterate through new status updates
            for (twitter4j.Status statusUpdate : homeTimeline)
            {
//                Log.i("rishab","tweetsLoader " + statusUpdate.getUser());
                publishProgress(statusUpdate);
            }
        }
        catch (Exception te) {
            Log.e(LOG_TAG, "Exception: " + te);
        }
        return null;
    }

    //add the tweets to the list adapter
    @Override
    protected void onProgressUpdate(twitter4j.Status... values) {
        loading.setVisibility(View.GONE);
        twitter4j.Status status = values[0];
        mAdapter.add((twitter4j.Status) status);
        mTweetsList.put(Long.toString(status.getId()), status);
        super.onProgressUpdate(values);
    }
}
