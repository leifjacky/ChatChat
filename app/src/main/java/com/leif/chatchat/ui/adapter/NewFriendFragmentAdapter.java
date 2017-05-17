package com.leif.chatchat.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.leif.chatchat.ui.fragment.ContactFragment;
import com.leif.chatchat.ui.fragment.NewFriendFragment;
import com.leif.chatchat.ui.fragment.SessionFragment;
import com.leif.chatchat.ui.fragment.ShareFragment;

/**
 * Created by leif on 6/3/16.
 */
public class NewFriendFragmentAdapter extends FragmentPagerAdapter {

    public NewFriendFragmentAdapter(FragmentManager fm){
        super(fm);
    }

    public Fragment getItem(int position) {
        Fragment fragment = null;

        switch (position){
            case 0:
                fragment = new NewFriendFragment();
                break;
            default:
        }
        return fragment;
    }

    public int getCount() {
        return 1;
    }
}
