/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cast.refplayer.browser;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class VideoProvider {

    private static final String TAG = "VideoProvider";
    private static String TAG_MEDIA = "videos";
    private static String THUMB_PREFIX_URL =
            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/";


    private static String VIDEO_URL_TEMPLATE =
            "http://h264-aws.vevo.com/v3/h264/%s/%s_high_1280x720_h264_2000_aac_128.mp4";
    private static String TAG_VIDEOS = "videos";
    private static String TAG_VIDEO_ID = "isrc";
    private static String TAG_TITLE = "title";
    private static String TAG_RELEASE_DATE = "releaseDate";
    private static String TAG_IMAGE_URL = "thumbnailUrl";
    private static String TAG_ARTISTS = "artists";
    private static String TAG_ARTIST_NAME = "name";

    private static final int MAX_VIDEOS = 200;

    private static List<MediaInfo> mediaList;

    protected JSONObject parseUrl(String urlString) {
        InputStream is = null;
        try {
            java.net.URL url = new java.net.URL(urlString);
            URLConnection urlConnection = url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Given a unique video identifier, returns a URL at to which video can
     * be played.
     * @param vid
     * @return
     */
    public static String getVideoURL(String vid) {

        if (vid == null || vid.length() == 0) {
            return "";
        }

        String url = String.format(VIDEO_URL_TEMPLATE, vid.toUpperCase(), vid.toLowerCase());
        Log.d(TAG, "Requesting URL: " + url);
        return url;
    }

    /**
     * Builds a list of MediaInfo objects given a URL that points to a JSON listing
     * of videos.
     * @param url - url to json file
     * @return
     * @throws JSONException
     */
    public static List<MediaInfo> buildMedia(String url) throws JSONException {

        if (null != mediaList) {
            return mediaList;
        }
        mediaList = new ArrayList<MediaInfo>();
        JSONObject jsonObj = new VideoProvider().parseUrl(url);

        JSONArray videos = jsonObj.getJSONArray(TAG_VIDEOS);

        if (videos != null) {
            for (int i  = 0; i < videos.length(); ++i) {
                // TODO(jervis): DELETE ME!!
                if (i > MAX_VIDEOS) break;

                JSONObject video = videos.getJSONObject(i);
                String vid = video.getString(TAG_VIDEO_ID);
                if (vid == null || vid.length() == 0)  {
                    continue;
                }
                String releaseDateString = video.getString(TAG_RELEASE_DATE);
                String imageUrl = video.getString(TAG_IMAGE_URL);// + "?width=520&height=800";
                String thumbnailUrl = video.getString(TAG_IMAGE_URL) + "?width=480&height=270";

                String videoUrl = getVideoURL(vid);
                String title = video.getString(TAG_TITLE);

                // Get the Artist
                String artist = getArtist(video);

                // Make the current method happy
                String studio = "";
                mediaList.add(buildMediaInfo(title, studio, artist, videoUrl, thumbnailUrl,
                        imageUrl));
            }
        }
        return mediaList;
    }

    private static String getArtist(JSONObject video) throws JSONException{
        StringBuilder result = new StringBuilder();
        if (video == null) {
            return result.toString();
        }
        JSONArray artists = video.getJSONArray(TAG_ARTISTS);
        final int LAST_ITEM_IDX = artists.length() - 1;
        final boolean multipleArtists = artists.length() > 1;
        for (int i = 0; i < artists.length(); ++i) {
            JSONObject artistObj = artists.getJSONObject(i);
            String artist = artistObj.getString(TAG_ARTIST_NAME);
            if (artist != null) {
                if (multipleArtists) {
                    if (i == LAST_ITEM_IDX) {
                        result.append("and " + artist);
                    } else {
                        result.append(artist);
                        // it's a preeceding item. Append a comma if have more than 2 items
                        if (artists.length() > 2) {
                            result.append( ", ");
                        }
                    }
                } else {
                    // only have 1 artist for song.
                    result.append(artist);
                }
            }
        }
        return result.toString();
    }

    private static MediaInfo buildMediaInfo(String title,
            String subTitle, String studio, String url, String imgUrl, String bigImageUrl) {
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subTitle);
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);
        movieMetadata.putString(MediaMetadata.KEY_STUDIO, studio);
        movieMetadata.addImage(new WebImage(Uri.parse(imgUrl)));
        movieMetadata.addImage(new WebImage(Uri.parse(bigImageUrl)));

        return new MediaInfo.Builder(url)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(getMediaType())
                .setMetadata(movieMetadata)
                .build();
    }

    private static String getMediaType() {
        return "video/mp4";
    }

    private static String getJsonMediaTag() {
        return TAG_MEDIA;
    }

    private static String getThumbPrefix() {
        return THUMB_PREFIX_URL;
    }
}
