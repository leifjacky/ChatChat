package com.leif.chatchat.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.leif.chatchat.IM;
import com.leif.chatchat.IMService;
import com.leif.chatchat.MainActivity;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;

/**
 * 登录
 */
public class LoginActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "IMService";

    private static final int ACTIVITY_SIGN_RESULTCODE = 1;

    private EditText inAccount, inPassword;
    private CheckBox remPass;
    private ServiceConnection conn = new IMConnection();
    private IMService service;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Intent intent = new Intent(LoginActivity.this, IMService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        inAccount = (EditText) findViewById(R.id.activity_login_input_account);
        inPassword = (EditText) findViewById(R.id.activity_login_input_password);
        remPass = (CheckBox) findViewById(R.id.activity_login_chkbox_remember_pass);
        findViewById(R.id.activity_login_btn_login).setOnClickListener(this);
        findViewById(R.id.activity_login_btn_register).setOnClickListener(this);
        boolean isRemember = preferences.getBoolean("remember_pass", false);

        if (isRemember) {
            inAccount.setText(preferences.getString("login_account", ""));
            inPassword.setText(preferences.getString("login_password", ""));
            remPass.setChecked(true);
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_login_btn_login:
                String accountStr = inAccount.getText().toString().trim();
                String passwordStr = inPassword.getText().toString().trim();

                if (TextUtils.isEmpty(accountStr) || TextUtils.isEmpty(passwordStr)) {
                    Toast.makeText(LoginActivity.this, "账户名或密码不能为空", Toast.LENGTH_LONG).show();
                    return;
                }

                new LoginTask().execute(inAccount.getText().toString(), inPassword.getText().toString());
                break;

            case R.id.activity_login_btn_register:
                startActivityForResult(new Intent(LoginActivity.this, SignActivity.class), ACTIVITY_SIGN_RESULTCODE);
                break;

            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case ACTIVITY_SIGN_RESULTCODE:
                if (requestCode == RESULT_OK){
                    String returnData = data.getStringExtra("activity_sign_result");
                    if (returnData.equals("NOTHING")){

                    } else if (returnData.equals("OK")) {
                        String account, password;
                        account = data.getStringExtra("account");
                        password = data.getStringExtra("password");

                        new LoginTask().execute(account, password);
                    }
                }
                break;

            default:
        }
    }

    private class LoginTask extends AsyncTask<String, Void, Integer>{

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(LoginActivity.this, null, "正在验证账户,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.login(strings[0], strings[1]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(LoginActivity.this, "登陆成功", Toast.LENGTH_LONG).show();

                    IM.password = inPassword.getText().toString().trim();
                    editor = preferences.edit();
                    if (remPass.isChecked()) {
                        editor.putBoolean("remember_pass", true);
                        editor.putString("login_account", inAccount.getText().toString());
                        editor.putString("login_password", inPassword.getText().toString());
                    } else {
                        editor.putBoolean("remember_pass", false);
                        editor.putString("login_account",  null);
                        editor.putString("login_password", null);
                    }
                    editor.commit();

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    LoginActivity.this.finish();
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(LoginActivity.this, "账户验证失败", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(LoginActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(LoginActivity.this, "未知错误", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
        }
    }

    public class IMConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service Connect");
            service = ((IMService.IMBinder)iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    @Override
    protected void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }
}
