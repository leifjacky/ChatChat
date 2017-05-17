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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.NewFriendProvider;
import com.leif.chatchat.provider.ShareProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class NewFriendAdapter extends BaseAdapter {
    private View.OnClickListener clickListener;
    private List<Item> items;

    public NewFriendAdapter() {
        items = new ArrayList<Item>();
        int listPosition = 0;
        Cursor entry = IM.im.getContentResolver().query(NewFriendProvider.NEW_FRIEND_URI, null, null, null, null);
        if (entry != null && entry.getCount() > 0) {
            for (int j = 0; j < entry.getCount(); j++) {
                entry.moveToPosition(j);
                String name = entry.getString(entry.getColumnIndex(NewFriendProvider.NewFriendColumns.NAME));
                String account = entry.getString(entry.getColumnIndex(NewFriendProvider.NewFriendColumns.ACCOUNT));
                String avatar = entry.getString(entry.getColumnIndex(NewFriendProvider.NewFriendColumns.AVATAR));

                Item item = new Item(account, name, avatar);
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
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_new_friend_list_item, null);
            holder.name = (TextView) view.findViewById(R.id.fragment_new_friend_list_item_name);
            holder.avatar = (ImageView) view.findViewById(R.id.fragment_new_friend_list_item_avatar);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Item item = getItem(position);
        holder.name.setText(item.name);

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

        return view;
    }

    public static class Item {

        public final String account, name, avatar;
        public int listPosition;

        public Item(String account, String name, String avatar) {
            this.account = account;
            this.name = name;
            this.avatar = avatar;
        }

        public String toString() {
            return account;
        }
    }

    private static class ViewHolder {
        TextView name;
        ImageView avatar;
    }

    public void setOnItemViewClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }
}
