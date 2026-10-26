package com.botdarr.clients.mattermost;

import com.botdarr.Config;
import com.botdarr.clients.ChatClient;
import com.botdarr.clients.mattermost.transactions.*;
import com.botdarr.connections.ConnectionHelper;
import com.botdarr.connections.RequestBuilder;
import com.google.gson.Gson;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MattermostChatClient implements ChatClient<MattermostResponse> {
  public MattermostChatClient() {
    this.token = Config.getProperty(Config.Constants.MATTERMOST_TOKEN);
    MattermostUser botUser = getBotUser();
    if (botUser == null || Strings.isEmpty(botUser.getId())) {
      throw new RuntimeException("Could not get bot user from mattermost, please verify your mattermost url and token");
    }
    this.userId = botUser.getId();
  }

  @Override
  public void sendToConfiguredChannels(List<MattermostResponse> chatClientResponses) {
    for (MattermostResponse response : chatClientResponses) {
      for (String channelId : getChannels()) {
        sendMattermostResponse(response, channelId);
      }
    }
  }

  @Override
  public void cleanup() {
    // nothing to cleanup
  }

  public void sendMessage(MattermostResponse response, String channelId) {
    sendMattermostResponse(response, channelId);
  }

  public void addListener(MattermostMessageListener listener) {
    this.listeners.add(listener);
  }

  public String getUserId() {
    return userId;
  }

  /**
   * Maintains a websocket connection to mattermost, reconnecting as needed.
   * Blocks the calling thread forever.
   */
  public void listen() {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    //mattermost events can be larger than the default websocket text message buffer size
    container.setDefaultMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_SIZE);
    ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
            .configurator(new ClientEndpointConfig.Configurator() {
              @Override
              public void beforeRequest(Map<String, List<String>> headers) {
                //tyrus sends an http origin even for wss connections, which mattermost rejects with a 403
                headers.put("Origin", Collections.singletonList(getBaseUrl()));
              }
            })
            .build();
    while (true) {
      try {
        Session session = container.connectToServer(new MattermostWebSocketEndpoint(), endpointConfig, URI.create(getWebsocketUrl()));
        LOGGER.info("Connected to mattermost websocket api");
        while (session.isOpen()) {
          Thread.sleep(1000);
        }
        LOGGER.warn("Mattermost websocket connection closed, attempting to reconnect");
      } catch (Throwable t) {
        LOGGER.error("Error during mattermost websocket connection", t);
      }
      try {
        Thread.sleep(RECONNECT_INTERVAL_MS);
      } catch (InterruptedException e) {
        LOGGER.trace("Interrupted while waiting to reconnect to mattermost websocket api", e);
      }
    }
  }

  /**
   * Package private for testing purposes.
   */
  void processWebSocketMessage(String rawMessage) {
    MattermostWebSocketEvent event = new Gson().fromJson(rawMessage, MattermostWebSocketEvent.class);
    if (event == null) {
      return;
    }
    if (event.getEvent() == null) {
      //this is the response to our websocket authentication request
      if ("OK".equals(event.getStatus())) {
        LOGGER.info("Authenticated with mattermost websocket api");
      } else {
        LOGGER.error("Error authenticating with mattermost websocket api, status=" + event.getStatus());
      }
      return;
    }
    switch (event.getEvent()) {
      case "posted":
        processPostedEvent(event);
        break;
      case "reaction_added":
        processReactionAddedEvent(event);
        break;
    }
  }

  private void processPostedEvent(MattermostWebSocketEvent event) {
    Map<String, Object> data = event.getData();
    Object rawPost = data == null ? null : data.get("post");
    if (!(rawPost instanceof String) || Strings.isEmpty((String) rawPost)) {
      return;
    }
    //the post value of a posted event is itself a json string
    MattermostPost post = new Gson().fromJson((String) rawPost, MattermostPost.class);
    if (post == null || Strings.isEmpty(post.getMessage())) {
      return;
    }
    //ignore our own posts, otherwise we would respond to ourselves
    if (post.getUserId() != null && post.getUserId().equals(this.userId)) {
      return;
    }
    //ignore system posts (i.e., user joined the channel)
    if (!Strings.isEmpty(post.getType())) {
      return;
    }
    //ignore posts from other bots
    if (post.getProps() != null && "true".equals(post.getProps().get("from_bot"))) {
      return;
    }
    String channelId = post.getChannelId();
    if (Strings.isEmpty(channelId) && event.getBroadcast() != null) {
      channelId = event.getBroadcast().getChannelId();
    }
    //only process messages in configured channels
    if (channelId == null || !getChannels().contains(channelId)) {
      return;
    }
    //mattermost reserves / for its own slash commands
    if (post.getMessage().startsWith("/")) {
      return;
    }
    Object rawSender = data.get("sender_username");
    String sender = rawSender instanceof String ? (String) rawSender : null;
    if (Strings.isEmpty(sender)) {
      sender = post.getUserId();
    }
    for (MattermostMessageListener listener : this.listeners) {
      listener.process(channelId, sender, post.getMessage());
    }
  }

  private void processReactionAddedEvent(MattermostWebSocketEvent event) {
    Map<String, Object> data = event.getData();
    Object rawReaction = data == null ? null : data.get("reaction");
    if (!(rawReaction instanceof String) || Strings.isEmpty((String) rawReaction)) {
      return;
    }
    //the reaction value of a reaction_added event is itself a json string
    MattermostReaction reaction = new Gson().fromJson((String) rawReaction, MattermostReaction.class);
    if (reaction == null || Strings.isEmpty(reaction.getPostId())) {
      return;
    }
    //ignore our own reactions
    if (reaction.getUserId() != null && reaction.getUserId().equals(this.userId)) {
      return;
    }
    String channelId = reaction.getChannelId();
    if (Strings.isEmpty(channelId) && event.getBroadcast() != null) {
      channelId = event.getBroadcast().getChannelId();
    }
    if (channelId == null || !getChannels().contains(channelId)) {
      return;
    }
    for (MattermostMessageListener listener : this.listeners) {
      listener.reactionAdded(channelId, reaction.getUserId(), reaction.getPostId(), reaction.getEmojiName());
    }
  }

  public MattermostPost getPost(String postId) {
    return ConnectionHelper.makeGetRequest(new RequestBuilder()
      .host(getApiUrl(String.format(Constants.API_POST, postId)))
      .headers("Authorization", "Bearer " + this.token),
      new MattermostResponseHandler<MattermostPost>() {
        @Override
        public MattermostPost onSuccess(String response) throws Exception {
          return new Gson().fromJson(response, MattermostPost.class);
        }
      });
  }

  public String getUsername(String userId) {
    MattermostUser user = ConnectionHelper.makeGetRequest(new RequestBuilder()
      .host(getApiUrl(String.format(Constants.API_USER, userId)))
      .headers("Authorization", "Bearer " + this.token),
      new MattermostResponseHandler<MattermostUser>() {
        @Override
        public MattermostUser onSuccess(String response) throws Exception {
          return new Gson().fromJson(response, MattermostUser.class);
        }
      });
    if (user == null || Strings.isEmpty(user.getUsername())) {
      return userId;
    }
    return user.getUsername();
  }

  private MattermostUser getBotUser() {
    return ConnectionHelper.makeGetRequest(new RequestBuilder()
      .host(getApiUrl(Constants.API_BOT_USER))
      .headers("Authorization", "Bearer " + this.token),
      new MattermostResponseHandler<MattermostUser>() {
        @Override
        public MattermostUser onSuccess(String response) throws Exception {
          return new Gson().fromJson(response, MattermostUser.class);
        }
      });
  }

  private void sendMattermostResponse(MattermostResponse response, String channelId) {
    MattermostSendPostRequest sendPostRequest = new MattermostSendPostRequest();
    sendPostRequest.setChannelId(channelId);
    sendPostRequest.setMessage(response.getContent());
    MattermostPost post = ConnectionHelper.makeGetRequest(new RequestBuilder()
      .host(getApiUrl(Constants.API_POSTS))
      .headers("Authorization", "Bearer " + this.token)
      .post(new Gson().toJson(sendPostRequest)),
      new MattermostResponseHandler<MattermostPost>() {
        @Override
        public MattermostPost onSuccess(String response) throws Exception {
          return new Gson().fromJson(response, MattermostPost.class);
        }
      });
    if (post == null || Strings.isEmpty(post.getId())) {
      LOGGER.error("Could not send message to mattermost channel " + channelId);
    }
  }

  private class MattermostWebSocketEndpoint extends Endpoint {

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
      //mattermost requires authentication within 10 seconds of connecting
      session.getAsyncRemote().sendText(new Gson().toJson(new MattermostAuthRequest(MattermostChatClient.this.token)));
      session.addMessageHandler(String.class, message -> {
        try {
          MattermostChatClient.this.processWebSocketMessage(message);
        } catch (Throwable t) {
          LOGGER.error("Error processing mattermost websocket event", t);
        }
      });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
      LOGGER.warn("Mattermost websocket connection closed, reason=" + closeReason);
    }
  }

  private static abstract class MattermostResponseHandler<T> implements ConnectionHelper.ResponseHandler<T> {

    @Override
    public T onFailure(int statusCode, String reason) {
      LOGGER.error("Error from mattermost response, code=" + statusCode + ", reason=" + reason);
      if (statusCode == 429) {
        LOGGER.error("Rate limited from mattermost");
      }
      return null;
    }

    @Override
    public T onException(Exception e) {
      LOGGER.error("Error trying to make mattermost request", e);
      return null;
    }

    @Override
    public T onConnectException(HttpHostConnectException e) {
      return onException(e);
    }
  }

  private List<String> getChannels() {
    String rawChannelsStr = Config.getProperty(Config.Constants.MATTERMOST_CHANNELS);
    List<String> channels = new ArrayList<>();
    if (rawChannelsStr != null) {
      for (String channel : rawChannelsStr.split(",")) {
        if (!Strings.isBlank(channel)) {
          channels.add(channel.trim());
        }
      }
    }
    return channels;
  }

  private String getApiUrl(String path) {
    //all endpoint constants already include the api base
    return getBaseUrl() + path;
  }

  private String getWebsocketUrl() {
    String url = getBaseUrl();
    //http -> ws, https -> wss
    if (url.startsWith("https://")) {
      url = "wss://" + url.substring("https://".length());
    } else if (url.startsWith("http://")) {
      url = "ws://" + url.substring("http://".length());
    }
    return url + Constants.API_BASE + Constants.API_WEBSOCKET;
  }

  private String getBaseUrl() {
    String url = Config.getProperty(Config.Constants.MATTERMOST_URL);
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  protected static class Constants {
    private static final String API_BASE = "/api/v4";
    private static final String API_BOT_USER = API_BASE + "/users/me";
    /** 1 == user id */
    private static final String API_USER = API_BASE + "/users/%s";
    private static final String API_POSTS = API_BASE + "/posts";
    /** 1 == post id */
    private static final String API_POST = API_BASE + "/posts/%s";
    private static final String API_WEBSOCKET = "/websocket";
  }

  public interface MattermostMessageListener {
    void process(String channelId, String sender, String content);
    void reactionAdded(String channelId, String userId, String postId, String emojiName);
  }

  private final List<MattermostMessageListener> listeners = new ArrayList<>();
  private final String token;
  private final String userId;
  private static final int MAX_TEXT_MESSAGE_BUFFER_SIZE = 262144;
  private static final int RECONNECT_INTERVAL_MS = 5000;
  private static final Logger LOGGER = LogManager.getLogger();
}
