package com.leif.chatchat.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.leif.chatchat.IM;
import com.leif.chatchat.IMService;
import com.leif.chatchat.R;
import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.ui.fragment.ContactFragment;
import com.leif.chatchat.ui.fragment.Dialog.AvatarDialogFragment;
import com.leif.chatchat.util.CompressImageUtil;
import com.leif.chatchat.util.FileOperateUtils;
import com.leif.chatchat.util.PhotoUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddShareActivity extends AppCompatActivity implements View.OnClickListener, AvatarDialogFragment.SelectTypeListener {

    public static final int TAKE_PHOTO = 1;
    public static final int CROP_PHOTO = 2;
    public static final int PICK_PHOTO = 3;

    private static final String TAG = "IMService";

    private EditText inContent;
    private ImageView inPhoto;
    private TextView inSelect;
    private Uri imageUri;
    private String path, date;
    private Bitmap bitmap;
    private File outputImage;

    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    private Date curDate;

    private IMService service;
    private IMConnection conn = new IMConnection();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_share);

        Intent intent = new Intent(this, IMService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);

        path = "null";
        inContent = (EditText) findViewById(R.id.activity_add_share_text_content);
        inPhoto = (ImageView) findViewById(R.id.activity_add_share_photo);
        inSelect = (TextView) findViewById(R.id.activity_add_share_select_photo);

        findViewById(R.id.activity_add_share_btn_back).setOnClickListener(this);
        findViewById(R.id.activity_add_share_btn_add).setOnClickListener(this);
        inPhoto.setOnClickListener(this);
        inSelect.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_setting_btn_back:
                onBackPressed();
                break;

            case R.id.activity_add_share_btn_add:
                new AddShareTask().execute(IM.account, inContent.getText().toString().trim(), "/" + path);
                break;

            case R.id.activity_add_share_photo:
            case R.id.activity_add_share_select_photo:
                AvatarDialogFragment avatarDialog = new AvatarDialogFragment();
                avatarDialog.show(getFragmentManager(), "dialogfragment_avatar");
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
                    byte b[] = CompressImageUtil.getimage(imageUri.getPath(), 800 ,480);
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
        curDate = new Date(System.currentTimeMillis());
        date = formatter.format(curDate);
        path = "chatchat/share/" + date + "_" + IM.account + ".jpg";
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
        path = "chatchat/share/" + date + "_" + IM.account + ".jpg";
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

    private class UploadTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(AddShareActivity.this, null, "正在上传图片,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.upload(strings[0], strings[1]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(AddShareActivity.this, "上传成功", Toast.LENGTH_LONG).show();
                    inSelect.setVisibility(View.GONE);
                    inPhoto.setImageBitmap(bitmap);
                    inPhoto.setVisibility(View.VISIBLE);
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(AddShareActivity.this, "上传失败", Toast.LENGTH_LONG).show();
                    inPhoto.setVisibility(View.GONE);
                    inSelect.setVisibility(View.VISIBLE);
                    path = "null";
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(AddShareActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    inPhoto.setVisibility(View.GONE);
                    inSelect.setVisibility(View.VISIBLE);
                    path = "null";
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(AddShareActivity.this, "未知错误", Toast.LENGTH_LONG).show();
                    inPhoto.setVisibility(View.GONE);
                    inSelect.setVisibility(View.VISIBLE);
                    path = "null";
                    break;
                default:
            }
        }
    }

    private class AddShareTask extends AsyncTask<String, Void, Integer> {

        private static final int OK = 0;
        private static final int ERROR_ACCOUNT = 1;
        private static final int ERROR_CONNECT = 2;
        private static final int ERROR_UNKNOWN = -1;

        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(AddShareActivity.this, null, "正在发布,请稍后...");
        }

        protected Integer doInBackground(String... strings) {
            return service.addShare(strings[0], strings[1], strings[2]);
        }

        protected void onPostExecute(Integer integer) {
            dialog.dismiss();
            switch (integer) {
                case OK:
                    Toast.makeText(AddShareActivity.this, "发布成功", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case ERROR_ACCOUNT:
                    Toast.makeText(AddShareActivity.this, "发布失败", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_CONNECT:
                    Toast.makeText(AddShareActivity.this, "网络错误", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_UNKNOWN:
                    Toast.makeText(AddShareActivity.this, "未知错误", Toast.LENGTH_LONG).show();
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
