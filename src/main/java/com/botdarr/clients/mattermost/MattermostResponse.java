package com.botdarr.clients.mattermost;

import com.botdarr.clients.ChatClientResponse;

public class MattermostResponse implements ChatClientResponse {
  public void addContent(String content) {
    this.content.append(content).append("\n");
  }

  public void addImage(String imageUrl) {
    if (imageUrl == null || imageUrl.isEmpty()) {
      return;
    }
    this.content.append("![](").append(imageUrl).append(")\n");
  }

  public String getContent() {
    return this.content.toString().trim();
  }

  private final StringBuilder content = new StringBuilder();
}
