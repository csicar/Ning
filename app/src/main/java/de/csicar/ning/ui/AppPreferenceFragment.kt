package de.csicar.ning.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.csicar.ning.R

class AppPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preference_fragment, rootKey)
    }

}