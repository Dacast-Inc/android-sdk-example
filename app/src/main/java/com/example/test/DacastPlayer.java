package com.example.test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.theoplayer.android.api.THEOplayerView;
import com.theoplayer.android.api.source.SourceDescription;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    private final WeakReference<ImageView> bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = new WeakReference<>(bmImage);
    }

    protected Bitmap doInBackground(String... urls) {
        try {
            InputStream in = new URL(urls[0]).openStream();
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    protected void onPostExecute(Bitmap result) {
        bmImage.get().setImageBitmap(result);
    }
}

class ContentInfoResponse {
    ContentInfo contentInfo;
}
class ContentInfo {
    String splashscreenUrl;
    Features features;
}
class Features {
    Watermark watermark;
}
class Watermark {
    String imageUrl;
}

class PlaybackUrlResponse {
    String hls;
    String mp4;
}

class ContentInfoBlob {
    PlaybackUrlResponse playback;
    ContentInfo info;

    ContentInfoBlob(PlaybackUrlResponse playback, ContentInfo info){
        this.playback = playback;
        this.info = info;
    }
}

public class DacastPlayer {

    private static final String URL_BASE = "https://playback.dacast.com";

    private final THEOplayerView theoplayer;
    private final RelativeLayout layout;
    private final ImageView watermarkImage;

    public DacastPlayer(Activity activity, String contentIdStr) {
        this(activity, contentIdStr, null);
    }

    public DacastPlayer(Activity activity, String contentId, String adUrl) {
        theoplayer = new THEOplayerView(activity);
        watermarkImage = new ImageView(activity);
        layout = new RelativeLayout(activity);

        watermarkImage.setClickable(false);
        watermarkImage.setFocusable(false);

        RelativeLayout.LayoutParams paramsPlayer = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        paramsPlayer.leftMargin = 0;
        paramsPlayer.topMargin = 0;
        layout.addView(theoplayer, paramsPlayer);

        RelativeLayout.LayoutParams paramsImage = new RelativeLayout.LayoutParams(300, 300);
        paramsImage.leftMargin = 50;
        paramsImage.topMargin = 10;
        layout.addView(watermarkImage, paramsImage);
        watermarkImage.setImageAlpha(90);

        fetchVideoInfo(contentId, adUrl);
    }

    public View getView(){
        return layout;
    }

    public THEOplayerView getTHEOplayer(){
        return theoplayer;
    }

    public void onPause() {
        theoplayer.onPause();
    }

    public void onResume() {
        theoplayer.onResume();
    }

    public void onDestroy() {
        theoplayer.onDestroy();
    }

    private void fetchVideoInfo(final String contentId, final String adUrl){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String rawContentInfo = httpGet( URL_BASE + "/content/info?provider=universe&contentId=" + contentId);
                    String rawPlaybackUrl = httpGet(URL_BASE + "/content/access?provider=universe&contentId=" + contentId);

                    Gson gson = new Gson();
                    ContentInfoResponse contentInfoResp = gson.fromJson(rawContentInfo, ContentInfoResponse.class);
                    PlaybackUrlResponse playbackUrl = gson.fromJson(rawPlaybackUrl, PlaybackUrlResponse.class);

                    Handler handler = new Handler(Looper.getMainLooper(), message -> {
                        ContentInfoBlob blob = (ContentInfoBlob)message.obj;

                        String sourceUrl = blob.playback.hls;
                        if (sourceUrl == null) {
                            sourceUrl = blob.playback.mp4;
                        }

                        SourceDescription.Builder sourceDescription = SourceDescription.Builder
                            .sourceDescription(sourceUrl)
                            .poster(blob.info.splashscreenUrl);

                        if(adUrl != null){
                            sourceDescription.ads(adUrl);
                        }

                        theoplayer.getPlayer().setSource(sourceDescription.build());

                        return true;
                    });

                    handler.sendMessage(handler.obtainMessage(0, new ContentInfoBlob(playbackUrl, contentInfoResp.contentInfo)));

                    if(contentInfoResp.contentInfo.features.watermark != null && contentInfoResp.contentInfo.features.watermark.imageUrl != null){
                        new DownloadImageTask(watermarkImage).execute(contentInfoResp.contentInfo.features.watermark.imageUrl);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private static String httpGet(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }
}
