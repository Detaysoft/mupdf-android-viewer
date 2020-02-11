package com.artifex.mupdf.viewer.gp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.artifex.mupdf.viewer.R;
import com.artifex.mupdf.viewer.gp.util.ThemeColor;
import com.edmodo.cropper.CropImageView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by p1025 on 04.02.2016.
 */
public class CropAndShareActivity extends Activity {

    private Bitmap bmp;
    private CropImageView cropImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crop_and_share_activity);

        findViewById(R.id.crop_base).setBackgroundColor(ThemeColor.getInstance().getThemeColor());

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            //options.inJustDecodeBounds = true;
            bmp = BitmapFactory.decodeFile(getApplicationContext().getFilesDir().getAbsolutePath()+File.separator+"capturedImage.png", options);

            int display_mode = getIntent().getIntExtra("displayMode", Configuration.ORIENTATION_PORTRAIT);

            if(display_mode == Configuration.ORIENTATION_PORTRAIT){
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }

        cropImageView = findViewById(R.id.crop_imageview);
        cropImageView.setGuidelines(1);
        cropImageView.setImageBitmap(bmp);


        Button share = findViewById(R.id.crop_submit_button);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // save bitmap to cache directory
                try {

                    File cachePath = new File(getApplicationContext().getCacheDir(), "images");
                    cachePath.mkdirs(); // don't forget to make the directory
                    FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
                    cropImageView.getCroppedImage().compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                // get saved file and send it to share activity
                File imagePath = new File(getApplicationContext().getCacheDir(), "images");
                File newFile = new File(imagePath, "image.png");

                String authorityString = getApplicationContext().getPackageName().concat(".fileprovider");
                Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), authorityString, newFile);

                if (contentUri != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                    shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(shareIntent, getApplicationContext().getResources().getString(R.string.choose_an_app)));
                }
                else {
                    Toast.makeText(CropAndShareActivity.this, CropAndShareActivity.this.getResources().getText(R.string.unexpected_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button cancel = findViewById(R.id.crop_cancel_button);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

