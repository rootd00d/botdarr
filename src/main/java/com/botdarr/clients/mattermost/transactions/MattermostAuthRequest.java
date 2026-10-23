package com.botdarr.clients.mattermost.transactions;

/**
 * Authentication request that must be sent within 10 seconds of connecting
 * to the mattermost websocket api.
 * https://api.mattermost.com/#tag/websocket
 */
public class MattermostAuthRequest {
  public MattermostAuthRequest(String token) {
    this.data = new MattermostAuthData(token);
  }

  @SuppressWarnings("unused")
  private final int seq = 1;
  @SuppressWarnings("unused")
  private final String action = "authentication_challenge";
  @SuppressWarnings("unused")
  private final MattermostAuthData data;

  private static class MattermostAuthData {
    @SuppressWarnings("unused")
    private final String token;

    private MattermostAuthData(String token) {
      this.token = token;
    }
  }
}
