package com.djekgrif.alternativeradio.common;

import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;

import com.djekgrif.alternativeradio.manager.ConfigurationManager;
import com.djekgrif.alternativeradio.manager.Preferences;
import com.djekgrif.alternativeradio.network.ApiService;
import com.djekgrif.alternativeradio.network.SimpleSubscriber;
import com.djekgrif.alternativeradio.network.model.Channel;
import com.djekgrif.alternativeradio.network.model.RecentlyItem;
import com.djekgrif.alternativeradio.network.model.SongInfoDetails;
import com.djekgrif.alternativeradio.network.model.StationData;
import com.djekgrif.alternativeradio.network.model.StreamData;
import com.djekgrif.alternativeradio.ui.model.LastAppState;

import java.util.List;

/**
 * Created by djek-grif on 2/18/17.
 */

public class StreamDataUpdater {

    protected ApiService apiService;
    protected ConfigurationManager configurationManager;
    protected Preferences preferences;

    private StreamDataUpdaterListener updateSoundInfoListener;
    private Channel currentChannel;
    private String searchMediaUrl;
    private StreamData currentStreamData;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void setCurrentStreamData(StreamData currentStreamData) {
        this.currentStreamData = currentStreamData;
    }

    public void setCurrentChannel(Channel currentChannel) {
        this.currentChannel = currentChannel;
    }

    public StreamData getCurrentStreamData() {
        return currentStreamData;
    }

    public boolean isCurrentStreamDataValid() {
        return currentStreamData != null && !TextUtils.isEmpty(currentStreamData.getUrl());
    }

    public Uri getUriFromCurrentStreamData() {
        return Uri.parse(currentStreamData.getUrl());
    }

    public StreamDataUpdater(StreamDataUpdaterListener updateSoundInfoListener, ApiService apiService, ConfigurationManager configurationManager, Preferences preferences) {
        this.apiService = apiService;
        this.configurationManager = configurationManager;
        this.preferences = preferences;
        this.updateSoundInfoListener = updateSoundInfoListener;
        configurationManager.updateConfigurationData(configurationData -> {
            LastAppState lastAppState = preferences.getLastAppState();
            if (lastAppState != null && configurationData.getStations().contains(lastAppState.getStationData())) {
                StationData stationData = configurationData.getStations().get(configurationData.getStations().indexOf(lastAppState.getStationData()));
                if (stationData.getChannels().contains(lastAppState.getChannel())) {
                    currentChannel = stationData.getChannels().get(stationData.getChannels().indexOf(lastAppState.getChannel()));
                    if (currentChannel.getStreamUrls().contains(lastAppState.getStreamData())) {
                        currentStreamData = currentChannel.getStreamUrls().get(currentChannel.getStreamUrls().indexOf(lastAppState.getStreamData()));
                    } else {
                        currentStreamData = currentChannel.getStreamUrls().get(currentChannel.getStreamUrls().size() > 1 ? 1 : 0);
                    }
                } else {
                    currentChannel = configurationData.getStations().get(0).getChannels().get(0);
                    currentStreamData = currentChannel.getStreamUrls().get(currentChannel.getStreamUrls().size() > 1 ? 1 : 0);
                }
            } else {
                currentChannel = configurationData.getStations().get(0).getChannels().get(0);
                currentStreamData = currentChannel.getStreamUrls().get(currentChannel.getStreamUrls().size() > 1 ? 1 : 0);
            }
            searchMediaUrl = configurationData.getSearchMediaUrl();
            this.updateSoundInfoListener.updateConfiguration(configurationData);
        });
    }

    private Handler infoUpdaterHandler = new Handler();
    private Runnable updateInfoRunnable = new Runnable() {
        @Override
        public void run() {
            updateSoundInfo();
            infoUpdaterHandler.postDelayed(updateInfoRunnable, 1000 * 20);
        }
    };

    public void startSoundInfoUpdater() {
        configurationManager.updateConfigurationData(configurationData -> {
            infoUpdaterHandler.removeCallbacks(updateInfoRunnable);
            infoUpdaterHandler.post(updateInfoRunnable);
        });
    }

    public void stopSoundInfoUpdater() {
        infoUpdaterHandler.removeCallbacks(updateInfoRunnable);
    }

    private void updateSoundInfo() {
        apiService.getCurrentSoundInfo(currentChannel.getSongInfoUrl(), searchMediaUrl, new SimpleSubscriber<SongInfoDetails>() {
            @Override
            public void onNext(SongInfoDetails soundInfoResponse) {
                if (soundInfoResponse != null) {
                    updateSoundInfoListener.updateData(soundInfoResponse);
//                    if (homeFragmentView.getSupportMediaController() != null) {
//                        Bundle bundle = new Bundle();
//                        bundle.putString(BundleKeys.ARTIST_NAME, soundInfoResponse.getArtistName());
//                        bundle.putString(BundleKeys.SONG_INFO_DETAILS, soundInfoResponse.getTrackName());
//                        homeFragmentView.getSupportMediaController().getTransportControls().sendCustomAction(StreamService.CUSTOM_UPDATE_NOTIFICATION_DATA_ACTION, bundle);
//                    }
                }
            }
        });
        apiService.getRecentlyList(currentChannel.getRecentlyInfoUrl(), new SimpleSubscriber<List<RecentlyItem>>() {
            @Override
            public void onNext(List<RecentlyItem> recentlyItems) {
                updateSoundInfoListener.updateRecentlyList(recentlyItems);
            }
        });
    }

}
