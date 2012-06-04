/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
	private static final String PREFERENCES_NAME = "preferences_name";
	public static final String PREFERENCES_CREDENTIALS = "preferences_credentials";
	public static final String PREFERENCES_TRIGGERS = "preferences_triggers";
	public static final String PREFERENCES_SUBMISSIONS = "preferences_submissions";
	
	private static final String KEY_VERSION_CODE = "version_code";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD_HASHED = "hashedPassword";
	private static final String KEY_IS_FIRST_RUN = "is_first_run";
	private static final String KEY_IS_AUTHENTICATED = "is_authenticated";
	private static final String KEY_IS_DISABLED = "is_disabled";
	private static final String KEY_LAST_MOBILITY_UPLOAD_TIMESTAMP = "last_mobility_upload_timestamp";
	private static final String KEY_LOGIN_TIMESTAMP = "login_timestamp";
	private static final String KEY_LAST_SURVEY_TIMESTAMP = "last_timestamp_";
	private static final String KEY_LAST_FEEDBACK_REFRESH_TIMESTAMP = "last_fb_refresh_timestamp";
	private static final String KEY_CAMPAIGN_REFRESH_TIME = "campaign_refresh_time";

	private final SharedPreferences mPreferences;
	
	public SharedPreferencesHelper(Context context) {
		mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
	}
	
	public SharedPreferencesHelper(Context context, String prefname, String username) {
		mPreferences = context.getSharedPreferences(prefname + username, Context.MODE_PRIVATE);
	}
	
	public boolean clearAll() {
		return mPreferences.edit().clear().commit();
	}
	
	public int getLastVersionCode() {
		return mPreferences.getInt(KEY_VERSION_CODE, -1);
	}
	
	public boolean setLastVersionCode(int versionCode) {
		return mPreferences.edit().putInt(KEY_VERSION_CODE, versionCode).commit();
	}
	
	public String getUsername() {
		return mPreferences.getString(KEY_USERNAME, "");
	}
	
	public boolean putUsername(String username) {
		return mPreferences.edit().putString(KEY_USERNAME, username).commit();
	}
	
	public String getHashedPassword() {
		return mPreferences.getString(KEY_PASSWORD_HASHED, "");
	}
	
	public boolean putHashedPassword(String hashedPassword) {
		return mPreferences.edit().putString(KEY_PASSWORD_HASHED, hashedPassword).commit();
	}
	
	public boolean clearCredentials() {
		return mPreferences.edit().remove(KEY_USERNAME).remove(KEY_PASSWORD_HASHED).commit();
	}
	
	public boolean isFirstRun() {
		return mPreferences.getBoolean(KEY_IS_FIRST_RUN, true);
	}
	
	public boolean setFirstRun(boolean isFirstRun) {
		return mPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, isFirstRun).commit();
	}
	
	public Long getLastMobilityUploadTimestamp() {
		return mPreferences.getLong(KEY_LAST_MOBILITY_UPLOAD_TIMESTAMP, 0);
	}
	
	public boolean putLastMobilityUploadTimestamp(Long timestamp) {
		return mPreferences.edit().putLong(KEY_LAST_MOBILITY_UPLOAD_TIMESTAMP, timestamp).commit();
	}
	
	public Long getLoginTimestamp() {
		return mPreferences.getLong(KEY_LOGIN_TIMESTAMP, 0);
	}
	
	public boolean putLoginTimestamp(Long timestamp) {
		return mPreferences.edit().putLong(KEY_LOGIN_TIMESTAMP, timestamp).commit();
	}
	
	public Long getLastSurveyTimestamp(String surveyId) {
		return mPreferences.getLong(KEY_LAST_SURVEY_TIMESTAMP + surveyId, 0);
	}
	
	public boolean putLastSurveyTimestamp(String surveyId, Long timestamp) {
		return mPreferences.edit().putLong(KEY_LAST_SURVEY_TIMESTAMP + surveyId, timestamp).commit();
	}
	
	public Long getLastFeedbackRefreshTimestamp(String urn) {
		return mPreferences.getLong(KEY_LAST_FEEDBACK_REFRESH_TIMESTAMP+"_"+urn, -1);
	}
	
	public boolean putLastFeedbackRefreshTimestamp(String urn, Long timestamp) {
		return mPreferences.edit().putLong(KEY_LAST_FEEDBACK_REFRESH_TIMESTAMP+"_"+urn, timestamp).commit();
	}

	public boolean removeLastFeedbackRefreshTimestamp(String urn) {
		return mPreferences.edit().remove(KEY_LAST_FEEDBACK_REFRESH_TIMESTAMP+"_"+urn).commit();
	}

	public boolean isAuthenticated() {
		if (getUsername().length() > 0 && getHashedPassword().length() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isUserDisabled() {
		return mPreferences.getBoolean(KEY_IS_DISABLED, false);
	}
	
	public boolean setUserDisabled(boolean isDisabled) {
		return mPreferences.edit().putBoolean(KEY_IS_DISABLED, isDisabled).commit();
	}

	public long getLastCampaignRefreshTime() {
		return mPreferences.getLong(KEY_CAMPAIGN_REFRESH_TIME, 0);
	}
	
	public boolean setLastCampaignRefreshTime(long time) {
		return mPreferences.edit().putLong(KEY_CAMPAIGN_REFRESH_TIME, time).commit();
	}
}
