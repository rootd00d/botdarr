package com.botdarr.clients.mattermost;

import com.botdarr.Config;
import com.google.gson.Gson;
import mockit.Deencapsulation;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MattermostChatClientTests {
  @Before
  public void beforeEachTest() {
    writeFakePropertiesFile(getDefaultProperties());
    stubBotUser();
  }

  @After
  public void afterEachTest() {
    mockServerRule.getClient().reset();
  }

  @Test
  public void constructor_validatesBotToken() {
    stubBotUser();
    MattermostChatClient chatClient = new MattermostChatClient();
    Assert.assertEquals("bot-user-id", chatClient.getUserId());
    mockServerRule.getClient().verify(
      HttpRequest.request()
        .withMethod("GET")
        .withPath("/api/v4/users/me")
        .withHeader("Authorization", "Bearer " + BOT_TOKEN));
  }

  @Test(expected = RuntimeException.class)
  public void constructor_throwsWhenBotUserLookupFails() {
    mockServerRule.getClient().reset();
    mockServerRule.getClient()
      .when(HttpRequest.request().withMethod("GET").withPath("/api/v4/users/me"))
      .respond(HttpResponse.response().withStatusCode(401));
    new MattermostChatClient();
  }

  @Test
  public void postedEvent_dispatchedToListener() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> post = getPost("post-id", "channel1", "some-user-id", "!help");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("post", new Gson().toJson(post));
    data.put("sender_username", "someuser");
    chatClient.processWebSocketMessage(buildEvent("posted", data, "channel1"));

    Assert.assertEquals(1, listener.messages.size());
    Assert.assertEquals("channel1", listener.messages.get(0)[0]);
    Assert.assertEquals("someuser", listener.messages.get(0)[1]);
    Assert.assertEquals("!help", listener.messages.get(0)[2]);
  }

  @Test
  public void postedEvent_fromBotdarrIgnored() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> post = getPost("post-id", "channel1", "bot-user-id", "!help");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("post", new Gson().toJson(post));
    chatClient.processWebSocketMessage(buildEvent("posted", data, "channel1"));

    Assert.assertEquals(0, listener.messages.size());
  }

  @Test
  public void postedEvent_fromOtherBotIgnored() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> post = getPost("post-id", "channel1", "some-user-id", "!help");
    post.put("props", Collections.singletonMap("from_bot", "true"));
    Map<String, String> data = new LinkedHashMap<>();
    data.put("post", new Gson().toJson(post));
    chatClient.processWebSocketMessage(buildEvent("posted", data, "channel1"));

    Assert.assertEquals(0, listener.messages.size());
  }

  @Test
  public void postedEvent_systemMessageIgnored() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> post = getPost("post-id", "channel1", "some-user-id", "user joined the channel");
    post.put("type", "system_join_channel");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("post", new Gson().toJson(post));
    chatClient.processWebSocketMessage(buildEvent("posted", data, "channel1"));

    Assert.assertEquals(0, listener.messages.size());
  }

  @Test
  public void postedEvent_unconfiguredChannelIgnored() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> post = getPost("post-id", "unconfigured-channel", "some-user-id", "!help");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("post", new Gson().toJson(post));
    chatClient.processWebSocketMessage(buildEvent("posted", data, "unconfigured-channel"));

    Assert.assertEquals(0, listener.messages.size());
  }

  @Test
  public void postedEvent_slashCommandIgnored() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> post = getPost("post-id", "channel1", "some-user-id", "/join some-channel");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("post", new Gson().toJson(post));
    chatClient.processWebSocketMessage(buildEvent("posted", data, "channel1"));

    Assert.assertEquals(0, listener.messages.size());
  }

  @Test
  public void reactionAddedEvent_dispatchedToListener() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> reaction = new LinkedHashMap<>();
    reaction.put("user_id", "some-user-id");
    reaction.put("post_id", "post-id");
    reaction.put("emoji_name", "+1");
    reaction.put("channel_id", "channel1");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("reaction", new Gson().toJson(reaction));
    chatClient.processWebSocketMessage(buildEvent("reaction_added", data, "channel1"));

    Assert.assertEquals(1, listener.reactions.size());
    Assert.assertEquals("channel1", listener.reactions.get(0)[0]);
    Assert.assertEquals("some-user-id", listener.reactions.get(0)[1]);
    Assert.assertEquals("post-id", listener.reactions.get(0)[2]);
    Assert.assertEquals("+1", listener.reactions.get(0)[3]);
  }

  @Test
  public void reactionAddedEvent_unconfiguredChannelIgnored() {
    MattermostChatClient chatClient = new MattermostChatClient();
    TestListener listener = new TestListener();
    chatClient.addListener(listener);

    Map<String, Object> reaction = new LinkedHashMap<>();
    reaction.put("user_id", "some-user-id");
    reaction.put("post_id", "post-id");
    reaction.put("emoji_name", "+1");
    reaction.put("channel_id", "unconfigured-channel");
    Map<String, String> data = new LinkedHashMap<>();
    data.put("reaction", new Gson().toJson(reaction));
    chatClient.processWebSocketMessage(buildEvent("reaction_added", data, "unconfigured-channel"));

    Assert.assertEquals(0, listener.reactions.size());
  }

  @Test
  public void sendMessage_postsToChannel() {
    MattermostChatClient chatClient = new MattermostChatClient();

    HttpRequest request = HttpRequest.request()
      .withMethod("POST")
      .withPath("/api/v4/posts")
      .withHeader("Authorization", "Bearer " + BOT_TOKEN)
      .withBody(JsonBody.json("{\"channel_id\":\"channel1\",\"message\":\"hello\",\"props\":{\"from_bot\":\"true\"}}"));
    mockServerRule.getClient()
      .when(request)
      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withBody("{\"id\":\"new-post-id\"}", MediaType.APPLICATION_JSON));

    MattermostResponse response = new MattermostResponse();
    response.addContent("hello");
    chatClient.sendMessage(response, "channel1");

    mockServerRule.getClient().verify(request);
  }

  @Test
  public void getPost_returnsPost() {
    MattermostChatClient chatClient = new MattermostChatClient();

    Map<String, Object> post = getPost("post-id", "channel1", "some-user-id", "!help");
    mockServerRule.getClient()
      .when(HttpRequest.request().withMethod("GET").withPath("/api/v4/posts/post-id"))
      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withBody(new Gson().toJson(post), MediaType.APPLICATION_JSON));

    Assert.assertEquals("!help", chatClient.getPost("post-id").getMessage());
  }

  private void stubBotUser() {
    mockServerRule.getClient()
      .when(HttpRequest.request().withMethod("GET").withPath("/api/v4/users/me"))
      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withBody("{\"id\":\"bot-user-id\",\"username\":\"botdarr\"}", MediaType.APPLICATION_JSON));
  }

  private Map<String, Object> getPost(String id, String channelId, String userId, String message) {
    Map<String, Object> post = new LinkedHashMap<>();
    post.put("id", id);
    post.put("channel_id", channelId);
    post.put("user_id", userId);
    post.put("message", message);
    return post;
  }

  private String buildEvent(String eventName, Map<String, String> data, String channelId) {
    Map<String, Object> event = new LinkedHashMap<>();
    event.put("event", eventName);
    event.put("data", data);
    Map<String, Object> broadcast = new LinkedHashMap<>();
    broadcast.put("channel_id", channelId);
    event.put("broadcast", broadcast);
    return new Gson().toJson(event);
  }

  private static class TestListener implements MattermostChatClient.MattermostMessageListener {
    private final List<String[]> messages = new ArrayList<>();
    private final List<String[]> reactions = new ArrayList<>();

    @Override
    public void process(String channelId, String sender, String content) {
      messages.add(new String[] {channelId, sender, content});
    }

    @Override
    public void reactionAdded(String channelId, String userId, String postId, String emojiName) {
      reactions.add(new String[] {channelId, userId, postId, emojiName});
    }
  }

  private Properties getDefaultProperties() {
    Properties properties = new Properties();
    properties.setProperty("mattermost-url", "http://localhost:" + mockServerRule.getPort());
    properties.setProperty("mattermost-token", BOT_TOKEN);
    properties.setProperty("mattermost-channels", "channel1,channel2");
    return properties;
  }

  private void writeFakePropertiesFile(Properties properties) {
    Deencapsulation.setField(Config.class, "instance", null);
    File propertiesFile = null;
    try {
      new File(temporaryFolder.getRoot(), "properties").delete();
      propertiesFile = temporaryFolder.newFile("properties");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Deencapsulation.setField(Config.class, "propertiesPath", propertiesFile.getPath());
    try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
      properties.store(fos, "");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Rule
  public MockServerRule mockServerRule = new MockServerRule(this);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String BOT_TOKEN = "FSJDkjmf#$Kf3";
}
