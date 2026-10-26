package com.botdarr.clients.mattermost;

import com.botdarr.Config;
import com.botdarr.api.lidarr.LidarrArtist;
import com.botdarr.api.lidarr.LidarrCommands;
import com.botdarr.api.lidarr.LidarrQueueRecord;
import com.botdarr.api.lidarr.LidarrQueueStatusMessage;
import com.botdarr.api.radarr.*;
import com.botdarr.api.sonarr.*;
import com.botdarr.clients.ChatClientResponseBuilder;
import com.botdarr.commands.*;
import com.botdarr.commands.responses.*;
import com.botdarr.utilities.ListUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;

import static com.botdarr.api.lidarr.LidarrApi.ADD_ARTIST_COMMAND_FIELD_PREFIX;
import static com.botdarr.api.radarr.RadarrApi.ADD_MOVIE_COMMAND_FIELD_PREFIX;
import static com.botdarr.api.sonarr.SonarrApi.ADD_SHOW_COMMAND_FIELD_PREFIX;
import static com.botdarr.commands.StatusCommand.STATUS_COMMAND;
import static com.botdarr.commands.StatusCommand.STATUS_COMMAND_DESCRIPTION;

public class MattermostResponseBuilder implements ChatClientResponseBuilder<MattermostResponse> {
  @Override
  public MattermostResponse build(HelpResponse helpResponse) {
    try {
      MattermostResponse mattermostResponse = new MattermostResponse();
      mattermostResponse.addContent("**Version: " + ChatClientResponseBuilder.getVersion() + "**");
      boolean radarrEnabled = Config.isRadarrEnabled();
      boolean sonarrEnabled = Config.isSonarrEnabled();
      boolean lidarrEnabled = Config.isLidarrEnabled();
      if (radarrEnabled) {
        mattermostResponse.addContent("**" + RadarrCommands.getHelpMovieCommandStr() + "** - Shows all the commands for movies");
      }

      if (sonarrEnabled) {
        mattermostResponse.addContent("**" + SonarrCommands.getHelpShowCommandStr() + "** - Shows all the commands for shows");
      }

      if (lidarrEnabled) {
        mattermostResponse.addContent("**" + LidarrCommands.getHelpCommandStr() + "** - Shows all the commands for music");
      }

      if (!radarrEnabled && !sonarrEnabled && !lidarrEnabled) {
        mattermostResponse.addContent("**No radarr or sonarr or lidarr commands configured, check your properties file and logs**");
      }

      if (!Config.getStatusEndpoints().isEmpty()) {
        mattermostResponse.addContent("**" + CommandContext.getConfig().getPrefix() + STATUS_COMMAND + "** - " + STATUS_COMMAND_DESCRIPTION);
      }
      return mattermostResponse;
    } catch (Exception e) {
      throw new RuntimeException("Error getting botdarr version", e);
    }
  }

  @Override
  public MattermostResponse build(MusicHelpResponse musicHelpResponse) {
    return getListOfCommands(musicHelpResponse.getLidarrCommands());
  }

  @Override
  public MattermostResponse build(MoviesHelpResponse moviesHelpResponse) {
    return getListOfCommands(moviesHelpResponse.getRadarrCommands());
  }

  @Override
  public MattermostResponse build(ShowsHelpResponse showsHelpResponse) {
    return getListOfCommands(showsHelpResponse.getSonarrCommands());
  }

  @Override
  public MattermostResponse build(ShowResponse showResponse) {
    SonarrShow show = showResponse.getShow();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + show.getTitle());
    mattermostResponse.addContent("**TvdbId** - " + show.getTvdbId());
    mattermostResponse.addContent(addCommandContent(ADD_SHOW_COMMAND_FIELD_PREFIX, SonarrCommands.getAddShowCommandStr(show.getTvdbId())));
    mattermostResponse.addImage(show.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(MusicArtistResponse musicArtistResponse) {
    LidarrArtist lidarrArtist = musicArtistResponse.getArtist();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Artist Name** - " + lidarrArtist.getArtistName());
    mattermostResponse.addContent("**Id** - " + lidarrArtist.getForeignArtistId());
    mattermostResponse.addContent(addCommandContent(ADD_ARTIST_COMMAND_FIELD_PREFIX, LidarrCommands.getAddArtistCommandStr(lidarrArtist.getArtistName(), lidarrArtist.getForeignArtistId())));
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(MovieResponse movieResponse) {
    RadarrMovie radarrMovie = movieResponse.getRadarrMovie();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Movie Title** - " + radarrMovie.getTitle());
    mattermostResponse.addContent("**TmdbId** - " + radarrMovie.getTmdbId());
    mattermostResponse.addContent(addCommandContent(ADD_MOVIE_COMMAND_FIELD_PREFIX, RadarrCommands.getAddMovieCommandStr(radarrMovie.getTitle(), radarrMovie.getTmdbId())));
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(ShowDownloadResponse showDownloadResponse) {
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + showDownloadResponse.getShowQueue().getTitle());
    mattermostResponse.addContent("**Season/Episode** - " + "S" + showDownloadResponse.getShowQueue().getSeasonNumber() + "E" + showDownloadResponse.getShowQueue().getEpisodeNumber());
    mattermostResponse.addContent("**Quality** - " + showDownloadResponse.getShowQueue().getQualityProfileName());
    mattermostResponse.addContent("**Status** - " + showDownloadResponse.getShowQueue().getStatus());
    mattermostResponse.addContent("**Time Left** - _" + (showDownloadResponse.getShowQueue().getTimeleft() == null ? "unknown" : showDownloadResponse.getShowQueue().getTimeleft()) + "_");
    mattermostResponse.addContent("**Overview** - " + showDownloadResponse.getShowQueue().getOverview());
    if (showDownloadResponse.getShowQueue().getStatusMessages() != null) {
      for (String statusMessage : showDownloadResponse.getShowQueue().getStatusMessages()) {
        mattermostResponse.addContent("**Download message** - " + statusMessage);
      }
    }
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(MovieDownloadResponse movieDownloadResponse) {
    RadarrQueue radarrQueue = movieDownloadResponse.getRadarrQueue();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + radarrQueue.getTitle());
    mattermostResponse.addContent("**Quality** - " + radarrQueue.getQuality().getQuality().getName());
    mattermostResponse.addContent("**Status** - " + radarrQueue.getStatus());
    mattermostResponse.addContent("**Time Left** - _" + (radarrQueue.getTimeleft() == null ? "unknown" : radarrQueue.getTimeleft()) + "_");
    if (radarrQueue.getStatusMessages() != null) {
      for (RadarrQueueStatusMessages statusMessage : radarrQueue.getStatusMessages()) {
        for (String message : statusMessage.getMessages()) {
          mattermostResponse.addContent("**Download message** - " + message);
        }
      }
    }
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(MusicArtistDownloadResponse musicArtistDownloadResponse) {
    LidarrQueueRecord lidarrQueue = musicArtistDownloadResponse.getQueueRecord();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + lidarrQueue.getTitle());
    mattermostResponse.addContent("**Time Left** - _" + (lidarrQueue.getTimeleft() == null ? "unknown" : lidarrQueue.getTimeleft()) + "_");
    mattermostResponse.addContent("**Status** - " + lidarrQueue.getStatus());
    if (lidarrQueue.getStatusMessages() != null) {
      for (LidarrQueueStatusMessage statusMessage : ListUtils.subList(lidarrQueue.getStatusMessages(), 5)) {
        for (String message : statusMessage.getMessages()) {
          mattermostResponse.addContent("**Download message** - " + message);
        }
      }
    }
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(ErrorResponse errorResponse) {
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**" + errorResponse.getErrorMessage() + "**");
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(InfoResponse infoResponse) {
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent(infoResponse.getInfoMessage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(SuccessResponse successResponse) {
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**" + successResponse.getSuccessMessage() + "**");
    return mattermostResponse;
  }

  private String addQualityItem(SonarrProfileQualityItem sonarrProfileQualityItem) {
    return "**Quality** - Name: " + sonarrProfileQualityItem.getQuality().getName() + ", Resolution: " + sonarrProfileQualityItem.getQuality().getResolution();
  }

  @Override
  public MattermostResponse build(ShowProfileResponse showProfileResponse) {
    SonarrProfile sonarrProfile = showProfileResponse.getShowProfile();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Profile**");
    mattermostResponse.addContent("**Name** - " + sonarrProfile.getName());
    mattermostResponse.addContent("**Cutoff** - " + sonarrProfile.getCutOffDisplayStr());
    for (int k = 0; k < sonarrProfile.getItems().size(); k++) {
      SonarrProfileQualityItem sonarrProfileQualityItem = sonarrProfile.getItems().get(k);
      if (sonarrProfileQualityItem.isAllowed()) {
        if (sonarrProfileQualityItem.getQuality() == null) {
          for(SonarrProfileQualityItem qualityItem : sonarrProfileQualityItem.getItems()) {
            mattermostResponse.addContent(addQualityItem(qualityItem));
          }
        } else {
          mattermostResponse.addContent(addQualityItem(sonarrProfileQualityItem));
        }
      }
    }
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(MovieProfileResponse movieProfileResponse) {
    RadarrProfile radarrProfile = movieProfileResponse.getRadarrProfile();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Profile**");
    mattermostResponse.addContent("**Name** - " + radarrProfile.getName());
    mattermostResponse.addContent("**Cutoff** - " + radarrProfile.getCutoff());
    for (int k = 0; k < radarrProfile.getItems().size(); k++) {
      RadarrProfileQualityItem radarrProfileQualityItem = radarrProfile.getItems().get(k);
      if (radarrProfileQualityItem.isAllowed() && radarrProfileQualityItem.getQuality() != null) {
        mattermostResponse.addContent("**Quality** - Name: " + radarrProfileQualityItem.getQuality().getName() + ", Resolution: " + radarrProfileQualityItem.getQuality().getResolution());
      }
    }
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(NewShowResponse newShowResponse) {
    SonarrShow sonarrShow = newShowResponse.getNewShow();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + sonarrShow.getTitle());
    mattermostResponse.addContent("**TvdbId** - " + sonarrShow.getTvdbId());
    mattermostResponse.addContent(addCommandContent(ADD_SHOW_COMMAND_FIELD_PREFIX, SonarrCommands.getAddShowCommandStr(sonarrShow.getTvdbId())));
    mattermostResponse.addImage(sonarrShow.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(ExistingShowResponse existingShowResponse) {
    SonarrShow existingShow = existingShowResponse.getExistingShow();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + existingShow.getTitle());
    mattermostResponse.addContent("**TvdbId** - " + existingShow.getTvdbId());
    mattermostResponse.addContent("**Id** - " + existingShow.getId());
    if (existingShow.getSeasons() != null) {
      mattermostResponse.addContent("Number of seasons - " + existingShow.getSeasons().size());
      for (SonarrSeason sonarrSeason : existingShow.getSeasons()) {
        mattermostResponse.addContent(
                "Season#" + sonarrSeason.getSeasonNumber() +
                        ",Available Epsiodes=" + sonarrSeason.getStatistics().getEpisodeCount() +
                        ",Total Epsiodes=" + sonarrSeason.getStatistics().getTotalEpisodeCount());
      }
    }
    mattermostResponse.addImage(existingShow.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(NewMovieResponse newMovieResponse) {
    RadarrMovie radarrMovie = newMovieResponse.getRadarrMovie();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + radarrMovie.getTitle());
    mattermostResponse.addContent("**TmdbId** - " + radarrMovie.getTmdbId());
    mattermostResponse.addContent(addCommandContent(ADD_MOVIE_COMMAND_FIELD_PREFIX, RadarrCommands.getAddMovieCommandStr(radarrMovie.getTitle(), radarrMovie.getTmdbId())));
    mattermostResponse.addImage(radarrMovie.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(ExistingMovieResponse existingMovieResponse) {
    RadarrMovie radarrMovie = existingMovieResponse.getRadarrMovie();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + radarrMovie.getTitle());
    mattermostResponse.addContent("**TmdbId** - " + radarrMovie.getTmdbId());
    mattermostResponse.addContent("**Id** - " + radarrMovie.getId());
    mattermostResponse.addContent("**Downloaded** - " + (radarrMovie.getSizeOnDisk() > 0));
    mattermostResponse.addContent("**Has File** - " + radarrMovie.isHasFile());
    mattermostResponse.addImage(radarrMovie.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(NewMusicArtistResponse newMusicArtistResponse) {
    LidarrArtist lidarrArtist = newMusicArtistResponse.getLidarrArtist();
    MattermostResponse mattermostResponse = new MattermostResponse();
    String artistDetail = " (" + lidarrArtist.getDisambiguation() + ")";
    mattermostResponse.addContent("**Title** - " + (lidarrArtist.getArtistName() + (Strings.isEmpty(lidarrArtist.getDisambiguation()) ? "" :  artistDetail)));
    mattermostResponse.addContent(addCommandContent(ADD_ARTIST_COMMAND_FIELD_PREFIX, LidarrCommands.getAddArtistCommandStr(lidarrArtist.getArtistName(), lidarrArtist.getForeignArtistId())));
    mattermostResponse.addImage(lidarrArtist.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(ExistingMusicArtistResponse existingMusicArtistResponse) {
    LidarrArtist lidarrArtist = existingMusicArtistResponse.getLidarrArtist();
    MattermostResponse mattermostResponse = new MattermostResponse();
    String artistDetail = " (" + lidarrArtist.getDisambiguation() + ")";
    mattermostResponse.addContent("**Title** - " + (lidarrArtist.getArtistName() + (Strings.isEmpty(lidarrArtist.getDisambiguation()) ? "" :  artistDetail)));
    mattermostResponse.addImage(lidarrArtist.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(DiscoverMovieResponse discoverMovieResponse) {
    RadarrMovie radarrMovie = discoverMovieResponse.getRadarrMovie();
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Title** - " + radarrMovie.getTitle());
    mattermostResponse.addContent("**TmdbId** - " + radarrMovie.getTmdbId());
    mattermostResponse.addContent(addCommandContent(ADD_MOVIE_COMMAND_FIELD_PREFIX, RadarrCommands.getAddMovieCommandStr(radarrMovie.getTitle(), radarrMovie.getTmdbId())));
    mattermostResponse.addImage(radarrMovie.getRemoteImage());
    return mattermostResponse;
  }

  @Override
  public MattermostResponse build(StatusCommandResponse statusCommandResponse) {
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("| Endpoint | Status |");
    mattermostResponse.addContent("| ------ | ------ |");
    for (Map.Entry<String, Boolean> endpointStatusEntry : statusCommandResponse.getEndpoints().entrySet()) {
      mattermostResponse.addContent("| " + endpointStatusEntry.getKey() + " | " + (endpointStatusEntry.getValue() ? "\uD83D\uDFE2" : "\uD83D\uDD34") + " |");
    }
    return mattermostResponse;
  }

  /**
   * Format the add command as a markdown line that users can copy into chat, and that
   * {@link MattermostBootstrap#getCommandFromPostMessage(String)} can extract on thumbs up reactions.
   */
  private String addCommandContent(String fieldPrefix, String command) {
    return "**" + fieldPrefix + ":** `" + command + "`";
  }

  private MattermostResponse getListOfCommands(List<Command> commands) {
    MattermostResponse mattermostResponse = new MattermostResponse();
    mattermostResponse.addContent("**Commands**");
    for (Command command : commands) {
      mattermostResponse.addContent("**" + CommandContext.getConfig().getPrefix() + command.getCommandUsage() + "**" + " - " + command.getDescription());
    }
    return mattermostResponse;
  }
}
