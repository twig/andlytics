package com.github.andlyticsproject;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.github.andlyticsproject.model.DeveloperAccount;
import com.github.andlyticsproject.sync.AutosyncHandler;

// Suppressing warnings as there is no SherlockPreferenceFragment
// for us to use instead of a PreferencesActivity
@SuppressWarnings("deprecation")
public class PreferenceActivity extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener, OnSharedPreferenceChangeListener {

	private PreferenceCategory accountListPrefCat;
	private ListPreference autosyncPref;
	private List<DeveloperAccount> developerAccounts;
	private AutosyncHandler autosyncHandler = new AutosyncHandler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName(Preferences.PREF);
		addPreferencesFromResource(R.xml.preferences);

		// Find the preference category used to list all the accounts
		accountListPrefCat = (PreferenceCategory) getPreferenceScreen().findPreference(
				"prefCatAccountSpecific");

		// Now build the list of accounts
		buildAccountsList();

		// Find and setup a listener for auto sync as we have had to adjust the sync handler
		autosyncPref = (ListPreference) getPreferenceScreen().findPreference(
				Preferences.AUTOSYNC_PERIOD);
		autosyncPref.setOnPreferenceChangeListener(this);

		// We have to clear cached date formats when they change
		getPreferenceScreen().findPreference(Preferences.DATE_FORMAT_LONG)
				.setOnPreferenceChangeListener(this);


		for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
			initSummary(getPreferenceScreen().getPreference(i));
		}
	}

	private void buildAccountsList() {
		developerAccounts = DeveloperAccountManager.getInstance(this).getActiveDeveloperAccounts();
		for (DeveloperAccount account : developerAccounts) {
			// Create a preference representing the account and add it to the screen
			Preference pref = new Preference(this);
			pref.setTitle(account.getName());
			pref.setOnPreferenceClickListener(accountPrefrenceClickedListener);
			accountListPrefCat.addPreference(pref);
		}

	}

	OnPreferenceClickListener accountPrefrenceClickedListener = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			String accountName = (String) preference.getTitle();
			Intent i = new Intent(PreferenceActivity.this, AccountSpecificPreferenceActivity.class);
			i.putExtra(BaseActivity.EXTRA_AUTH_ACCOUNT_NAME, accountName);
			startActivity(i);
			return true;
		}
	};

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		if (key.equals(Preferences.AUTOSYNC_PERIOD)) {
			Integer newPeriod = Integer.parseInt((String) newValue);
			if (!newPeriod.equals(0)) {
				// Keep track of the last valid sync period for re-enabling the pref
				Preferences.saveLastNonZeroAutosyncPeriod(PreferenceActivity.this, newPeriod);
			}
			int oldPeriod = Preferences.getAutosyncPeriod(PreferenceActivity.this);
			for (DeveloperAccount account : developerAccounts) {
				// If syncing is currently on, or it used to be app wide off
				// set the new period (and enable it)
				if (autosyncHandler.isAutosyncEnabled(account.getName()) || oldPeriod == 0) {
					autosyncHandler.setAutosyncPeriod(account.getName(), newPeriod);
				}
			}
		} else if (key.equals(Preferences.DATE_FORMAT_LONG)) {
			Preferences.clearCachedDateFormats();
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);


		// Make sure we are consistent with any system changes/changes in that account
		// specific sections
		boolean anyEnabled = false;
		for (DeveloperAccount account : developerAccounts) {
			if (autosyncHandler.isAutosyncEnabled(account.getName())) {
				anyEnabled = true;
				break;
			}
		}
		if (anyEnabled) {
			// At least one account is enabled, so this should show
			// the sync period
			autosyncPref.setValue(Integer.toString(Preferences
					.getLastNonZeroAutosyncPeriod(PreferenceActivity.this)));
		} else {
			// All the accounts are disabled, so set it to 0
			autosyncPref.setValue("0");
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Generic code to provide summaries for preferences based on their values
	// Overkill at the moment, but may be useful in the future as we add more options

	private void initSummary(Preference p) {
		if (p instanceof PreferenceCategory) {
			PreferenceCategory pCat = (PreferenceCategory) p;
			for (int i = 0; i < pCat.getPreferenceCount(); i++) {
				initSummary(pCat.getPreference(i));
			}
		} else {
			updatePrefSummary(p);
		}

	}

	private void updatePrefSummary(Preference p) {
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			p.setSummary(listPref.getEntry());
		} else if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key));

	}

	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
				this);
	}

}
