package uk.openvk.android.legacy.api.models;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;

import uk.openvk.android.legacy.R;
import uk.openvk.android.legacy.api.attachments.Attachment;
import uk.openvk.android.legacy.api.attachments.PhotoAttachment;
import uk.openvk.android.legacy.api.attachments.PollAttachment;
import uk.openvk.android.legacy.api.attachments.VideoAttachment;
import uk.openvk.android.legacy.api.entities.Comment;
import uk.openvk.android.legacy.api.entities.PollAnswer;
import uk.openvk.android.legacy.api.entities.VideoFiles;
import uk.openvk.android.legacy.api.entities.WallPostSource;
import uk.openvk.android.legacy.api.wrappers.DownloadManager;
import uk.openvk.android.legacy.api.wrappers.JSONParser;
import uk.openvk.android.legacy.api.wrappers.OvkAPIWrapper;
import uk.openvk.android.legacy.api.counters.PostCounters;
import uk.openvk.android.legacy.api.entities.WallPost;
import uk.openvk.android.legacy.api.entities.RepostInfo;

/** OPENVK LEGACY LICENSE NOTIFICATION
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU Affero General Public License as published by the Free Software Foundation, either
 *  version 3 of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this
 *  program. If not, see https://www.gnu.org/licenses/.
 *
 *  Source code: https://github.com/openvk/mobile-android-legacy
 **/

public class Wall implements Parcelable {
    private JSONParser jsonParser;
    private ArrayList<WallPost> items;
    private ArrayList<Comment> comments;
    private ArrayList<PhotoAttachment> photos_msize;
    private ArrayList<PhotoAttachment> photos_hsize;
    private ArrayList<PhotoAttachment> photos_osize;

    public Wall(String response, DownloadManager downloadManager, String quality, Context ctx) {
        jsonParser = new JSONParser();
        parse(ctx, downloadManager, quality, response);
    }

    public Wall() {
        jsonParser = new JSONParser();
    }

    protected Wall(Parcel in) {
        items = in.createTypedArrayList(WallPost.CREATOR);
    }

    public static final Creator<Wall> CREATOR = new Creator<Wall>() {
        @Override
        public Wall createFromParcel(Parcel in) {
            return new Wall(in);
        }

        @Override
        public Wall[] newArray(int size) {
            return new Wall[size];
        }
    };

    public void parse(Context ctx, DownloadManager downloadManager, String quality, String response) {
        items = new ArrayList<WallPost>();
        photos_msize = new ArrayList<PhotoAttachment>();
        photos_hsize = new ArrayList<PhotoAttachment>();
        photos_osize = new ArrayList<PhotoAttachment>();
        ArrayList<PhotoAttachment> avatars = new ArrayList<PhotoAttachment>();
        try {
            JSONObject json = jsonParser.parseJSON(response);
            if(json != null) {
                JSONObject newsfeed = json.getJSONObject("response");
                JSONArray items = newsfeed.getJSONArray("items");
                for(int i = 0; i < items.length(); i++) {
                    JSONObject post = items.getJSONObject(i);
                    JSONObject comments = post.getJSONObject("comments");
                    JSONObject likes = post.getJSONObject("likes");
                    JSONObject reposts = post.getJSONObject("reposts");
                    JSONArray attachments = post.getJSONArray("attachments");
                    long owner_id = post.getLong("owner_id");
                    long post_id = post.getLong("id");
                    long author_id = post.getLong("from_id");
                    long dt_sec = post.getLong("date");
                    String original_author_name = "";
                    String original_author_avatar_url = "";
                    String author_name = "";
                    String owner_name = "";
                    String avatar_url = "";
                    String owner_avatar_url = "";
                    String author_avatar_url = "";
                    String content = post.getString("text");
                    boolean isLiked = false;
                    boolean verified_author = false;
                    if(likes.getInt("user_likes") > 0) {
                        isLiked = true;
                    } else {
                        isLiked = false;
                    }
                    PostCounters counters = new PostCounters(likes.getInt("count"), comments.getInt("count"),
                            reposts.getInt("count"), isLiked, false);

                    ArrayList<Attachment> attachments_list = createAttachmentsList(owner_id, post_id, quality,
                            attachments, "wall_attachment");

                    WallPost item = new WallPost(String.format("(Unknown author: %s)", author_id), dt_sec, null,
                            content, counters, "", attachments_list, owner_id, post_id, ctx);
                    if(post.has("post_source") && !post.isNull("post_source")) {
                        if(post.getJSONObject("post_source").getString("type").equals("api")) {
                            item.post_source = new WallPostSource(post.getJSONObject("post_source").getString("type"),
                                    post.getJSONObject("post_source").getString("platform"));
                        } else {
                            item.post_source = new WallPostSource(post.getJSONObject("post_source").getString("type"), null);
                        }
                    }
                    if(post.getJSONArray("copy_history").length() > 0) {
                        JSONObject repost = post.getJSONArray("copy_history").getJSONObject(0);
                        WallPost repost_item = new WallPost(String.format("(Unknown author: %s)",
                                repost.getInt("from_id")),
                                repost.getInt("date"), null, repost.getString("text"), null, "",
                                null, repost.getInt("owner_id"), repost.getInt("id"), ctx);
                        RepostInfo repostInfo = new RepostInfo(String.format("(Unknown author: %s)",
                                repost.getInt("from_id")),
                                repost.getInt("date"), ctx);
                        repostInfo.newsfeed_item = repost_item;
                        item.repost = repostInfo;
                        JSONArray repost_attachments = repost.getJSONArray("attachments");
                        attachments_list = createAttachmentsList(owner_id, post_id, quality,
                                repost_attachments, "wall_attachment");
                        repost_item.attachments = attachments_list;
                    }
                    item.author_id = author_id;
                    if(author_id > 0) {
                        if(newsfeed.has("profiles")) {
                            JSONArray profiles = newsfeed.getJSONArray("profiles");
                            for (int profiles_index = 0; profiles_index < profiles.length(); profiles_index++) {
                                JSONObject profile = profiles.getJSONObject(profiles_index);
                                if (profile.getInt("id") == author_id) {
                                    author_name = String.format("%s %s", profile.getString("first_name"),
                                            profile.getString("last_name"));
                                    author_avatar_url = profile.getString("photo_50");
                                    if(profile.has("verified")) {
                                        if(profile.get("verified") instanceof Integer) {
                                            verified_author = profile.getInt("verified") == 1;
                                        } else {
                                            verified_author = profile.getBoolean("verified");
                                        }
                                    }
                                } else if (profile.getInt("id") == owner_id) {
                                    owner_name = String.format("%s %s", profile.getString("first_name"),
                                            profile.getString("last_name"));
                                    owner_avatar_url = profile.getString("photo_50");
                                    if(profile.has("verified")) {
                                        if(profile.get("verified") instanceof Integer) {
                                            verified_author = profile.getInt("verified") == 1;
                                        } else {
                                            verified_author = profile.getBoolean("verified");
                                        }
                                    }
                                }
                            }
                            if(author_avatar_url.length() > 0)
                                avatar_url = author_avatar_url;
                        }
                        if(owner_id < 0) {
                            if(newsfeed.has("groups")) {
                                JSONArray groups = newsfeed.getJSONArray("groups");
                                for (int groups_index = 0; groups_index < groups.length(); groups_index++) {
                                    JSONObject group = groups.getJSONObject(groups_index);
                                    if (-group.getInt("id") == owner_id) {
                                        owner_name = group.getString("name");
                                        avatar_url = group.getString("photo_50");
                                        if(group.get("verified") instanceof Integer) {
                                            if (group.getInt("verified") == 1) {
                                                verified_author = true;
                                            } else {
                                                verified_author = false;
                                            }
                                        } else {
                                            if (group.getBoolean("verified")) {
                                                verified_author = true;
                                            } else {
                                                verified_author = false;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if(author_id == owner_id) {
                            item.name = author_name;
                        } else {
                            item.name = ctx.getResources().getString(R.string.on_wall, author_name, owner_name);
                        }
                    } else {
                        if(newsfeed.has("groups") && newsfeed.has("profiles")) {
                            JSONArray groups = newsfeed.getJSONArray("groups");
                            JSONArray profiles = newsfeed.getJSONArray("profiles");
                            for (int groups_index = 0; groups_index < groups.length(); groups_index++) {
                                JSONObject group = groups.getJSONObject(groups_index);
                                if (-group.getInt("id") == author_id) {
                                    item.name = group.getString("name");
                                    avatar_url = group.getString("photo_50");
                                    if(group.has("verified")) {
                                        if(group.get("verified") instanceof Integer) {
                                            if (group.getInt("verified") == 1) {
                                                verified_author = true;
                                            } else {
                                                verified_author = false;
                                            }
                                        } else {
                                            if (group.getBoolean("verified")) {
                                                verified_author = true;
                                            } else {
                                                verified_author = false;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PhotoAttachment avatar = new PhotoAttachment();
                    avatar.url = avatar_url;
                    avatar.filename = String.format("avatar_%s", author_id);
                    avatars.add(avatar);
                    item.verified_author = verified_author;
                    this.items.add(item);
                }
                switch (quality) {
                    case "medium":
                        downloadManager.downloadPhotosToCache(photos_msize, "wall_photo_attachments");
                        break;
                    case "high":
                        downloadManager.downloadPhotosToCache(photos_hsize, "wall_photo_attachments");
                        break;
                    case "original":
                        downloadManager.downloadPhotosToCache(photos_osize, "wall_photo_attachments");
                        break;
                }
                downloadManager.downloadPhotosToCache(avatars, "wall_avatars");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Comment> parseComments(Context ctx, DownloadManager downloadManager, String quality,
                                            String response) {
        comments = new ArrayList<Comment>();
        photos_msize = new ArrayList<PhotoAttachment>();
        photos_hsize = new ArrayList<PhotoAttachment>();
        photos_osize = new ArrayList<PhotoAttachment>();
        try {
            JSONObject json = jsonParser.parseJSON(response);
            if (json != null) {
                JSONObject comments = json.getJSONObject("response");
                JSONArray items = comments.getJSONArray("items");
                ArrayList<PhotoAttachment> avatars = new ArrayList<>();
                for(int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    String text = item.getString("text");
                    long comment_id = item.getLong("id");
                    long author_id = item.getLong("from_id");
                    long date = item.getLong("date");
                    JSONArray attachments = items.getJSONObject(i).getJSONArray("attachments");
                    ArrayList<Attachment> attachments_list = createAttachmentsList(author_id, comment_id,
                            quality, attachments, "comment_photo");
                    Comment comment = new Comment();
                    comment.id = comment_id;
                    comment.author_id = author_id;
                    comment.text = text;
                    comment.author = String.format("(Unknown author: %s)", author_id);
                    comment.date = date;
                    PhotoAttachment photoAttachment = new PhotoAttachment();
                    photoAttachment.url = "";
                    photoAttachment.filename = "";
                    if(author_id > 0) {
                        if(comments.has("profiles")) {
                            JSONArray profiles = comments.getJSONArray("profiles");
                            for (int profiles_index = 0; profiles_index < profiles.length(); profiles_index++) {
                                JSONObject profile = profiles.getJSONObject(profiles_index);
                                if (profile.getLong("id") == author_id) {
                                    comment.author = String.format("%s %s", profile.getString("first_name"),
                                            profile.getString("last_name"));
                                    if(profile.has("photo_100")) {
                                        comment.avatar_url = profile.getString("photo_100");
                                    }
                                    photoAttachment.url = comment.avatar_url;
                                    photoAttachment.filename = String.format("avatar_%s", author_id);
                                }
                            }
                        }
                    } else {
                        if(comments.has("groups")) {
                            JSONArray groups = comments.getJSONArray("groups");
                            for (int groups_index = 0; groups_index < groups.length(); groups_index++) {
                                JSONObject group = groups.getJSONObject(groups_index);
                                if (group.getLong("id") == author_id) {
                                    comment.author = group.getString("name");
                                    if(group.has("photo_100")) {
                                        comment.avatar_url = group.getString("photo_100");
                                    }
                                    photoAttachment.url = comment.avatar_url;
                                    photoAttachment.filename = String.format("avatar_%s", author_id);
                                }
                            }
                        }
                    }
                    comment.attachments = attachments_list;
                    avatars.add(photoAttachment);
                    this.comments.add(comment);
                }
                switch (quality) {
                    case "medium":
                        downloadManager.downloadPhotosToCache(photos_msize, "comment_photos");
                        break;
                    case "high":
                        downloadManager.downloadPhotosToCache(photos_hsize, "comment_photos");
                        break;
                    case "original":
                        downloadManager.downloadPhotosToCache(photos_osize, "comment_photos");
                        break;
                }
                downloadManager.downloadPhotosToCache(avatars, "comment_avatars");
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
        return comments;
    }

    public ArrayList<Attachment> createAttachmentsList(long owner_id, long post_id, String quality,
                                                       JSONArray attachments, String prefix) {
        ArrayList<Attachment> attachments_list = new ArrayList<>();
        try {
            for (int attachments_index = 0; attachments_index < attachments.length(); attachments_index++) {
                String photo_medium_size;
                String photo_high_size;
                String photo_original_size;
                String attachment_status;
                JSONObject attachment = attachments.getJSONObject(attachments_index);
                if (attachment.getString("type").equals("photo")) {
                    JSONObject photo = attachment.getJSONObject("photo");
                    PhotoAttachment photoAttachment = new PhotoAttachment();
                    photoAttachment.id = photo.getLong("id");
                    JSONArray photo_sizes = photo.getJSONArray("sizes");
                    photo_medium_size = photo_sizes.getJSONObject(5).getString("url");
                    photo_high_size = photo_sizes.getJSONObject(8).getString("url");
                    photo_original_size = photo_sizes.getJSONObject(10).getString("url");
                    switch (quality) {
                        case "medium":
                            photoAttachment.url = photo_medium_size;
                            break;
                        case "high":
                            photoAttachment.url = photo_high_size;
                            break;
                        case "original":
                            photoAttachment.url = photo_original_size;
                            break;
                    }
                    photoAttachment.filename = String.format("%s_o%sp%s", prefix, owner_id, post_id);
                    photoAttachment.original_url = photo_original_size;
                    if (photo_medium_size.length() > 0 || photo_high_size.length() > 0) {
                        attachment_status = "loading";
                    } else {
                        attachment_status = "none";
                    }
                    Attachment attachment_obj = new Attachment(attachment.getString("type"));
                    attachment_obj.status = attachment_status;
                    attachment_obj.setContent(photoAttachment);
                    attachments_list.add(attachment_obj);
                    if(quality.equals("medium")) {
                        photos_msize.add(photoAttachment);
                    } else if(quality.equals("high")) {
                        photos_hsize.add(photoAttachment);
                    } else if(quality.equals("original")) {
                        photos_osize.add(photoAttachment);
                    }
                } else if (attachment.getString("type").equals("video")) {
                    JSONObject video = attachment.getJSONObject("video");
                    VideoAttachment videoAttachment = new VideoAttachment();
                    videoAttachment.id = video.getLong("id");
                    videoAttachment.title = video.getString("title");
                    VideoFiles files = new VideoFiles();
                    if(video.has("files")) {
                        JSONObject videoFiles = video.getJSONObject("files");
                        if(videoFiles.has("mp4_144")) {
                            files.mp4_144 = videoFiles.getString("mp4_144");
                        } if(videoFiles.has("mp4_240")) {
                            files.mp4_240 = videoFiles.getString("mp4_240");
                        } if(videoFiles.has("mp4_360")) {
                            files.mp4_360 = videoFiles.getString("mp4_360");
                        } if(videoFiles.has("mp4_480")) {
                            files.mp4_480 = videoFiles.getString("mp4_480");
                        } if(videoFiles.has("mp4_720")) {
                            files.mp4_720 = videoFiles.getString("mp4_720");
                        } if(videoFiles.has("mp4_1080")) {
                            files.mp4_1080 = videoFiles.getString("mp4_1080");
                        } if(videoFiles.has("ogv_480")) {
                            files.ogv_480 = videoFiles.getString("ogv_480");
                        }
                    }
                    videoAttachment.files = files;
                    if(video.has("image")) {
                        JSONArray thumb_array = video.getJSONArray("image");
                        videoAttachment.url_thumb = thumb_array.getJSONObject(0).getString("url");
                    }
                    videoAttachment.duration = video.getInt("duration");
                    attachment_status = "done";
                    Attachment attachment_obj = new Attachment(attachment.getString("type"));
                    attachment_obj.status = attachment_status;
                    attachment_obj.setContent(videoAttachment);
                    attachments_list.add(attachment_obj);
                } else if (attachment.getString("type").equals("poll")) {
                    JSONObject poll_attachment = attachment.getJSONObject("poll");
                    PollAttachment pollAttachment = new PollAttachment(poll_attachment.getString("question"),
                            poll_attachment.getInt("id"), poll_attachment.getLong("end_date"),
                            poll_attachment.getBoolean("multiple"),
                            poll_attachment.getBoolean("can_vote"),
                            poll_attachment.getBoolean("anonymous"));
                    JSONArray answers = poll_attachment.getJSONArray("answers");
                    JSONArray votes = poll_attachment.getJSONArray("answer_ids");
                    if (votes.length() > 0) {
                        pollAttachment.user_votes = votes.length();
                    }
                    pollAttachment.votes = poll_attachment.getInt("votes");
                    for (int answers_index = 0; answers_index < answers.length(); answers_index++) {
                        JSONObject answer = answers.getJSONObject(answers_index);
                        PollAnswer pollAnswer = new PollAnswer(answer.getInt("id"), answer.getInt("rate"),
                                answer.getInt("votes"), answer.getString("text"));
                        for (int votes_index = 0; votes_index < votes.length(); votes_index++) {
                            if (answer.getInt("id") == votes.getInt(votes_index)) {
                                pollAnswer.is_voted = true;
                            }
                        }
                        pollAttachment.answers.add(pollAnswer);
                    }
                    attachment_status = "done";
                    Attachment attachment_obj = new Attachment(attachment.getString("type"));
                    attachment_obj.status = attachment_status;
                    attachment_obj.setContent(pollAttachment);
                    attachments_list.add(attachment_obj);
                } else {
                    attachment_status = "not_supported";
                    Attachment attachment_obj = new Attachment(attachment.getString("type"));
                    attachment_obj.status = attachment_status;
                    attachments_list.add(attachment_obj);
                }
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return attachments_list;
    }

    public void get(OvkAPIWrapper ovk, long owner_id, int count) {
        ovk.sendAPIMethod("Wall.get", String.format("owner_id=%s&count=%s&extended=1",
                owner_id, count));
    }

    public ArrayList<WallPost> getWallItems() {
        return items;
    }

    public void post(OvkAPIWrapper ovk, long owner_id, String post) {
        ovk.sendAPIMethod("Wall.post", String.format("owner_id=%s&message=%s",
                owner_id, URLEncoder.encode(post)));
    }

    public void getComments(OvkAPIWrapper ovk, long owner_id, long post_id) {
        ovk.sendAPIMethod("Wall.getComments", String.format("owner_id=%s&post_id=%s&extended=1&count=50",
                owner_id, post_id));
    }

    public void createComment(OvkAPIWrapper ovk, long owner_id, long post_id, String text) {
        ovk.sendAPIMethod("Wall.createComment", String.format("owner_id=%s&post_id=%s&message=%s", owner_id,
                post_id, URLEncoder.encode(text)));
    }

    public void repost(OvkAPIWrapper ovk, long owner_id, long post_id, String text) {
        ovk.sendAPIMethod("Wall.repost", String.format("object=wall%s_%s&message=%s", owner_id,
                post_id, URLEncoder.encode(text)));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(items);
    }
}