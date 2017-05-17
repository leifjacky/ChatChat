package com.leif.chatchat.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.leif.chatchat.IM;
import com.leif.chatchat.IMService;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.SMSProvider;
import com.leif.chatchat.ui.adapter.ChatAdapter;
import com.leif.chatchat.ui.fragment.Dialog.AvatarDialogFragment;
import com.leif.chatchat.util.CompressImageUtil;
import com.leif.chatchat.util.FileOperateUtils;
import com.leif.chatchat.util.PhotoUtil;
import com.leif.chatchat.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatActivity extends FragmentActivity implements View.OnClickListener, AvatarDialogFragment.SelectTypeListener {

    public static final int TAKE_PHOTO = 1;
    public static final int CROP_PHOTO = 2;
    public static final int PICK_PHOTO = 3;
    private static final String TAG = "SettingAvatar";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_OTHER_ACCOUNT = "other_account";

    private String session_id, otherAccount;
    private ListView listView;
    private EditText input;
    private TextView title;
    private ImageView camera;
    private ChatAdapter adapter;
    private ContentObserver co;
    private Intent lastIntent;

    private Uri imageUri;
    private String path, date;
    private Bitmap bitmap;
    private File outputImage;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    private Date curDate;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private ServiceConnection conn = new IMConnection();
    private IMService service;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        bindService(new Intent(this, IMService.class), conn, BIND_AUTO_CREATE);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        lastIntent = getIntent();
        session_id = lastIntent.getStringExtra(EXTRA_SESSION_ID);
        otherAccount = lastIntent.getStringExtra(EXTRA_OTHER_ACCOUNT);
        IM.currentSession = session_id;

        listView = (ListView) findViewById(R.id.activity_chat_list);
        input = (EditText) findViewById(R.id.activity_chat_send_input);
        title = (TextView) findViewById(R.id.activity_chat_text_name);
        camera = (ImageView) findViewById(R.id.activity_chat_btn_camera);
        change();

        findViewById(R.id.activity_chat_send_btn).setOnClickListener(this);
        findViewById(R.id.activity_chat_btn_back).setOnClickListener(this);
        findViewById(R.id.activity_chat_btn_camera).setOnClickListener(this);

        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    camera.setVisibility(View.GONE);
                } else {
                    camera.setVisibility(View.VISIBLE);
                }
            }
        });

        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                listView.setFocusable(true);
                listView.setFocusableInTouchMode(true);
                listView.requestFocus();
                return false;
            }
        });

        adapter = new ChatAdapter(session_id);
        adapter.setOnChatViewClickListener(this);
        listView.setAdapter(adapter);

        co = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                Cursor cursor = getContentResolver().query(SMSProvider.SMS_URI, null, SMSProvider.SMSColumns.SESSION_ID + " = ?", new String[]{session_id}, null);
                adapter.swapCursor(cursor);
                change();
            }
        };

        getContentResolver().registerContentObserver(SMSProvider.SMS_URI, true, co);
        getContentResolver().registerContentObserver(ContactsProvider.CONTACT_URI, true, co);
    }

    private void change() {
        Cursor cursor = getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{otherAccount}, null);
        cursor.moveToFirst();
        title.setText(cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.NAME)));
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_chat_btn_back:
                onBackPressed();
                break;

            case R.id.activity_chat_btn_camera:
                AvatarDialogFragment avatarDialog = new AvatarDialogFragment();
                avatarDialog.show(getFragmentManager(), "dialogfragment_avatar");
                break;

            case R.id.activity_chat_send_btn:
                String content = input.getText().toString();
                if (!TextUtils.isEmpty(content)) {
                    service.sendSMS(IM.account, otherAccount, content);
                }
                input.setText(null);
                break;

            case R.id.activity_chat_item_photo:
                startActivity(new Intent(this, ImageActivity.class).putExtra("filepath", view.getTag().toString()));
                break;

            case R.id.activity_chat_item_avatar:
                startActivity(new Intent(this, ImageActivity.class).putExtra("filepath", view.getTag().toString()));
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
                    byte b[] = CompressImageUtil.getimage(imageUri.getPath(), 800, 480);
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
                    new PhotoTask().execute(IM.account, otherAccount, Environment.getExternalStorageDirectory() + "/" + path, path);
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
        curDate = new Date(System.currentTimeMillis());
        date = formatter.format(curDate);
        path = "chatchat/chat/" + date + "_" + IM.account + ".jpg";
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
        curDate = new Date(System.currentTimeMillis());
        date = formatter.format(curDate);
        path = "chatchat/chat/" + date + "_" + IM.account + ".jpg";
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

    private class PhotoTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(ChatActivity.this, null, "正在发送图片,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.sendPHOTO(strings[0], strings[1], strings[2], strings[3]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(ChatActivity.this, "发送失败", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(ChatActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(ChatActivity.this, "未知错误", Toast.LENGTH_LONG).show();
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
        editor = preferences.edit();
        editor.putInt("count_" + session_id, 0);
        editor.commit();
        getContentResolver().notifyChange(SMSProvider.SMS_URI, null);

        unbindService(conn);
        getContentResolver().unregisterContentObserver(co);
        super.onDestroy();
    }

    public void onBackPressed() {
        editor = preferences.edit();
        editor.putInt("count_" + session_id, 0);
        editor.commit();
        getContentResolver().notifyChange(SMSProvider.SMS_URI, null);

        IM.currentSession = null;

        super.onBackPressed();
    }
}
