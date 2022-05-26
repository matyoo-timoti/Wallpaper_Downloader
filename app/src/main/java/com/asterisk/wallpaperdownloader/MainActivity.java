package com.asterisk.wallpaperdownloader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    ImageView ivShowImage;
    ImageButton btnSearch;
    Button btnSaveImg;
    Button btnRefresh;
    Button btnRandom;
    Button btnGallery;
    EditText edKeyword;
    int screenWidth;
    int screenHeight;
    String key;
    String recentlySaved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        //---get device screen resolution---
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        //Start with a random image
        String source = String.format("https://source.unsplash.com/%sx%s/?%s", screenWidth, screenHeight, "random");
        new ImageDownloader().execute(source);

        ivShowImage = findViewById(R.id.ivShowImage);
        edKeyword = findViewById(R.id.edTxtKeyword);
        btnSearch = findViewById(R.id.btnDownload);
        btnSaveImg = findViewById(R.id.btnSaveImg);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnRandom = findViewById(R.id.btnRandom);
        btnGallery = findViewById(R.id.btnGallery);

        btnSearch.setOnClickListener(this::onClickSearch);
        btnSaveImg.setOnClickListener(this::onClickSaveImg);
        btnRefresh.setOnClickListener(this::onClickRefresh);
        btnRandom.setOnClickListener(this::onClickShuffle);
        btnGallery.setOnClickListener(this::onClickGallery);
    }

    private void onClickGallery(View view) {
        openFolder();
    }

    private void onClickRefresh(View view) {
        String source = String.format("https://source.unsplash.com/%sx%s/?%s", screenWidth, screenHeight, key);
        new ImageDownloader().execute(source);
        Toast.makeText(this, "Loading new image", Toast.LENGTH_SHORT).show();
    }

    private void onClickShuffle(View view) {
        edKeyword.setText("");
        String source = String.format("https://source.unsplash.com/%sx%s/?%s", screenWidth, screenHeight, "random");
        new ImageDownloader().execute(source);
        Toast.makeText(this, "Random", Toast.LENGTH_SHORT).show();
    }

    private void onClickSaveImg(View view) {
        if (ivShowImage.getDrawable() == null) {
            Toast.makeText(this, "There is no image loaded!", Toast.LENGTH_SHORT).show();
        } else saveImg();
    }

    private void onClickSearch(View view) {
        String keyword = edKeyword.getText().toString();
        //---if edit text is empty---
        if (TextUtils.isEmpty(edKeyword.getText()) || edKeyword.getText().toString().equals("")) {
            Toast toast = Toast.makeText(this, "Please Enter a keyword!", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL, Gravity.CENTER_VERTICAL, 0);
            toast.show();
        } else {
            key = keyword;
            String source = String.format("https://source.unsplash.com/%sx%s/?%s", screenWidth, screenHeight, keyword);
            new ImageDownloader().execute(source);
        }
    }

    private void saveImg() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        FileOutputStream fileOutputStream;
        File file = getDisc();

        if (!file.exists() && !file.mkdirs()) {
            file.mkdirs();
        }

        @SuppressLint("SimpleDateFormat")
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String name = String.format("IMG%s.jpg", date);
        String fileName = String.format("%s/%s", file.getAbsolutePath(), name);
        File newFile = new File(fileName);
        recentlySaved = fileName;
        try {
            BitmapDrawable draw = (BitmapDrawable) ivShowImage.getDrawable();
            Bitmap bitmap = draw.getBitmap();
            fileOutputStream = new FileOutputStream(newFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshGallery(newFile);
    }

    private void refreshGallery(File file) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        getBaseContext().sendBroadcast(intent);
    }

    private File getDisc() {
        //---path to app's internal storage---
        //String filePath = this.getApplicationContext().getExternalFilesDir(null).toString();

        //---path to phone's public picture storage---
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(path, "WallpaperDownloads");
    }

    public void openFolder() {
        if (recentlySaved == null) {
            Toast.makeText(this, "There are no saved images yet.", Toast.LENGTH_SHORT).show();
        } else {
            Uri uri = Uri.parse(recentlySaved);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
        HttpURLConnection httpURLConnection;

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                return BitmapFactory.decodeStream(inputStream);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                httpURLConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                ivShowImage.setImageBitmap(bitmap);

                Toast toast = Toast.makeText(getApplicationContext(), "Image loaded", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 20);
                toast.show();

            } else
                Toast.makeText(getApplicationContext(), "Image failed to load", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }
}