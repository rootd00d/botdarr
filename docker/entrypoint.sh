#!/bin/bash

function addConfiguration {
    local environmentVariableName=$1
    local environmentVariableValue=$2
    local propertiesFile=$3

    echo "${environmentVariableName}=${environmentVariableValue}" >> "${propertiesFile}"
}

propertiesFile="/home/botdarr/config/properties"
generatedProperties="${propertiesFile}.generated"

mkdir -p "/home/botdarr/config"
: > "${generatedProperties}"

[[ ! -z "${MATRIX_USERNAME}" ]] && addConfiguration "matrix-username" "${MATRIX_USERNAME}" "${generatedProperties}"
[[ ! -z "${MATRIX_PASSWORD}" ]] && addConfiguration "matrix-password" "${MATRIX_PASSWORD}" "${generatedProperties}"
[[ ! -z "${MATRIX_ROOM}" ]] && addConfiguration "matrix-room" "${MATRIX_ROOM}" "${generatedProperties}"
[[ ! -z "${MATRIX_HOME_SERVER_URL}" ]] && addConfiguration "matrix-home-server-url" "${MATRIX_HOME_SERVER_URL}" "${generatedProperties}"

[[ ! -z "${MATTERMOST_URL}" ]] && addConfiguration "mattermost-url" "${MATTERMOST_URL}" "${generatedProperties}"
[[ ! -z "${MATTERMOST_TOKEN}" ]] && addConfiguration "mattermost-token" "${MATTERMOST_TOKEN}" "${generatedProperties}"
[[ ! -z "${MATTERMOST_CHANNELS}" ]] && addConfiguration "mattermost-channels" "${MATTERMOST_CHANNELS}" "${generatedProperties}"

[[ ! -z "${TELEGRAM_TOKEN}" ]] && addConfiguration "telegram-token" "${TELEGRAM_TOKEN}" "${generatedProperties}"
[[ ! -z "${TELEGRAM_PRIVATE_CHANNELS}" ]] && addConfiguration "telegram-private-channels" "${TELEGRAM_PRIVATE_CHANNELS}" "${generatedProperties}"
[[ ! -z "${TELEGRAM_PRIVATE_GROUPS}" ]] && addConfiguration "telegram-private-groups" "${TELEGRAM_PRIVATE_GROUPS}" "${generatedProperties}"

[[ ! -z "${SLACK_BOT_TOKEN}" ]] && addConfiguration "slack-bot-token" "${SLACK_BOT_TOKEN}" "${generatedProperties}"
[[ ! -z "${SLACK_APP_TOKEN}" ]] && addConfiguration "slack-app-token" "${SLACK_APP_TOKEN}" "${generatedProperties}"
[[ ! -z "${SLACK_CHANNELS}" ]] && addConfiguration "slack-channels" "${SLACK_CHANNELS}" "${generatedProperties}"

[[ ! -z "${DISCORD_TOKEN}" ]] && addConfiguration "discord-token" "${DISCORD_TOKEN}" "${generatedProperties}"
[[ ! -z "${DISCORD_CHANNELS}" ]] && addConfiguration "discord-channels" "${DISCORD_CHANNELS}" "${generatedProperties}"

[[ ! -z "${RADARR_URL}" ]] && addConfiguration "radarr-url" "${RADARR_URL}" "${generatedProperties}"
[[ ! -z "${RADARR_TOKEN}" ]] && addConfiguration "radarr-token" "${RADARR_TOKEN}" "${generatedProperties}"
[[ ! -z "${RADARR_DEFAULT_PROFILE}" ]] && addConfiguration "radarr-default-profile" "${RADARR_DEFAULT_PROFILE}" "${generatedProperties}"
[[ ! -z "${RADARR_PATH}" ]] && addConfiguration "radarr-path" "${RADARR_PATH}" "${generatedProperties}"
[[ ! -z "${RADARR_URL_BASE}" ]] && addConfiguration "radarr-url-base" "${RADARR_URL_BASE}" "${generatedProperties}"
[[ ! -z "${RADARR_DEFAULT_AVAILABILITY}" ]] && addConfiguration "radarr-default-availability" "${RADARR_DEFAULT_AVAILABILITY}" "${generatedProperties}"

[[ ! -z "${SONARR_URL}" ]] && addConfiguration "sonarr-url" "${SONARR_URL}" "${generatedProperties}"
[[ ! -z "${SONARR_TOKEN}" ]] && addConfiguration "sonarr-token" "${SONARR_TOKEN}" "${generatedProperties}"
[[ ! -z "${SONARR_DEFAULT_PROFILE}" ]] && addConfiguration "sonarr-default-profile" "${SONARR_DEFAULT_PROFILE}" "${generatedProperties}"
[[ ! -z "${SONARR_PATH}" ]] && addConfiguration "sonarr-path" "${SONARR_PATH}" "${generatedProperties}"
[[ ! -z "${SONARR_URL_BASE}" ]] && addConfiguration "sonarr-url-base" "${SONARR_URL_BASE}" "${generatedProperties}"
[[ ! -z "${SONARR_V4}" ]] && addConfiguration "sonarr-v4" "${SONARR_V4}" "${generatedProperties}"

[[ ! -z "${LIDARR_URL}" ]] && addConfiguration "lidarr-url" "${LIDARR_URL}" "${generatedProperties}"
[[ ! -z "${LIDARR_TOKEN}" ]] && addConfiguration "lidarr-token" "${LIDARR_TOKEN}" "${generatedProperties}"
[[ ! -z "${LIDARR_DEFAULT_QUALITY_PROFILE}" ]] && addConfiguration "lidarr-default-quality-profile" "${LIDARR_DEFAULT_QUALITY_PROFILE}" "${generatedProperties}"
[[ ! -z "${LIDARR_DEFAULT_METADATA_PROFILE}" ]] && addConfiguration "lidarr-default-metadata-profile" "${LIDARR_DEFAULT_METADATA_PROFILE}" "${generatedProperties}"
[[ ! -z "${LIDARR_PATH}" ]] && addConfiguration "lidarr-path" "${LIDARR_PATH}" "${generatedProperties}"
[[ ! -z "${LIDARR_URL_BASE}" ]] && addConfiguration "lidarr-url-base" "${LIDARR_URL_BASE}" "${generatedProperties}"


[[ ! -z "${MAX_REQUESTS_THRESHOLD}" ]] && addConfiguration "max-requests-threshold" "${MAX_REQUESTS_THRESHOLD}" "${generatedProperties}"
[[ ! -z "${MAX_ARTIST_REQUESTS_PER_USER}" ]] && addConfiguration "max-artist-requests-per-user" "${MAX_ARTIST_REQUESTS_PER_USER}" "${generatedProperties}"
[[ ! -z "${MAX_SHOW_REQUESTS_PER_USER}" ]] && addConfiguration "max-show-requests-per-user" "${MAX_SHOW_REQUESTS_PER_USER}" "${generatedProperties}"
[[ ! -z "${MAX_MOVIE_REQUESTS_PER_USER}" ]] && addConfiguration "max-movie-requests-per-user" "${MAX_MOVIE_REQUESTS_PER_USER}" "${generatedProperties}"
[[ ! -z "${EXISTING_ITEM_PATHS_BLACKLIST}" ]] && addConfiguration "existing-item-paths-blacklist" "${EXISTING_ITEM_PATHS_BLACKLIST}" "${generatedProperties}"
[[ ! -z "${STATUS_ENDPOINTS}" ]] && addConfiguration "status-endpoints" "${STATUS_ENDPOINTS}" "${generatedProperties}"
[[ ! -z "${LOG_LEVEL}" ]] && addConfiguration "log-level" "${LOG_LEVEL}" "${generatedProperties}"
[[ ! -z "${NOTIFICATION_INTERVAL}" ]] && addConfiguration "notification-interval" "${NOTIFICATION_INTERVAL}" "${generatedProperties}"

if [ -s "${generatedProperties}" ] || [ ! -e "${propertiesFile}" ]; then
    # environment variables are the source of truth: regenerate the properties file
    # on every start so template edits take effect on restart. When no environment
    # variables are set and a properties file already exists (e.g. the user mounted
    # their own), leave that file untouched.
    addConfiguration "max-downloads-to-show" "${MAX_DOWNLOADS_TO_SHOW:-20}" "${generatedProperties}"
    addConfiguration "max-results-to-show" "${MAX_RESULTS_TO_SHOW:-20}" "${generatedProperties}"
    addConfiguration "command-prefix" "${COMMAND_PREFIX:-!}" "${generatedProperties}"
    addConfiguration "timeout" "${TIMEOUT:-5000}" "${generatedProperties}"
    mv "${generatedProperties}" "${propertiesFile}"
else
    rm -f "${generatedProperties}"
fi

exec "$@"
