package com.botdarr.clients.mattermost.transactions;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * A mattermost post.
 * https://api.mattermost.com/#tag/posts
 */
public class MattermostPost {
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Empty for user-created posts, system_* for system messages (i.e., user joined channel)
   */
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  /**
   * Post properties, i.e., from_bot is set for posts created by bots
   */
  public Map<String, String> getProps() {
    return props;
  }

  public void setProps(Map<String, String> props) {
    this.props = props;
  }

  private String id;
  @SerializedName("channel_id")
  private String channelId;
  @SerializedName("user_id")
  private String userId;
  private String message;
  private String type;
  private Map<String, String> props;
}
