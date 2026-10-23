package com.botdarr.clients.mattermost.transactions;

/**
 * A mattermost user.
 * https://api.mattermost.com/#tag/users
 */
public class MattermostUser {
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  private String id;
  private String username;
}
