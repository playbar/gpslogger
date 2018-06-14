/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.mendhak.gpslogger.ui.fragments.settings;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.network.Networks;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.PreferenceNames;
import com.mendhak.gpslogger.common.network.ServerType;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlJob;
import com.mendhak.gpslogger.loggers.customurl.CustomUrlRequest;
import com.mendhak.gpslogger.senders.PreferenceValidator;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.PermissionedPreferenceFragment;
import com.path.android.jobqueue.JobManager;

import org.slf4j.Logger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import de.greenrobot.event.EventBus;

public class CustomUrlFragment extends PermissionedPreferenceFragment implements
        PreferenceValidator,
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final Logger LOG = Logs.of(CustomUrlFragment.class);


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customurlsettings);

        EditTextPreference urlPathPreference = (EditTextPreference)findPreference(PreferenceNames.LOG_TO_URL_PATH);
        urlPathPreference.setSummary(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setText(PreferenceHelper.getInstance().getCustomLoggingUrl());
        urlPathPreference.setOnPreferenceChangeListener(this);

        findPreference("customurl_legend_1").setOnPreferenceClickListener(this);
        findPreference("customurl_http_test").setOnPreferenceClickListener(this);
        findPreference("customurl_validatecustomsslcert").setOnPreferenceClickListener(this);

        registerEventBus();

    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference.getKey().equals(PreferenceNames.LOG_TO_URL_PATH)){
            preference.setSummary(newValue.toString());
        }
        return true;
    }


    @Override
    public boolean isValid() {
        return true;
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference.getKey().equals("customurl_legend_1")){

            String legend1 = MessageFormat.format("{0} %LAT<br />{1} %LON<br />{2} %DESC<br />{3} %SAT<br />{4} %ALT<br />" +
                            "{5} %SPD<br />{6} %ACC<br />{7} %DIR<br />{8} %PROV<br />{9} %TIMESTAMP<br />" +
                            "{10} %TIME<br />{11} %STARTTIMESTAMP<br />{12} %BATT<br />{13} %AID<br />{14} %SER<br />" +
                            "{15} %ACT<br />{16} %FILENAME<br />{17} %PROFILE<br />" +
                            "{18} %HDOP<br />{19} %VDOP<br />{20} %PDOP",
                    getString(R.string.txt_latitude), getString(R.string.txt_longitude), getString(R.string.txt_annotation),
                    getString(R.string.txt_satellites), getString(R.string.txt_altitude), getString(R.string.txt_speed),
                    getString(R.string.txt_accuracy), getString(R.string.txt_direction), getString(R.string.txt_provider),
                    getString(R.string.txt_timestamp_epoch),
                    getString(R.string.txt_time_isoformat),
                    getString(R.string.txt_starttimestamp_epoch),
                    getString(R.string.txt_battery), "Android ID ", "Serial ", getString(R.string.txt_activity), getString(R.string.summary_current_filename), "Profile:", "HDOP:", "VDOP:", "PDOP:");
            Dialogs.alert(getString(R.string.parameters), legend1, getActivity());

        }
        else if(preference.getKey().equals("customurl_validatecustomsslcert")){

            try {
                URL u = new URL(PreferenceHelper.getInstance().getCustomLoggingUrl());
                Networks.beginCertificateValidationWorkflow(getActivity(), u.getHost(), u.getPort() < 0 ? u.getDefaultPort() : u.getPort(), ServerType.HTTPS);
            } catch (MalformedURLException e) {
                LOG.error("Could not start certificate validation", e);
            }

            return true;
        }
        else if (preference.getKey().equals("customurl_http_test")){
            LOG.debug("Sending test HTTP GET request to " + PreferenceHelper.getInstance().getCustomLoggingUrl() );
            Dialogs.progress(getActivity(), getString(R.string.please_wait), getString(R.string.please_wait));
            JobManager jobManager = AppSettings.getJobManager();
            jobManager.addJobInBackground(new CustomUrlJob(new CustomUrlRequest(PreferenceHelper.getInstance().getCustomLoggingUrl()), new UploadEvents.CustomUrl()));
        }
        return false;
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.CustomUrl c){
        LOG.debug("Custom URL test, success: " + c.success);
        Dialogs.hideProgress();
        if(!c.success){
            Dialogs.error(getString(R.string.error), c.message, c.throwable.getMessage(),c.throwable, getActivity());
        }
        else {
            Dialogs.alert(getString(R.string.success), "", getActivity());
        }
    }


}
