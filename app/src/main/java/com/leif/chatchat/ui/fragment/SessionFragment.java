package com.leif.chatchat.ui.fragment;


import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.SMSProvider;
import com.leif.chatchat.ui.ChatActivity;
import com.leif.chatchat.ui.adapter.SessionsAdapter;

public class SessionFragment extends ListFragment implements AdapterView.OnItemClickListener {
    private SessionsAdapter adapter;
    private ContentObserver co;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_session, container, false);
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new SessionsAdapter();

        getListView().setAdapter(adapter);
        getListView().setOnItemClickListener(this);

        co = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                Cursor cursor = getActivity().getContentResolver().query(SMSProvider.SMS_SESSIONS_URI, null, null, null, null);
                adapter.changeCursor(cursor);
            }
        };

        getActivity().getContentResolver().registerContentObserver(SMSProvider.SMS_URI, true, co);
        getActivity().getContentResolver().registerContentObserver(ContactsProvider.CONTACT_URI, true, co);
    }

    public void onDestroy() {
        getActivity().getContentResolver().unregisterContentObserver(co);
        super.onDestroy();
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Cursor cursor = (Cursor) adapter.getItem(i);
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_SESSION_ID, cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.SESSION_ID)));
        intent.putExtra(ChatActivity.EXTRA_OTHER_ACCOUNT, cursor.getString(cursor.getColumnIndex(SMSProvider.SMSColumns.OTHER_ACCOUNT)));
        startActivity(intent);
    }
}
