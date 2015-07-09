package com.rishab.mangla.tweety;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import twitter4j.Status;

/**
 * Created by rishabmangla on 9/7/15.
 */
public class TweetsAdapter extends ArrayAdapter<twitter4j.Status> {

    // List context
    private final Context context;

    public TweetsAdapter(Context context) {
        super(context, R.layout.tweet_list_item);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        Status tweet;
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(R.layout.tweet_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.senderInfo = (TextView) convertView.findViewById(R.id.phoneNum);
            viewHolder.body = (TextView) convertView.findViewById(R.id.body);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        tweet = getItem(position);
        String displayName = tweet.getUser().getName();

        viewHolder.senderInfo.setText(displayName);
        viewHolder.body.setText(tweet.getText());

        return convertView;
    }

}

class ViewHolder {
    TextView senderInfo;
    TextView body;
}