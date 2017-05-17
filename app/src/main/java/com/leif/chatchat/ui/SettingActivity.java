package com.leif.chatchat.ui;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.leif.chatchat.IMService;
import com.leif.chatchat.MainActivity;
import com.leif.chatchat.ui.fragment.Dialog.AvatarDialogFragment;
import com.leif.chatchat.ui.fragment.Dialog.NicknameDialogFragment.ChangeNicknameListener;
import com.leif.chatchat.ui.fragment.Dialog.PasswordDialogFragment.ChangePasswordListener;
import com.leif.chatchat.IM;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.ui.fragment.Dialog.NicknameDialogFragment;
import com.leif.chatchat.ui.fragment.Dialog.PasswordDialogFragment;
import com.leif.chatchat.ui.view.RoundedImageView;
import com.leif.chatchat.util.CompressImageUtil;
import com.leif.chatchat.util.FileOperateUtils;
import com.leif.chatchat.util.PhotoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener, ChangeNicknameListener, ChangePasswordListener, AvatarDialogFragment.SelectTypeListener {

    public static final int TAKE_PHOTO = 1;
    public static final int CROP_PHOTO = 2;
    public static final int PICK_PHOTO = 3;
    public static final String EXTRA_SETTING_ACCOUNT = "setting_account";

    private static final String TAG = "SettingAvatar";

    private TextView inAccount, inNickName;
    private Button inSetNickname, inSetPassword;
    private RoundedImageView avatar;
    private Uri imageUri;
    private String path, account, name;
    private Bitmap bitmap;
    private File outputImage;

    private ServiceConnection conn = new IMConnection();
    private IMService service;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Intent intent = new Intent(this, IMService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        inNickName = (TextView) findViewById(R.id.activity_setting_text_nickname);
        inAccount = (TextView) findViewById(R.id.activity_setting_text_account);
        avatar = (RoundedImageView) findViewById(R.id.activity_setting_avatar);
        inSetNickname = (Button) findViewById(R.id.activity_setting_btn_nickname);
        inSetPassword = (Button) findViewById(R.id.activity_setting_btn_password);

        Intent lastIntent = getIntent();
        account = lastIntent.getStringExtra(EXTRA_SETTING_ACCOUNT);
        Cursor cursor = getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = '" + account + "'", null, null);
        cursor.moveToFirst();
        name = cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.NAME));
        path = cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.AVATAR));

        inNickName.setText(name);
        inAccount.setText(account);

        File avatarFile = new File(Environment.getExternalStorageDirectory(), path);
        if (avatarFile.exists()) {
            Uri avatarUri = Uri.fromFile(avatarFile);
            byte b[] = CompressImageUtil.getimage(avatarUri.getPath(), 256, 256);
            bitmap = PhotoUtil.Bytes2Bimap(b);
            avatar.setImageBitmap(bitmap);
        }

        findViewById(R.id.activity_setting_avatar).setOnClickListener(this);
        findViewById(R.id.activity_setting_btn_back).setOnClickListener(this);
        findViewById(R.id.activity_setting_btn_logoff).setOnClickListener(this);

        if (IM.account.equals(account)){
            inSetNickname.setVisibility(View.VISIBLE);
            inSetPassword.setVisibility(View.VISIBLE);
            inSetNickname.setOnClickListener(this);
            inSetPassword.setOnClickListener(this);
        } else {
            inSetNickname.setVisibility(View.GONE);
            inSetPassword.setVisibility(View.GONE);
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_setting_btn_back:
                onBackPressed();
                break;

            case R.id.activity_setting_btn_nickname:
                NicknameDialogFragment nicknameDialog = new NicknameDialogFragment();
                nicknameDialog.show(getFragmentManager(), "dialogfragment_nickname");
                break;

            case R.id.activity_setting_btn_password:
                PasswordDialogFragment passwordDialog = new PasswordDialogFragment();
                passwordDialog.show(getFragmentManager(), "dialogfragment_password");
                break;

            case R.id.activity_setting_avatar:
                AvatarDialogFragment avatarDialog = new AvatarDialogFragment();
                avatarDialog.show(getFragmentManager(), "dialogfragment_avatar");
                break;

            case R.id.activity_setting_btn_logoff:
                IMService.Online = false;
                MainActivity.mainActivity.finish();
                Runtime.getRuntime().exit(0);
                break;

            default:
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(imageUri, "image/*");
                    intent.putExtra("scale", true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CROP_PHOTO);
                }
                break;

            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    byte b[] = CompressImageUtil.getimage(imageUri.getPath(), 256, 256);
                    bitmap = PhotoUtil.Bytes2Bimap(b);
                    try {
                        FileOutputStream out = null;
                        out = new FileOutputStream(new File(imageUri.getPath()));
                        out.write(b, 0, b.length);
                        out.flush();
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    new UploadTask().execute(Environment.getExternalStorageDirectory() + "/" + path, path);
                }
                break;

            case PICK_PHOTO:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    } else {
                        handleImageBeforeKitKat(data);
                    }
                }
                break;

            default:
        }
    }

    public void onSelectType(int type) {
        switch (type) {
            case TAKE_PHOTO:
                takePhoto();
                break;

            case PICK_PHOTO:
                pickPhoto();
                break;

            default:
        }
    }

    private void takePhoto() {
        path = "chatchat/avatar/avatar_" + IM.account + ".jpg";
        outputImage = new File(Environment.getExternalStorageDirectory(), "/" + path);

        try {
            File parent = outputImage.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
            imageUri = Uri.fromFile(outputImage);
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PHOTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void pickPhoto() {
        path = "chatchat/avatar/avatar_" + IM.account + ".jpg";
        outputImage = new File(Environment.getExternalStorageDirectory(), "/" + path);

        try {
            File parent = outputImage.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
            imageUri = Uri.fromFile(outputImage);
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setDataAndType(imageUri, "image/*");
            startActivityForResult(intent, PICK_PHOTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(19)
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        }
        FileOperateUtils.copyfile(new File(imagePath), outputImage, true);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CROP_PHOTO);
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
    }

    private String getImagePath(Uri uri, String selection) {

        String path = null;
        Cursor cursor = getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    public void onNicknameChangeComplete(String nickname) {
        inNickName.setText(nickname);
        IM.nickname = nickname;
//        ContentValues values = new ContentValues();
//        values.put(ContactsProvider.ContactColumns.NAME, nickname);
//        getContentResolver().update(ContactsProvider.CONTACT_URI, values, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{IM.account});
    }

    public void onPasswordChangeComplete(String password) {
        IM.password = password;
    }

    private class UploadTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(SettingActivity.this, null, "正在更改头像,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.upload(strings[0], strings[1]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(SettingActivity.this, "更改成功", Toast.LENGTH_LONG).show();
                    service.changSetting("&" + ContactsProvider.ContactColumns.ACCOUNT + "=" + IM.account
                            + "&" + ContactsProvider.ContactColumns.AVATAR + "=/" + path);
//                    ContentValues values = new ContentValues();
//                    values.put(ContactsProvider.ContactColumns.AVATAR, "/" + path);
//                    getContentResolver().update(ContactsProvider.CONTACT_URI, values, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{IM.account});
                    avatar.setImageBitmap(bitmap);
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(SettingActivity.this, "账户验证失败", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(SettingActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(SettingActivity.this, "未知错误", Toast.LENGTH_LONG).show();
                    break;
                default:
            }
        }
    }

    public class IMConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service Connect");
            service = ((IMService.IMBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    protected void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }
}