package com.botdarr.clients.mattermost.transactions;

import java.util.Map;

/**
 * Envelope of every message sent over the mattermost websocket api.
 * Event payloads live in the data map, and some of the values are themselves
 * json strings that need to be parsed again (i.e., the post value of a posted event).
 * https://api.mattermost.com/#tag/websocket
 */
public class MattermostWebSocketEvent {
  /**
   * Event name (i.e., posted, reaction_added, hello), null for authentication responses
   */
  public String getEvent() {
    return event;
  }

  /**
   * OK or FAIL, only set for authentication responses
   */
  public String getStatus() {
    return status;
  }

  public int getSeq() {
    return seq;
  }

  public Map<String, String> getData() {
    return data;
  }

  public MattermostEventBroadcast getBroadcast() {
    return broadcast;
  }

  private String event;
  private String status;
  private int seq;
  private Map<String, String> data;
  private MattermostEventBroadcast broadcast;
}
