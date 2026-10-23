package com.botdarr.clients.mattermost.transactions;

/**
 * Authentication request that must be sent within 10 seconds of connecting
 * to the mattermost websocket api.
 * https://api.mattermost.com/#tag/websocket
 */
public class MattermostAuthRequest {
  public MattermostAuthRequest(String token) {
    this.token = token;
  }

  @SuppressWarnings("unused")
  private final String action = "authentication";
  @SuppressWarnings("unused")
  private final String token;
  @SuppressWarnings("unused")
  private final int seq = 1;
}
