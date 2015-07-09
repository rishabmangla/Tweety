package com.rishab.mangla.tweety;//add package

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by rishabmangla on 9/7/15.
 */
public class TweetThePicha extends Activity  {

    private SharedPreferences tweetPrefs;
    //twitter object
    private Twitter tweetTwitter;
    private String tweetName = "";
    String userToken;
    String userSecret;
    EditText tweetTxt;
    String toTweet;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.tweet);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupTweet();
    }

    //setting up the layout and listeners
    private void setupTweet() {
        //get preferences for user twitter details
        tweetPrefs = getSharedPreferences(TweetLineActivity.TWEET_PREF, 0);

        //get user token and secret for authentication
        userToken = tweetPrefs.getString("user_token", null);
        userSecret = tweetPrefs.getString("user_secret", null);

        tweetTxt = (EditText)findViewById(R.id.tweettext);
        //set up listener for send tweet button
        Button tweetButton = (Button)findViewById(R.id.dotweet);
        tweetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String toTweet = tweetTxt.getText().toString();
                TweetStatus update = new TweetStatus();
                update.execute(toTweet);
            }
        });

    }

    //AsyncTask started on Add Image button to create a new twitter configuration using user details
    //and then image picker intent to retrieve the image
    class TweetStatus extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... text) {
            Log.i("rishab","pichwaade me karr");
            //create a new twitter configuration using user details
            toTweet = text[0];
            Configuration twitConf = new ConfigurationBuilder()
                    .setOAuthConsumerKey(TweetLineActivity.TWIT_KEY)
                    .setOAuthConsumerSecret(TweetLineActivity.TWIT_SECRET)
                    .setOAuthAccessToken(userToken)
                    .setOAuthAccessTokenSecret(userSecret)
                    .build();
            //create a twitter instance
            tweetTwitter = new TwitterFactory(twitConf).getInstance();
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            return null;
        }
    }
    private final int SELECT_PHOTO = 1;
    //	private ImageView imageView;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        Log.i("rishab","onActivityResult requestCode " + requestCode +" SELECT_PHOTO " + SELECT_PHOTO);
        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    StatusUpdate statusUpdate = new StatusUpdate(toTweet);
                                    statusUpdate.setMedia("test.jpg", imageStream);
                                    Status updatedStatus = tweetTwitter.updateStatus(statusUpdate);
                                    Intent intent = new Intent("TWEETY_UPDATES");
                                    intent.putExtra("MESSAGE", updatedStatus);
                                    Log.i("rishab", "sendBroadcast ");
                                    //dis will be received in the main tweetline class
                                    sendBroadcast(intent);
                                } catch (TwitterException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    } catch (FileNotFoundException e) {
                        Log.i("rishab","excptn 2" + e);
                        e.printStackTrace();
                    }
                    Toast.makeText(this, "Status Updated", Toast.LENGTH_SHORT).show();
                    //reset the edit text
                    tweetTxt.setText("");
                    //finish to go back to home
                    finish();
                }
        }
    }

}
