package com.leif.chatchat.ui;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.leif.chatchat.IM;
import com.leif.chatchat.IMService;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.ui.view.RoundedImageView;
import com.leif.chatchat.util.CompressImageUtil;
import com.leif.chatchat.util.PhotoUtil;
import com.leif.chatchat.util.PinYin;

import java.io.File;
import java.util.Locale;

public class AddContactActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "IMService";

    private EditText inAccount;
    private TextView inNickname;
    private RoundedImageView avatar;

    private IMService service;
    private IMConnection conn = new IMConnection();

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private String avatarPath;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        Intent intent = new Intent(this, IMService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = preferences.edit();
        editor.putString("search_account", "");
        editor.commit();

        inAccount = (EditText) findViewById(R.id.activity_add_contact_input_account);
        inNickname = (TextView) findViewById(R.id.activity_add_contact_text_nickname);
        avatar = (RoundedImageView) findViewById(R.id.activity_add_contact_avatar);

        findViewById(R.id.activity_add_contact_btn_add).setOnClickListener(this);
        findViewById(R.id.activity_add_contact_btn_back).setOnClickListener(this);
        findViewById(R.id.activity_add_contact_btn_search).setOnClickListener(this);
        findViewById(R.id.activity_add_contact_avatar).setOnClickListener(this);

        inNickname.setVisibility(View.INVISIBLE);
        avatar.setVisibility(View.INVISIBLE);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_add_contact_btn_back:
                onBackPressed();
                break;

            case R.id.activity_add_contact_btn_add:
                if (preferences.getString("search_account", "").equals("")) {
                    Toast.makeText(AddContactActivity.this, "请先搜索用户", Toast.LENGTH_LONG).show();
                } else {
                    new AddContactTask().execute(IM.account, preferences.getString("search_account", ""));
                }
                break;

            case R.id.activity_add_contact_btn_search:
                new SearchTask().execute(inAccount.getText().toString().trim());
                break;

            case R.id.activity_add_contact_avatar:
                startActivity(new Intent(this, ImageActivity.class).putExtra("filepath", avatarPath));
                break;

            default:
        }
    }

    private class SearchTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(AddContactActivity.this, null, "正在搜索用户,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.search(strings[0]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(AddContactActivity.this, "搜索到一个用户", Toast.LENGTH_LONG).show();

                    inNickname.setText(preferences.getString("search_nickname", ""));
                    if (preferences.getString("search_avatar", "").equals("null")) {
                        avatar.setImageResource(R.drawable.ic_launcher);
                    } else {
                        avatarPath = preferences.getString("search_avatar", "");
                        File file = new File(Environment.getExternalStorageDirectory(), avatarPath);
                        if (file.exists()) {
                            Uri avatarUri = Uri.fromFile(file);
                            byte b[] = CompressImageUtil.getimage(avatarUri.getPath(), 256, 256);
                            Bitmap bitmap = PhotoUtil.Bytes2Bimap(b);
                            avatar.setImageBitmap(bitmap);
                        } else {
                            avatar.setImageResource(R.drawable.ic_launcher);
                        }
                    }

                    inNickname.setVisibility(View.VISIBLE);
                    avatar.setVisibility(View.VISIBLE);

                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(AddContactActivity.this, "找不到此用户", Toast.LENGTH_LONG).show();

                    inNickname.setVisibility(View.INVISIBLE);
                    avatar.setVisibility(View.INVISIBLE);
                    editor.putString("search_account", "");
                    editor.commit();

                    break;
                case ERROR_CONNECT:
                    Toast.makeText(AddContactActivity.this, "网络错误", Toast.LENGTH_LONG).show();

                    inNickname.setVisibility(View.INVISIBLE);
                    avatar.setVisibility(View.INVISIBLE);
                    editor.putString("search_account", "");
                    editor.commit();

                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(AddContactActivity.this, "未知错误", Toast.LENGTH_LONG).show();

                    inNickname.setVisibility(View.INVISIBLE);
                    avatar.setVisibility(View.INVISIBLE);
                    editor.putString("search_account", "");
                    editor.commit();

                    break;
                default:
            }
        }
    }

    private class AddContactTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(AddContactActivity.this, null, "正在添加好友,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.addContact(strings[0], strings[1]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(AddContactActivity.this, "已申请添加好友", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(AddContactActivity.this, "添加好友错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(AddContactActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(AddContactActivity.this, "未知错误", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
        }
    }

    public class IMConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service Connect");
            service = ((IMService.IMBinder) iBinder).getService();
        }

        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    protected void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }
}
