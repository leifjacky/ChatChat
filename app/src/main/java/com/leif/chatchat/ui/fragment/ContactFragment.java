package com.leif.chatchat.ui.fragment;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.NewFriendProvider;
import com.leif.chatchat.provider.SMSProvider;
import com.leif.chatchat.ui.ChatActivity;
import com.leif.chatchat.ui.ImageActivity;
import com.leif.chatchat.ui.NewFriendActivity;
import com.leif.chatchat.ui.adapter.ContactsAdapter;
import com.leif.chatchat.ui.adapter.ContactsAdapter.Item;
import com.leif.chatchat.ui.adapter.NewFriendAdapter;
import com.leif.chatchat.util.PhotoUtil;
import com.leif.chatchat.util.StringUtils;

public class ContactFragment extends ListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private TextView NewFriendCount;

    private ContactsAdapter adapter;
    private ContentObserver co, coNewFriend;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        NewFriendCount = (TextView) view.findViewById(R.id.fragment_new_friend_count);
        view.findViewById(R.id.fragment_contacts_list_btn_new).setOnClickListener(this);
        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new ContactsAdapter();
        adapter.setOnItemViewClickListener(this);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);

        Cursor cursor = getActivity().getContentResolver().query(NewFriendProvider.NEW_FRIEND_URI, null, null, null, null);
        cursor.moveToFirst();
        int count = cursor.getCount();
        if (count == 0){
            NewFriendCount.setVisibility(View.GONE);
        } else {
            NewFriendCount.setText(String.valueOf(count));
            NewFriendCount.setVisibility(View.VISIBLE);
        }

        co = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                adapter = new ContactsAdapter();
                adapter.setOnItemViewClickListener(ContactFragment.this);
                getListView().setAdapter(adapter);
            }
        };

        coNewFriend = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                Cursor cursor = getActivity().getContentResolver().query(NewFriendProvider.NEW_FRIEND_URI, null, null, null, null);
                cursor.moveToFirst();
                int count = cursor.getCount();
                if (count == 0){
                    NewFriendCount.setVisibility(View.GONE);
                } else {
                    NewFriendCount.setText(String.valueOf(count));
                    NewFriendCount.setVisibility(View.VISIBLE);
                }
            }
        };

        getActivity().getContentResolver().registerContentObserver(ContactsProvider.CONTACT_URI, true, co);
        getActivity().getContentResolver().registerContentObserver(NewFriendProvider.NEW_FRIEND_URI, true, coNewFriend);
    }

    public void onDestroy() {
        super.onDestroy();
        getActivity().getContentResolver().unregisterContentObserver(co);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fragment_contacts_list_btn_new:
                getActivity().startActivity(new Intent(getActivity(), NewFriendActivity.class));
                break;

            case (R.id.fragment_contacts_list_item_avatar):
                if (view.getTag() != null) {
                    startActivity(new Intent(getActivity(), ImageActivity.class).putExtra("filepath", view.getTag().toString()));
                }
                break;

            default:
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Item item = adapter.getItem(position);
        if (item.type == Item.ITEM) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_SESSION_ID, StringUtils.getSessionID(IM.account, item.account));
            intent.putExtra(ChatActivity.EXTRA_OTHER_ACCOUNT, item.account);
            startActivity(intent);
        }
    }
}
