#!/bin/bash

function addConfiguration {
    local environmentVariableName=$1
    local environmentVariableValue=$2
    local propertiesFile=$3

    echo "${environmentVariableName}=${environmentVariableValue}" >> "${propertiesFile}"
}

propertiesFile="/home/botdarr/config/properties"
overrides="/home/botdarr/config/properties.overrides"

mkdir -p "/home/botdarr/config"
: > "${overrides}"

[[ ! -z "${MATRIX_USERNAME}" ]] && addConfiguration "matrix-username" "${MATRIX_USERNAME}" "${overrides}"
[[ ! -z "${MATRIX_PASSWORD}" ]] && addConfiguration "matrix-password" "${MATRIX_PASSWORD}" "${overrides}"
[[ ! -z "${MATRIX_ROOM}" ]] && addConfiguration "matrix-room" "${MATRIX_ROOM}" "${overrides}"
[[ ! -z "${MATRIX_HOME_SERVER_URL}" ]] && addConfiguration "matrix-home-server-url" "${MATRIX_HOME_SERVER_URL}" "${overrides}"

[[ ! -z "${MATTERMOST_URL}" ]] && addConfiguration "mattermost-url" "${MATTERMOST_URL}" "${overrides}"
[[ ! -z "${MATTERMOST_TOKEN}" ]] && addConfiguration "mattermost-token" "${MATTERMOST_TOKEN}" "${overrides}"
[[ ! -z "${MATTERMOST_CHANNELS}" ]] && addConfiguration "mattermost-channels" "${MATTERMOST_CHANNELS}" "${overrides}"

[[ ! -z "${TELEGRAM_TOKEN}" ]] && addConfiguration "telegram-token" "${TELEGRAM_TOKEN}" "${overrides}"
[[ ! -z "${TELEGRAM_PRIVATE_CHANNELS}" ]] && addConfiguration "telegram-private-channels" "${TELEGRAM_PRIVATE_CHANNELS}" "${overrides}"
[[ ! -z "${TELEGRAM_PRIVATE_GROUPS}" ]] && addConfiguration "telegram-private-groups" "${TELEGRAM_PRIVATE_GROUPS}" "${overrides}"

[[ ! -z "${SLACK_BOT_TOKEN}" ]] && addConfiguration "slack-bot-token" "${SLACK_BOT_TOKEN}" "${overrides}"
[[ ! -z "${SLACK_APP_TOKEN}" ]] && addConfiguration "slack-app-token" "${SLACK_APP_TOKEN}" "${overrides}"
[[ ! -z "${SLACK_CHANNELS}" ]] && addConfiguration "slack-channels" "${SLACK_CHANNELS}" "${overrides}"

[[ ! -z "${DISCORD_TOKEN}" ]] && addConfiguration "discord-token" "${DISCORD_TOKEN}" "${overrides}"
[[ ! -z "${DISCORD_CHANNELS}" ]] && addConfiguration "discord-channels" "${DISCORD_CHANNELS}" "${overrides}"

[[ ! -z "${RADARR_URL}" ]] && addConfiguration "radarr-url" "${RADARR_URL}" "${overrides}"
[[ ! -z "${RADARR_TOKEN}" ]] && addConfiguration "radarr-token" "${RADARR_TOKEN}" "${overrides}"
[[ ! -z "${RADARR_DEFAULT_PROFILE}" ]] && addConfiguration "radarr-default-profile" "${RADARR_DEFAULT_PROFILE}" "${overrides}"
[[ ! -z "${RADARR_PATH}" ]] && addConfiguration "radarr-path" "${RADARR_PATH}" "${overrides}"
[[ ! -z "${RADARR_URL_BASE}" ]] && addConfiguration "radarr-url-base" "${RADARR_URL_BASE}" "${overrides}"
[[ ! -z "${RADARR_DEFAULT_AVAILABILITY}" ]] && addConfiguration "radarr-default-availability" "${RADARR_DEFAULT_AVAILABILITY}" "${overrides}"

[[ ! -z "${SONARR_URL}" ]] && addConfiguration "sonarr-url" "${SONARR_URL}" "${overrides}"
[[ ! -z "${SONARR_TOKEN}" ]] && addConfiguration "sonarr-token" "${SONARR_TOKEN}" "${overrides}"
[[ ! -z "${SONARR_DEFAULT_PROFILE}" ]] && addConfiguration "sonarr-default-profile" "${SONARR_DEFAULT_PROFILE}" "${overrides}"
[[ ! -z "${SONARR_PATH}" ]] && addConfiguration "sonarr-path" "${SONARR_PATH}" "${overrides}"
[[ ! -z "${SONARR_URL_BASE}" ]] && addConfiguration "sonarr-url-base" "${SONARR_URL_BASE}" "${overrides}"
[[ ! -z "${SONARR_V4}" ]] && addConfiguration "sonarr-v4" "${SONARR_V4}" "${overrides}"

[[ ! -z "${LIDARR_URL}" ]] && addConfiguration "lidarr-url" "${LIDARR_URL}" "${overrides}"
[[ ! -z "${LIDARR_TOKEN}" ]] && addConfiguration "lidarr-token" "${LIDARR_TOKEN}" "${overrides}"
[[ ! -z "${LIDARR_DEFAULT_QUALITY_PROFILE}" ]] && addConfiguration "lidarr-default-quality-profile" "${LIDARR_DEFAULT_QUALITY_PROFILE}" "${overrides}"
[[ ! -z "${LIDARR_DEFAULT_METADATA_PROFILE}" ]] && addConfiguration "lidarr-default-metadata-profile" "${LIDARR_DEFAULT_METADATA_PROFILE}" "${overrides}"
[[ ! -z "${LIDARR_PATH}" ]] && addConfiguration "lidarr-path" "${LIDARR_PATH}" "${overrides}"
[[ ! -z "${LIDARR_URL_BASE}" ]] && addConfiguration "lidarr-url-base" "${LIDARR_URL_BASE}" "${overrides}"


[[ ! -z "${MAX_REQUESTS_THRESHOLD}" ]] && addConfiguration "max-requests-threshold" "${MAX_REQUESTS_THRESHOLD}" "${overrides}"
[[ ! -z "${MAX_ARTIST_REQUESTS_PER_USER}" ]] && addConfiguration "max-artist-requests-per-user" "${MAX_ARTIST_REQUESTS_PER_USER}" "${overrides}"
[[ ! -z "${MAX_SHOW_REQUESTS_PER_USER}" ]] && addConfiguration "max-show-requests-per-user" "${MAX_SHOW_REQUESTS_PER_USER}" "${overrides}"
[[ ! -z "${MAX_MOVIE_REQUESTS_PER_USER}" ]] && addConfiguration "max-movie-requests-per-user" "${MAX_MOVIE_REQUESTS_PER_USER}" "${overrides}"
[[ ! -z "${EXISTING_ITEM_PATHS_BLACKLIST}" ]] && addConfiguration "existing-item-paths-blacklist" "${EXISTING_ITEM_PATHS_BLACKLIST}" "${overrides}"
[[ ! -z "${STATUS_ENDPOINTS}" ]] && addConfiguration "status-endpoints" "${STATUS_ENDPOINTS}" "${overrides}"
[[ ! -z "${LOG_LEVEL}" ]] && addConfiguration "log-level" "${LOG_LEVEL}" "${overrides}"
[[ ! -z "${NOTIFICATION_INTERVAL}" ]] && addConfiguration "notification-interval" "${NOTIFICATION_INTERVAL}" "${overrides}"

if [ ! -e "${propertiesFile}" ]; then
    # first boot: generate the properties file from the environment variables
    addConfiguration "max-downloads-to-show" "${MAX_DOWNLOADS_TO_SHOW:-20}" "${overrides}"
    addConfiguration "max-results-to-show" "${MAX_RESULTS_TO_SHOW:-20}" "${overrides}"
    addConfiguration "command-prefix" "${COMMAND_PREFIX:-!}" "${overrides}"
    addConfiguration "timeout" "${TIMEOUT:-5000}" "${overrides}"
    mv "${overrides}" "${propertiesFile}"
elif [ -s "${overrides}" ]; then
    # later boots: only the keys whose environment variables are set get updated,
    # every other line (including manually added properties) is left untouched
    awk -F= '
        NR==FNR { override[$1] = substr($0, index($0, "=") + 1); next }
        {
            key = $1
            if (key in override) { print key "=" override[key]; done[key] = 1; delete override[key] }
            else if (key in done) { }
            else print
        }
        END { for (key in override) print key "=" override[key] }
    ' "${overrides}" "${propertiesFile}" > "${propertiesFile}.tmp" && mv "${propertiesFile}.tmp" "${propertiesFile}"
fi

rm -f "${overrides}"

exec "$@"
