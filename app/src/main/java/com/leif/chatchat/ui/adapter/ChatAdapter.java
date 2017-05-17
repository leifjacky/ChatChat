package com.leif.chatchat.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.widget.CursorAdapter;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by leif on 6/16/16.
 */
public class ChatAdapter extends CursorAdapter {
    private final int ITEM_RIGHT = 0;
    private final int ITEM_LEFT = 1;

    View.OnClickListener onClickListener;

    public ChatAdapter(String session_id) {
        super(IM.im, IM.im.getContentResolver().query(SMSProvider.SMS_URI, null, SMSProvider.SMSColumns.SESSION_ID + " = ?", new String[]{session_id}, null), FLAG_REGISTER_CONTENT_OBSERVER);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            switch (getItemViewType(position)) {
                case ITEM_RIGHT:
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_chat_item_right, null);
                    break;

                case ITEM_LEFT:
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_chat_item_left, null);
                    break;

                default:
            }

            holder.content = (TextView) convertView.findViewById(R.id.activity_chat_item_content);
            holder.time = (TextView) convertView.findViewById(R.id.activity_chat_item_time);
            holder.time_layout = convertView.findViewById(R.id.activity_chat_item_time_layout);
            holder.avatar = (RoundedImageView) convertView.findViewById(R.id.activity_chat_item_avatar);
            holder.photo = (ImageView) convertView.findViewById(R.id.activity_chat_item_photo);
            holder.avatar.setOnClickListener(onClickListener);
            holder.photo.setOnClickListener(onClickListener);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Cursor cursor = (Cursor) getItem(position);
        String content = cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.CONTENT));
        String account = cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.FROM_ACCOUNT));
        String nowDate = cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.TIME));
        String type = cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.TYPE));

        holder.time_layout.setVisibility(View.VISIBLE);
        holder.time.setText(nowDate.substring(5, 16));
        holder.avatar.setTag("/chatchat/avatar/avatar_" + account + ".jpg");
        holder.photo.setTag(content);

        if (type.equals("PHOTO")) {
            holder.photo.setVisibility(View.VISIBLE);
            holder.content.setVisibility(View.GONE);

            File file = new File(Environment.getExternalStorageDirectory(), content);
            if (file.exists()) {
                Uri avatarUri = Uri.fromFile(file);
                byte b[] = CompressImageUtil.getimage(avatarUri.getPath(), 800, 480);
                Bitmap bitmap = PhotoUtil.Bytes2Bimap(b);
                holder.photo.setImageBitmap(bitmap);
            }
        } else {
            holder.photo.setVisibility(View.GONE);
            holder.content.setVisibility(View.VISIBLE);

            holder.content.setText(content);
        }

        if (position > 0) {
            Cursor entry = (Cursor) getItem(position - 1);
            String lastDate = entry.getString(entry.getColumnIndex(SMSProvider.SMSColumns.TIME));
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date d1 = null, d2 = null;
            try {
                d1 = dateFormat.parse(lastDate);
                d2 = dateFormat.parse(nowDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            long diff = d2.getTime() - d1.getTime();
            if (diff < (1000 * 60 * 5)) {
                holder.time_layout.setVisibility(View.GONE);
            }
        }

        File avatarFile = new File(Environment.getExternalStorageDirectory(), "/chatchat/avatar/avatar_" + account + ".jpg");
        if (avatarFile.exists()) {
                Uri avatarUri = Uri.fromFile(avatarFile);
                byte b[] = CompressImageUtil.getimage(avatarUri.getPath(), 800, 480);
                Bitmap bitmap = PhotoUtil.Bytes2Bimap(b);
                holder.avatar.setImageBitmap(bitmap);
        } else {
            holder.avatar.setImageResource(R.drawable.ic_launcher);
        }

        return convertView;
    }

    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        String account = cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.FROM_ACCOUNT));
        if (IM.account.equals(account)) {
            return ITEM_RIGHT;
        } else {
            return ITEM_LEFT;
        }
    }

    public int getViewTypeCount() {
        return 2;
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return null;
    }

    public void bindView(View view, Context context, Cursor cursor) {
    }

    private static class ViewHolder {
        TextView content, time;
        RoundedImageView avatar;
        View time_layout;
        ImageView photo;
    }

    public void setOnChatViewClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

}
