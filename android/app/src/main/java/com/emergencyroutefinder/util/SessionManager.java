package com.emergencyroutefinder.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.emergencyroutefinder.network.ApiClient;

/**
 * SessionManager — manages authenticated user/worker session.
 * Stores JWT token and principal metadata in SharedPreferences.
 *
 * Demonstrates: Encapsulation, Android SharedPreferences, Security
 */
public class SessionManager {

    private static final String PREF_NAME    = "erf_session";
    private static final String KEY_TOKEN    = "auth_token";
    private static final String KEY_ROLE     = "role";
    private static final String KEY_ID       = "principal_id";
    private static final String KEY_NAME     = "full_name";
    private static final String KEY_EMAIL    = "email";

    private final SharedPreferences prefs;
    private static SessionManager instance;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveSession(String token, String role, long principalId,
                            String fullName, String email) {
        prefs.edit()
                .putString(KEY_TOKEN,  token)
                .putString(KEY_ROLE,   role)
                .putLong(KEY_ID,       principalId)
                .putString(KEY_NAME,   fullName)
                .putString(KEY_EMAIL,  email)
                .apply();
        // Update Retrofit client with new token
        ApiClient.setAuthToken(token);
    }

    public boolean isLoggedIn() {
        return prefs.getString(KEY_TOKEN, null) != null;
    }

    public String getToken()     { return prefs.getString(KEY_TOKEN, null); }
    public String getRole()      { return prefs.getString(KEY_ROLE, null); }
    public long getPrincipalId() { return prefs.getLong(KEY_ID, -1); }
    public String getFullName()  { return prefs.getString(KEY_NAME, ""); }
    public String getEmail()     { return prefs.getString(KEY_EMAIL, ""); }

    public boolean isUser()      { return "USER".equals(getRole()); }
    public boolean isWorker()    { return "WORKER".equals(getRole()); }

    public void logout() {
        prefs.edit().clear().apply();
        ApiClient.setAuthToken(null);
    }

    /** Restore auth token into ApiClient after app restart. */
    public void restoreToken() {
        String token = getToken();
        if (token != null) {
            ApiClient.setAuthToken(token);
        }
    }
}
