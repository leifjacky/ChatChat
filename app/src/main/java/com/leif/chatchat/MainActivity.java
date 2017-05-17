package com.leif.chatchat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.leif.chatchat.ui.AddContactActivity;
import com.leif.chatchat.ui.AddShareActivity;
import com.leif.chatchat.ui.SettingActivity;
import com.leif.chatchat.ui.adapter.FragmentAdapter;
import com.leif.chatchat.ui.fragment.Dialog.AddDIalogFragment;
import com.leif.chatchat.ui.view.PageIndicator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AddDIalogFragment.AddTypeListener {

    public static final int CONTACT = 1;
    public static final int SHARE = 2;
    public static MainActivity mainActivity;

    private static final String TAG = "IMService";

    private ViewPager viewPager;
    private PageIndicator indicator;
    private FragmentPagerAdapter adapter;
    private ServiceConnection conn = new IMConnection();
    private IMService service;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;

        Intent intent = new Intent(this, IMService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        viewPager = (ViewPager) findViewById(R.id.activity_main_pager);
        adapter = new FragmentAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        indicator = (PageIndicator) findViewById(R.id.activity_main_pager_indicator);
        indicator.setViewPager(viewPager);

        viewPager.setCurrentItem(1);
        viewPager.setCurrentItem(2);
        viewPager.setCurrentItem(0);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            public void onPageSelected(int position) {
                switch (position){
                    case 0:
                        IMService.Pause = false;
                        break;

                    case 1:
                    case 2:
                        IMService.Pause = true;
                        break;

                    default:
                }
            }

            public void onPageScrollStateChanged(int state) {}
        });

        findViewById(R.id.activity_main_btn_chat).setOnClickListener(this);
        findViewById(R.id.activity_main_btn_contacts).setOnClickListener(this);
        findViewById(R.id.activity_main_btn_share).setOnClickListener(this);
        findViewById(R.id.activity_main_btn_setting).setOnClickListener(this);
        findViewById(R.id.activity_main_btn_add).setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_main_btn_chat:
                viewPager.setCurrentItem(0);
                break;

            case R.id.activity_main_btn_contacts:
                viewPager.setCurrentItem(1);
                break;

            case R.id.activity_main_btn_share:
                viewPager.setCurrentItem(2);
                break;

            case R.id.activity_main_btn_setting:
                startActivity(new Intent(MainActivity.this, SettingActivity.class).putExtra(SettingActivity.EXTRA_SETTING_ACCOUNT, IM.account));
                break;

            case R.id.activity_main_btn_add:
                AddDIalogFragment addDialog = new AddDIalogFragment();
                addDialog.show(getFragmentManager(), "dialogfragment_add");
                break;

            default:
        }
    }

    public void onAddType(int type) {
        switch (type) {
            case CONTACT:
                startActivity(new Intent(this, AddContactActivity.class));
                break;

            case SHARE:
                startActivity(new Intent(this, AddShareActivity.class));
                break;

            default:
        }
    }

    public class IMConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service Connect");
            service = ((IMService.IMBinder) iBinder).getService();
            service.onlineService();
        }

        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    protected void onDestroy() {
        service.Online = false;
        unbindService(conn);
        super.onDestroy();
    }

    public void onBackPressed() {
        // super.onBackPressed();
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    protected void onPause() {
        super.onPause();
        IMService.Pause = true;
        IM.currentSession = null;
    }

    protected void onStart() {
        super.onStart();
        IM.currentSession = null;
        IMService.Pause = false;
    }
}
