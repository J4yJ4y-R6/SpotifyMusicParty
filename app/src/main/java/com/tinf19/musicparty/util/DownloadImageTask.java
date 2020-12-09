package com.tinf19.musicparty.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Asynchronous Task for downloading a image from the internet via url and returning a bitmap
 * @author Jannik Junker
 * @author Silas Wessely
 * @since 1.1
 */
public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    ImageView bmImage;

    /**
     * Constructor to set the bitmap directly in the view
     * @param bmImage {@link ImageView} where the image will be displayed
     */
    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    /**
     * Downloading a image from the internet via url and returning a bitmap
     * @param urls Url to find the image in the internet
     * @return Get the converted image as a bitmap
     */
    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage(), e);
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.setImageBitmap(result);
    }


}
