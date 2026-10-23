package com.botdarr.clients.mattermost.transactions;

import com.google.gson.annotations.SerializedName;

/**
 * A mattermost reaction on a post.
 * https://api.mattermost.com/#tag/reactions
 */
public class MattermostReaction {
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getPostId() {
    return postId;
  }

  public void setPostId(String postId) {
    this.postId = postId;
  }

  public String getEmojiName() {
    return emojiName;
  }

  public void setEmojiName(String emojiName) {
    this.emojiName = emojiName;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  @SerializedName("user_id")
  private String userId;
  @SerializedName("post_id")
  private String postId;
  @SerializedName("emoji_name")
  private String emojiName;
  @SerializedName("channel_id")
  private String channelId;
}
