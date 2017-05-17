package com.leif.chatchat.ui.adapter;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.ShareProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ShareAdapter extends BaseAdapter {
    private View.OnClickListener clickListener;
    private List<Item> items;

    public ShareAdapter() {
        items = new ArrayList<Item>();
        int listPosition = 0;
        Cursor entry = IM.im.getContentResolver().query(ShareProvider.SHARE_URI, null, null, null, ShareProvider.ShareColumns.DATE + " DESC" + " LIMIT 15");
        if (entry != null && entry.getCount() > 0) {
            for (int j = 0; j < entry.getCount(); j++) {
                entry.moveToPosition(j);
                String account = entry.getString(entry.getColumnIndex(ShareProvider.ShareColumns.ACCOUNT));
                String photo = entry.getString(entry.getColumnIndex(ShareProvider.ShareColumns.PHOTO));
                String date = entry.getString(entry.getColumnIndex(ShareProvider.ShareColumns.DATE));
                String content = entry.getString(entry.getColumnIndex(ShareProvider.ShareColumns.CONTENT));

                Cursor cursor = IM.im.getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{account}, null);
                cursor.moveToFirst();
                String name = cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.NAME));
                String avatar = cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.AVATAR));

                Item item = new Item(account, name, avatar, content, photo, date);
                item.listPosition = listPosition++;

                items.add(item);
            }
        }
    }


    public int getCount() {
        return (items == null) ? 0 : items.size();
    }

    public Item getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int i) {
        return 0;
    }

    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_share_list_item, null);
            holder.photo = (ImageView) view.findViewById(R.id.fragment_share_list_item_photo);
            holder.name = (TextView) view.findViewById(R.id.fragment_share_list_item_name);
            holder.date = (TextView) view.findViewById(R.id.fragment_share_list_item_date);
            holder.content = (TextView) view.findViewById(R.id.fragment_share_list_item_content);
            holder.avatar = (ImageView) view.findViewById(R.id.fragment_share_list_item_avatar);
            holder.layout = view.findViewById(R.id.fragment_share_list_item_layout);

            holder.avatar.setOnClickListener(clickListener);
            holder.photo.setOnClickListener(clickListener);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Item item = getItem(position);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date nowDate = new Date();
            Date shareDate = dateFormat.parse(item.date);
            long distance = nowDate.getTime() - shareDate.getTime();
            if (distance < 1000 * 60) {
                holder.date.setText("刚刚");
            } else if (distance < 1000 * 60 * 60) {
                holder.date.setText(distance / 1000 / 60 + "分钟前");
            } else if (distance < 1000 * 60 * 60 * 24) {
                holder.date.setText(distance / 1000 / 60 / 60 + "小时前");
            } else if (distance < 1000 * 60 * 60 * 24 * 3) {
                holder.date.setText(distance / 1000 / 60 / 24 + "天前");
            } else {
                holder.date.setText(item.date.substring(5, item.date.lastIndexOf('.')));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            holder.date.setText(item.date.substring(5, item.date.lastIndexOf('.')));
        }

        holder.layout.setVisibility(View.VISIBLE);
        holder.name.setText(item.name);
        holder.content.setText(item.content);

        try {
            File avatarFile = new File(Environment.getExternalStorageDirectory(), item.avatar);
            if (avatarFile.exists()) {
                Uri avatarUri = Uri.fromFile(avatarFile);
                Bitmap bitmap = BitmapFactory.decodeStream(IM.im.getContentResolver().openInputStream(avatarUri));
                BitmapDrawable bitmapDrawable = new BitmapDrawable(null, bitmap);
                holder.avatar.setImageDrawable(bitmapDrawable);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        holder.avatar.setTag(item.avatar);

        if (item.photo.equals("/null")) {
            holder.photo.setVisibility(View.GONE);
        } else {
            try {
                File photoFile = new File(Environment.getExternalStorageDirectory(), item.photo);
                if (photoFile.exists()) {
                    Uri photoUri = Uri.fromFile(photoFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(IM.im.getContentResolver().openInputStream(photoUri));
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(null, bitmap);
                    holder.photo.setImageDrawable(bitmapDrawable);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                holder.photo.setVisibility(View.GONE);
            }
        }
        holder.photo.setTag(item.photo);

        return view;
    }

    public static class Item {

        public final String account, name, avatar, photo, date, content;
        public int listPosition;

        public Item(String account, String name, String avatar, String content, String photo, String date) {
            this.account = account;
            this.name = name;
            this.photo = photo;
            this.date = date;
            this.content = content;
            this.avatar = avatar;
        }

        public String toString() {
            return account;
        }
    }

    private static class ViewHolder {
        TextView name, date, content;
        ImageView photo, avatar;
        View layout;
    }

    public void setOnItemViewClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }
}
