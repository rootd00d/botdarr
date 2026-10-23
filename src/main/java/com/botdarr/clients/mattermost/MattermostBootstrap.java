package com.botdarr.clients.mattermost;

import com.botdarr.Config;
import com.botdarr.clients.ChatClientBootstrap;
import com.botdarr.clients.ChatClientResponseBuilder;
import com.botdarr.clients.mattermost.transactions.MattermostPost;
import com.botdarr.commands.CommandContext;
import com.botdarr.scheduling.Scheduler;
import org.apache.logging.log4j.util.Strings;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.botdarr.api.lidarr.LidarrApi.ADD_ARTIST_COMMAND_FIELD_PREFIX;
import static com.botdarr.api.radarr.RadarrApi.ADD_MOVIE_COMMAND_FIELD_PREFIX;
import static com.botdarr.api.sonarr.SonarrApi.ADD_SHOW_COMMAND_FIELD_PREFIX;

public class MattermostBootstrap extends ChatClientBootstrap {

  @Override
  public void init() throws Exception {
    MattermostChatClient chatClient = new MattermostChatClient();
    ChatClientResponseBuilder<MattermostResponse> responseChatClientResponseBuilder = new MattermostResponseBuilder();
    ChatClientBootstrap.ApisAndCommandConfig config = buildConfig();
    initScheduling(chatClient, responseChatClientResponseBuilder, config.getApis());
    chatClient.addListener(new MattermostChatClient.MattermostMessageListener() {
      @Override
      public void process(String channelId, String sender, String content) {
        Scheduler.getScheduler().executeCommand(() -> {
          MattermostBootstrap.this.runAndProcessCommands(CommandContext.getConfig().getPrefix(), content, sender,
                  responseChatClientResponseBuilder,
                  chatClientResponse -> chatClient.sendMessage(chatClientResponse, channelId));
          return null;
        });
      }

      @Override
      public void reactionAdded(String channelId, String userId, String postId, String emojiName) {
        if (emojiName == null || !THUMBS_UP_EMOTES.contains(emojiName)) {
          return;
        }
        //fetch the reacted post and run the add command embedded in it (if any)
        MattermostPost post = chatClient.getPost(postId);
        if (post == null) {
          return;
        }
        String command = getCommandFromPostMessage(post.getMessage());
        if (Strings.isEmpty(command)) {
          return;
        }
        String username = chatClient.getUsername(userId);
        Scheduler.getScheduler().executeCommand(() -> {
          MattermostBootstrap.this.runAndProcessCommands(CommandContext.getConfig().getPrefix(), command, username,
                  responseChatClientResponseBuilder,
                  chatClientResponse -> chatClient.sendMessage(chatClientResponse, channelId));
          return null;
        });
      }
    });
    chatClient.listen();
  }

  /**
   * Mattermost responses embed a copyable add command line, i.e.,
   * <pre>**Add movie command:** `!movie id add Some Movie 12345`</pre>
   * When a user reacts to a bot post with a thumbs up, we extract that command and run it.
   * @return the command to run, or null if the message doesn't contain one
   */
  public String getCommandFromPostMessage(String message) {
    if (Strings.isEmpty(message)) {
      return null;
    }
    for (String line : message.split("\n")) {
      //strip markdown bold and code formatting
      String cleanedLine = line.replace("**", "").replace("`", "").trim();
      for (String fieldPrefix : ADD_COMMAND_FIELD_PREFIXES) {
        String key = fieldPrefix + ":";
        if (cleanedLine.startsWith(key)) {
          String command = cleanedLine.substring(key.length()).trim();
          if (!command.isEmpty()) {
            return command;
          }
        }
      }
    }
    return null;
  }

  private boolean isValidURL(String urlString) {
    try {
      new URL(urlString);
      return true;
    } catch (MalformedURLException e) {
      return false;
    }
  }

  @Override
  public boolean isConfigured(Properties properties) {
    final boolean isConfigured =
            !Strings.isBlank(properties.getProperty(Config.Constants.MATTERMOST_URL)) &&
                    !Strings.isBlank(properties.getProperty(Config.Constants.MATTERMOST_TOKEN)) &&
                    !Strings.isBlank(properties.getProperty(Config.Constants.MATTERMOST_CHANNELS));

    if (isConfigured && !isValidURL(properties.getProperty(Config.Constants.MATTERMOST_URL))) {
      throw new RuntimeException("Mattermost url must be a url");
    }

    return isConfigured;
  }

  @Override
  public void validatePrefix(String configuredPrefix) {
    super.validatePrefix(configuredPrefix);
    if (configuredPrefix.equals("/")) {
      throw new RuntimeException("Cannot use / command prefix in mattermost since / commands are reserved for mattermost slash commands");
    }
  }

  //mattermost normalizes the thumbs up emoji to +1
  private static final List<String> THUMBS_UP_EMOTES = Arrays.asList("+1", "thumbsup", "\uD83D\uDC4D");
  private static final List<String> ADD_COMMAND_FIELD_PREFIXES = Arrays.asList(
          ADD_MOVIE_COMMAND_FIELD_PREFIX, ADD_SHOW_COMMAND_FIELD_PREFIX, ADD_ARTIST_COMMAND_FIELD_PREFIX);
}
