package com.leif.chatchat.ui.fragment;


import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.ShareProvider;
import com.leif.chatchat.ui.ImageActivity;
import com.leif.chatchat.ui.adapter.ContactsAdapter;
import com.leif.chatchat.ui.adapter.ShareAdapter;

public class ShareFragment extends ListFragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private ShareAdapter adapter;
    private ContentObserver co;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new ShareAdapter();
        adapter.setOnItemViewClickListener(this);
        setListAdapter(adapter);
        getListView().setAdapter(adapter);
        getListView().setOnItemClickListener(this);

        co = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                adapter = new ShareAdapter();
                adapter.setOnItemViewClickListener(ShareFragment.this);
                getListView().setAdapter(adapter);
            }
        };

        getActivity().getContentResolver().registerContentObserver(ShareProvider.SHARE_URI, true, co);
    }

    public void onDestroy(){
        super.onDestroy();
        getActivity().getContentResolver().unregisterContentObserver(co);
    }

    public void onClick(View view) {
        switch (view.getId()){
            case R.id.fragment_share_list_item_photo:
            case R.id.fragment_share_list_item_avatar:
                startActivity(new Intent(getActivity(), ImageActivity.class).putExtra("filepath", view.getTag().toString()));

            default:
        }
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
