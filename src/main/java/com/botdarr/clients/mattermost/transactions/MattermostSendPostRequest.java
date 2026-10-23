package com.botdarr.clients.mattermost.transactions;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Request body for creating a post via the mattermost rest api.
 * https://api.mattermost.com/#tag/posts
 */
public class MattermostSendPostRequest {
  public MattermostSendPostRequest() {
    //let mattermost (and other clients) know this post came from a bot
    this.props.put("from_bot", "true");
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @SerializedName("channel_id")
  private String channelId;
  private String message;
  private Map<String, String> props = new HashMap<>();
}
