package com.botdarr.clients.mattermost;

import com.botdarr.Config;
import mockit.Deencapsulation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class MattermostBootstrapTests {
  @Before
  public void beforeEachTest() {
    writeFakePropertiesFile(getDefaultProperties());
  }

  @Test
  public void isConfigured_allPropertiesSet() {
    Assert.assertTrue(new MattermostBootstrap().isConfigured(getDefaultProperties()));
  }

  @Test
  public void isConfigured_missingUrl() {
    Properties properties = getDefaultProperties();
    properties.remove("mattermost-url");
    Assert.assertFalse(new MattermostBootstrap().isConfigured(properties));
  }

  @Test
  public void isConfigured_missingToken() {
    Properties properties = getDefaultProperties();
    properties.remove("mattermost-token");
    Assert.assertFalse(new MattermostBootstrap().isConfigured(properties));
  }

  @Test
  public void isConfigured_missingChannels() {
    Properties properties = getDefaultProperties();
    properties.remove("mattermost-channels");
    Assert.assertFalse(new MattermostBootstrap().isConfigured(properties));
  }

  @Test(expected = RuntimeException.class)
  public void isConfigured_invalidUrl() {
    Properties properties = getDefaultProperties();
    properties.setProperty("mattermost-url", "not a url");
    new MattermostBootstrap().isConfigured(properties);
  }

  @Test(expected = RuntimeException.class)
  public void validatePrefix_rejectsSlash() {
    new MattermostBootstrap().validatePrefix("/");
  }

  @Test
  public void validatePrefix_acceptsExclamation() {
    new MattermostBootstrap().validatePrefix("!");
  }

  @Test
  public void getCommandFromPostMessage_returnsMovieCommand() {
    String message = "**Title** - MovieTitle1\n**TmdbId** - 43234\n**Add movie command:** `!movie id add MovieTitle1 43234`";
    Assert.assertEquals("!movie id add MovieTitle1 43234", new MattermostBootstrap().getCommandFromPostMessage(message));
  }

  @Test
  public void getCommandFromPostMessage_returnsShowCommand() {
    String message = "**Title** - ShowTitle1\n**TvdbId** - 43234\n**Add show command:** `!show id add 43234`";
    Assert.assertEquals("!show id add 43234", new MattermostBootstrap().getCommandFromPostMessage(message));
  }

  @Test
  public void getCommandFromPostMessage_returnsArtistCommand() {
    String message = "**Title** - ArtistTitle1\n**Add artist command:** `!music artist id add ArtistTitle1 1234-abcd`";
    Assert.assertEquals("!music artist id add ArtistTitle1 1234-abcd", new MattermostBootstrap().getCommandFromPostMessage(message));
  }

  @Test
  public void getCommandFromPostMessage_returnsNullForMessageWithoutCommand() {
    String message = "**Title** - MovieTitle1\n**TmdbId** - 43234";
    Assert.assertNull(new MattermostBootstrap().getCommandFromPostMessage(message));
  }

  @Test
  public void getCommandFromPostMessage_returnsNullForEmptyMessage() {
    Assert.assertNull(new MattermostBootstrap().getCommandFromPostMessage(""));
    Assert.assertNull(new MattermostBootstrap().getCommandFromPostMessage(null));
  }

  private Properties getDefaultProperties() {
    Properties properties = new Properties();
    properties.setProperty("mattermost-url", "http://localhost:8065");
    properties.setProperty("mattermost-token", "G$K$GK");
    properties.setProperty("mattermost-channels", "channel1,channel2");
    properties.setProperty("radarr-url", "http://localhost:444");
    properties.setProperty("radarr-token", "FSJDkjmf#$Kf3");
    properties.setProperty("radarr-path", "/movies");
    properties.setProperty("radarr-default-profile", "any");
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
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
}
