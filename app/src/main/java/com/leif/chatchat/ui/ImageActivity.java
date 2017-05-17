package com.leif.chatchat.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.leif.chatchat.R;
import com.leif.chatchat.util.CompressImageUtil;
import com.leif.chatchat.util.PhotoUtil;

import java.io.File;

public class ImageActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView imageView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        Intent intent = getIntent();
        String filepath = intent.getStringExtra("filepath");

        findViewById(R.id.activity_image_btn_back).setOnClickListener(this);

        imageView = (ImageView) findViewById(R.id.activity_main_image);
        imageView.setImageResource(R.drawable.ic_launcher);

        File file = new File(Environment.getExternalStorageDirectory(), filepath);
        if (file.exists()) {
            Uri avatarUri = Uri.fromFile(file);
            byte b[] = CompressImageUtil.getimage(avatarUri.getPath(), 800, 480);
            Bitmap bitmap = PhotoUtil.Bytes2Bimap(b);
            imageView.setImageBitmap(bitmap);
        }
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_image_btn_back:
                onBackPressed();
                break;

            default:
        }
    }
}