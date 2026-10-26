package com.botdarr.clients.mattermost.transactions;

import java.util.Map;

/**
 * Envelope of every message sent over the mattermost websocket api.
 * Event payloads live in the data map, and some of the values are themselves
 * json strings that need to be parsed again (i.e., the post value of a posted event).
 * Some events (i.e., status_change) carry object values in the data map, so values
 * must be widened to Object and checked before use.
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

  public Map<String, Object> getData() {
    return data;
  }

  public MattermostEventBroadcast getBroadcast() {
    return broadcast;
  }

  private String event;
  private String status;
  private int seq;
  private Map<String, Object> data;
  private MattermostEventBroadcast broadcast;
}
