package com.leif.chatchat.ui;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.leif.chatchat.IMService;
import com.leif.chatchat.R;
import com.leif.chatchat.ui.adapter.NewFriendFragmentAdapter;
import com.leif.chatchat.ui.fragment.Dialog.NewFriendDialogFragment.FriendResultListener;

public class NewFriendActivity extends AppCompatActivity implements FriendResultListener, View.OnClickListener {

    private static final String TAG = "IMService";

    private static final int ACCEPT = 1;
    private static final int IGNORE = 0;

    private ViewPager viewPager;
    private FragmentPagerAdapter adapter;
    private ServiceConnection conn = new IMConnection();
    private IMService service;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);

        Intent intent = new Intent(this, IMService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        viewPager = (ViewPager) findViewById(R.id.activity_new_friend_pager);
        adapter = new NewFriendFragmentAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        findViewById(R.id.activity_new_friend_btn_back).setOnClickListener(this);
    }

    public void onFriendResult(int type, String fromAccount, String toAccount) {
        switch (type){
            case ACCEPT:
                new NewFriendTask().execute(fromAccount, toAccount);
                break;

            case IGNORE:
            default:
        }
    }

    public void onClick(View view) {
        switch (view.getId()){
            case R.id.activity_new_friend_btn_back:
                onBackPressed();
                break;

            default:
        }
    }

    private class NewFriendTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(NewFriendActivity.this, null, "正在响应好友请求,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.friendResult(strings[0], strings[1]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(NewFriendActivity.this, "成功响应好友请求", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(NewFriendActivity.this, "响应失败", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(NewFriendActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(NewFriendActivity.this, "未知错误", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
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
        unbindService(conn);
        super.onDestroy();
    }
}
