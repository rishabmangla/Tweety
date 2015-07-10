package com.rishab.mangla.tweety;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Hashtable;

//  Main Activity class for the application
//  user can sign in to twitter
//  it shows all the tweets
//  this application uses twitter4j library
//  Click option menu button to update photo status
public class TweetLineActivity extends Activity {

    //my acc key for the app
    public final static String TWIT_KEY = "xLqZkGRHsF9OzyLMUxRhIa2ib";
    //my secret key for the app
    public final static String TWIT_SECRET = "SbuUXx5XNMMSSMbb9XLPLi5CQqdCYPsjLozijIngD2JtS9G6zC";
    //app_url
    public final static String TWIT_URL = "tweety-bobtask:///";
    //shared pref
    public final static String TWEET_PREF = "TWEET_PREF";
    //state
    public static boolean isLogin = false;

    private String LOG_TAG = "TweetLineActivity";

    //twitter instance
    private Twitter mTwitter;
    //request token for accessing user account
    private RequestToken mRequestToken;
    //shared preferences to store user details
    private SharedPreferences tweetPrefs;
    //list view for all the tweets
    private ListView homeTimeline;

    private TweetsAdapter mAdapter;

    public Hashtable<String, Status> mTweetsList = new Hashtable<String, Status>();

    //broadcast receiver for updating the tweets list
    private BroadcastReceiver mStatusReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //get the preferences
        tweetPrefs = getSharedPreferences(TWEET_PREF, 0);
        //find out if the user preferences are set
        if(tweetPrefs.getString("user_token", null)==null) {
            //no user preferences so prompt to sign in
            setContentView(R.layout.main);

            if(isNetworkConnected()) {
                //authentication
                SetupConnection connect = new SetupConnection();
                connect.execute();
            }else {
                Toast.makeText(this, "Network not connected", Toast.LENGTH_LONG).show();
                finish();
            }
            Log.i("rishab", "onCreate ");

            //setup button for click listener
            Button signIn = (Button)findViewById(R.id.signin);
            signIn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //take user to twitter authentication web page to allow app access to their twitter account
                    String authURL = mRequestToken.getAuthenticationURL();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authURL)));
                }
            });
        }
        else
        {
            //user preferences are set - get tweetline
            listHomeTweets();
        }

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

    //AsyncTask to setup the initial connection and get the auth request
    class SetupConnection extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i("rishab", "SetupConnection pichwaade me karr ");

            mTwitter = new TwitterFactory().getInstance();
            Log.i("rishab", "setOAuthConsumer 1");
            //pass developer key and secret
            mTwitter.setOAuthConsumer(TWIT_KEY, TWIT_SECRET);
            Log.i("rishab","setOAuthConsumer 2");
            //try to get request token
            try
            {
                //get authentication request token
                mRequestToken = mTwitter.getOAuthRequestToken(TWIT_URL);
                Log.i("mangla","after try");
            }
            catch(TwitterException te) {
                Log.e(LOG_TAG, "TE " + te.getMessage());
                Log.i("mangla", "catch " + te.getMessage());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean check) {
            super.onPostExecute(check);
            if(!check) {
                Toast.makeText(TweetLineActivity.this, "Network not connected", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    //onNewIntent fires when user returns from Twitter authentication Web page
    @Override
    protected void onNewIntent(Intent intent) {
        Log.i("rishab","onNewIntent " + intent.toString());
        super.onNewIntent(intent);
        //get the retrieved data
        Uri twitURI = intent.getData();
        //make sure the url is correct
        if(twitURI!=null && twitURI.toString().startsWith(TWIT_URL))
        {
            //is verification - get the returned data
            String oaVerifier = twitURI.getQueryParameter("oauth_verifier");

            //attempt to retrieve access token

            RetrieveAccessToken retrieve = new RetrieveAccessToken();
            retrieve.execute(oaVerifier);
        }
    }

    //AsyncTask to getOAuthAccessToken
    class RetrieveAccessToken extends AsyncTask<String, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(String... oaVerifier) {
            try {
                //try to get an access token using the returned data from the verification page
                AccessToken accToken = mTwitter.getOAuthAccessToken(mRequestToken, oaVerifier[0]);
                //add the token and secret to shared prefs for future reference
                return accToken;
            }catch (TwitterException te)
            {
                Log.e(LOG_TAG, "Failed to get access token: "+te.getMessage());
                return null;
            }

        }

        @Override
        protected void onPostExecute(AccessToken accToken) {
            if(accToken != null) {
                Log.i("rishab","onPostExecute, accToken != null");
                tweetPrefs.edit()
                        .putString("user_token", accToken.getToken())
                        .putString("user_secret", accToken.getTokenSecret())
                        .commit();
                //display the tweetline
                listHomeTweets();
            }else {
                Toast.makeText(TweetLineActivity.this, "Failed to get access token: Network not connected", Toast.LENGTH_LONG).show();
                finish();
            }
            super.onPostExecute(accToken);
        }
    }

    //this displays the user's main home Twitter tweetline
    private void listHomeTweets() {
        isLogin = true;
        //set the layout
        setContentView(R.layout.tweetline);
        //get reference to the list view
        homeTimeline = (ListView)findViewById(R.id.homeList);
        mAdapter = new TweetsAdapter(this);
        homeTimeline.setAdapter(mAdapter);
        //retrieve the tweetline
        LoaderTask retrieve = new LoaderTask(getApplication(), (TextView) findViewById(R.id.loading), mAdapter);
        retrieve.execute();
        //instantiate receiver class for finding out when new updates are available
        mStatusReceiver = new TwitterUpdateReceiver();
        //register for updates
        registerReceiver(mStatusReceiver, new IntentFilter("TWEETY_UPDATES"));
    }

     //broadcast receiver for new updates
    class TwitterUpdateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("rishab","onReceive ");
            Status status = (Status) intent.getSerializableExtra("MESSAGE");
            Log.i("rishab", "onReceive 2 " + getIntent().getSerializableExtra("MESSAGE"));
            if(status != null) {
                mAdapter.insert(status, 0);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_tweet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("rishab", "onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.update_tweet) {
            if(isLogin)
                //launch tweet activity
                startActivity(new Intent(this, TweetThePicha.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * When the class is destroyed, close database and service classes
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        try
        {
            //remove receiver register
            unregisterReceiver(mStatusReceiver);
        }
        catch(Exception se) { Log.e(LOG_TAG, "unable to stop service or receiver"); }
    }
}