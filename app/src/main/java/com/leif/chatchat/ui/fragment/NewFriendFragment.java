package com.leif.chatchat.ui.fragment;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.NewFriendProvider;
import com.leif.chatchat.ui.ImageActivity;
import com.leif.chatchat.ui.adapter.NewFriendAdapter;
import com.leif.chatchat.ui.fragment.Dialog.NewFriendDialogFragment;

public class NewFriendFragment extends ListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {

    private NewFriendAdapter adapter;
    private ContentObserver co;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new NewFriendAdapter();
        adapter.setOnItemViewClickListener(this);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);

        co = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                adapter = new NewFriendAdapter();
                adapter.setOnItemViewClickListener(NewFriendFragment.this);
                getListView().setAdapter(adapter);
            }
        };
        getActivity().getContentResolver().registerContentObserver(NewFriendProvider.NEW_FRIEND_URI, true, co);
    }

    public void onDestroy() {
        super.onDestroy();
        getActivity().getContentResolver().unregisterContentObserver(co);
    }

    public void onClick(View view) {
        if (view.getTag() != null) {
            if (view.getId() == R.id.fragment_new_friend_list_item_avatar) {
                startActivity(new Intent(getActivity(), ImageActivity.class).putExtra("filepath", view.getTag().toString()));
            }
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        NewFriendAdapter.Item item = adapter.getItem(position);
        if (item.account != null) {
            editor = preferences.edit();
            editor.putString("newfriend_account", item.account);
            editor.putString("newfriend_name", item.name);
            editor.commit();

            NewFriendDialogFragment newFriendDialogFragment = new NewFriendDialogFragment();
            newFriendDialogFragment.show(getFragmentManager(), "dialogfragment_newfriend");

            getActivity().getContentResolver().delete(NewFriendProvider.NEW_FRIEND_URI, NewFriendProvider.NewFriendColumns.ACCOUNT + "= ?", new String[]{item.account});
        }
    }
}
