package com.leif.chatchat.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.SMSProvider;
import com.leif.chatchat.ui.view.RoundedImageView;
import com.leif.chatchat.util.CompressImageUtil;
import com.leif.chatchat.util.PhotoUtil;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by leif on 6/16/16.
 */
public class SessionsAdapter extends SimpleCursorAdapter {

    private SharedPreferences preferences;

    public SessionsAdapter() {
        super(
                IM.im,
                R.layout.fragment_session_item,
                IM.im.getContentResolver().query(SMSProvider.SMS_SESSIONS_URI, null, null, null, null),
                new String[]{SMSProvider.SMSColumns.CLOCK},
                new int[]{R.id.fragment_sessions_item_time},
                FLAG_REGISTER_CONTENT_OBSERVER);

    }

    public void bindView(View view, Context context, Cursor cursor) {
        preferences = PreferenceManager.getDefaultSharedPreferences(IM.im.getApplicationContext());

        RoundedImageView avatar = (RoundedImageView) view.findViewById(R.id.fragment_sessions_item_avatar);
        TextView session_name = (TextView) view.findViewById(R.id.fragment_sessions_item_name);
        TextView sms_count = (TextView) view.findViewById(R.id.fragment_sessions_item_count);
        TextView content = (TextView) view.findViewById(R.id.fragment_sessions_item_content);

        String session_id = cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.SESSION_ID));
        session_name.setText(preferences.getString("sessionName_" + session_id, session_id));
        if (cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.TYPE)).equals("PHOTO")) {
            content.setText("图片");
        } else {
            content.setText(cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.CONTENT)));
        }

        int now_count = preferences.getInt("count_" + session_id, 0);

        if (now_count > 0) {
            sms_count.setText(String.valueOf(now_count));
            sms_count.setVisibility(View.VISIBLE);
        } else {
            sms_count.setVisibility(View.INVISIBLE);
        }

        try {
            File file = new File(Environment.getExternalStorageDirectory(), preferences.getString("otherAvatar_" + session_id, null));
            if (file.exists()) {
                Uri avatarUri = Uri.fromFile(file);
                byte b[] = CompressImageUtil.getimage(avatarUri.getPath(), 256, 256);
                Bitmap bitmap = PhotoUtil.Bytes2Bimap(b);
                avatar.setImageBitmap(bitmap);
            } else {
                avatar.setImageResource(R.drawable.ic_launcher);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            avatar.setImageResource(R.drawable.ic_launcher);
        }
        super.bindView(view, context, cursor);
    }
}