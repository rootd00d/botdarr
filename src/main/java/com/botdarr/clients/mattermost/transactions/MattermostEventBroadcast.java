package com.botdarr.clients.mattermost.transactions;

import com.google.gson.annotations.SerializedName;

/**
 * Broadcast information of a mattermost websocket event.
 * https://api.mattermost.com/#tag/websocket
 */
public class MattermostEventBroadcast {
  public String getUserId() {
    return userId;
  }

  public String getChannelId() {
    return channelId;
  }

  @SerializedName("user_id")
  private String userId;
  @SerializedName("channel_id")
  private String channelId;
}
