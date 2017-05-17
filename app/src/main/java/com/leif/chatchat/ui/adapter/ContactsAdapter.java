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
import com.leif.chatchat.ui.view.PinnedSectionListView;
import com.leif.chatchat.ui.view.RoundedImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends BaseAdapter implements PinnedSectionListView.PinnedSectionListAdapter {
    private View.OnClickListener clickListener;
    private List<Item> items;

    public ContactsAdapter() {
        Cursor group = IM.im.getContentResolver().query(ContactsProvider.CONTACT_GROUP_URI, null, null, null, ContactsProvider.ContactColumns.SECTION);
        if (group != null && group.getCount() > 0) {
            items = new ArrayList<Item>();
            for (int i = 0; i < group.getCount(); i++) {
                int sectionPosition = 0, listPosition = 0;
                group.moveToPosition(i);
                String index = group.getString(group.getColumnIndex(ContactsProvider.ContactColumns.SECTION));
                Item section = new Item(Item.SECTION, index, null, null);
                section.listPosition = listPosition++;
                items.add(section);
                Cursor entry = IM.im.getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.SECTION + " = ?", new String[]{index}, ContactsProvider.ContactColumns.SORT);
                if (entry != null && entry.getCount() > 0) {
                    for (int j = 0; j < entry.getCount(); j++) {
                        entry.moveToPosition(j);
                        String name = entry.getString(entry.getColumnIndex(ContactsProvider.ContactColumns.NAME));
                        String account = entry.getString(entry.getColumnIndex(ContactsProvider.ContactColumns.ACCOUNT));
                        String sort = entry.getString(entry.getColumnIndex(ContactsProvider.ContactColumns.SORT));
                        String avatar = entry.getString(entry.getColumnIndex(ContactsProvider.ContactColumns.AVATAR));

                        Item item = new Item(Item.ITEM, account, name, avatar);
                        item.sectionPosition = sectionPosition;
                        item.listPosition = listPosition++;

                        items.add(item);
                    }
                }
                entry.close();
                sectionPosition++;
            }
        }
    }

    public boolean isItemViewTypePinned(int viewType) {
        return viewType == Item.SECTION;
    }

    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    public int getViewTypeCount() {
        return 2;
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
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.fragment_contacts_list_item, null);
            holder.avatar = (ImageView) view.findViewById(R.id.fragment_contacts_list_item_avatar);
            holder.name = (TextView) view.findViewById(R.id.fragment_contacts_list_item_name);
            holder.title = (TextView) view.findViewById(R.id.fragment_contacts_list_item_title);
            holder.layout = view.findViewById(R.id.fragment_contacts_list_item_layout);
            holder.titleLayout = view.findViewById(R.id.fragment_contact_list_item_titleLayout);

            holder.avatar.setOnClickListener(clickListener);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Item item = getItem(position);
        if (item.type == Item.SECTION){
            holder.layout.setVisibility(View.GONE);
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(item.account);
            holder.titleLayout.setVisibility(View.VISIBLE);
        } else {
            holder.layout.setVisibility(View.VISIBLE);
            holder.title.setVisibility(View.GONE);
            holder.name.setText(item.name);
            holder.titleLayout.setVisibility(View.GONE);

            try {
                File avatarFile = new File(Environment.getExternalStorageDirectory(), "/" + item.avatar);
                if (avatarFile.exists()){
                    Uri avatarUri = Uri.fromFile(avatarFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(IM.im.getContentResolver().openInputStream(avatarUri));
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(null, bitmap);
                    holder.avatar.setImageDrawable(bitmapDrawable);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            holder.avatar.setTag(item.avatar);
        }
        return view;
    }

    public static class Item {
        public static final int ITEM = 0;
        public static final int SECTION = 1;

        public int type;
        public String account, name, avatar;
        public int sectionPosition;

        public int listPosition;

        public Item(int type, String account, String name, String avatar) {
            this.type = type;
            this.account = account;
            this.name = name;
            this.avatar = avatar;
        }

        public String toString() {
            return account;
        }

    }

    private static class ViewHolder {
        TextView name, title;
        ImageView avatar;
        View layout;
        View titleLayout;
    }

    public void setOnItemViewClickListener(View.OnClickListener clickListener){
        this.clickListener = clickListener;
    }
}
