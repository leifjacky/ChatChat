package com.leif.chatchat.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.leif.chatchat.IMService;
import com.leif.chatchat.R;

public class SignActivity extends Activity implements View.OnClickListener {

    private EditText inAccount, inPassword, inRePassword;
    private IMService service;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        inAccount = (EditText) findViewById(R.id.activity_sign_input_account);
        inPassword = (EditText) findViewById(R.id.activity_sign_input_password);
        inRePassword = (EditText) findViewById(R.id.activity_sign_input_repassword);
        findViewById(R.id.activity_sign_btn_register).setOnClickListener(this);
        findViewById(R.id.activity_sign_btn_back).setOnClickListener(this);

        service = new IMService();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_sign_btn_register:
                String accountStr = inAccount.getText().toString().trim();
                String passwordStr = inPassword.getText().toString().trim();
                String rePasswordStr = inRePassword.getText().toString().trim();

                if (TextUtils.isEmpty(accountStr) || TextUtils.isEmpty(passwordStr)) {
                    Toast.makeText(SignActivity.this, "账户名或密码不能为空", Toast.LENGTH_LONG).show();
                    return;
                }

                char[] pass = (accountStr + passwordStr).toCharArray();
                boolean flag = true;
                for (char c : pass) {
                    if (c >= '0' && c <= '9') ;
                    else if (c >= 'a' && c <= 'z') ;
                    else if (c >= 'A' && c <= 'Z') ;
                    else {
                        flag = false;
                        break;
                    }
                }
                if (!flag) {
                    Toast.makeText(SignActivity.this, "用户名、密码只能由英文字母和数字组成", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!passwordStr.equals(rePasswordStr)) {
                    Toast.makeText(SignActivity.this, "两次输入的密码不同", Toast.LENGTH_LONG).show();
                    return;
                }

                new AsyncTask<String, Void, Integer>() {

                    private static final int OK = 0;
                    private static final int ACCOUNT_USED = 1;
                    private static final int ERROR_CONNECT = 2;
                    private static final int ERROR_UNKNOWN = -1;

                    private ProgressDialog dialog;

                    protected void onPreExecute() {
                        super.onPreExecute();
                        dialog = ProgressDialog.show(SignActivity.this, null, "注册中，请稍后...");
                    }

                    protected Integer doInBackground(String... strings) {
                        return service.sign(strings[0], strings[1]);
                    }

                    protected void onPostExecute(Integer integer) {
                        super.onPostExecute(integer);
                        dialog.dismiss();

                        switch (integer) {
                            case OK:
                                Toast.makeText(SignActivity.this, "注册成功", Toast.LENGTH_LONG).show();

                                Intent intent = new Intent();
                                intent.putExtra("activity_sign_return", "OK");
                                intent.putExtra("account", inAccount.getText().toString());
                                intent.putExtra("password", inPassword.getText().toString());
                                setResult(RESULT_OK, intent);
                                SignActivity.this.finish();
                                break;

                            case ACCOUNT_USED:
                                Toast.makeText(SignActivity.this, "该账号已存在，请输入其他账号", Toast.LENGTH_LONG).show();
                                break;

                            case ERROR_CONNECT:
                                Toast.makeText(SignActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                                break;

                            case ERROR_UNKNOWN:
                                Toast.makeText(SignActivity.this, "未知错误", Toast.LENGTH_LONG).show();
                                break;

                            default:
                        }
                    }
                }.execute(inAccount.getText().toString(), inPassword.getText().toString());

                break;

            case R.id.activity_sign_btn_back:
                onBackPressed();
                break;

            default:
        }
    }

    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("activity_sign_result", "NOTHING");
        setResult(RESULT_OK, intent);
        SignActivity.this.finish();
    }
}
