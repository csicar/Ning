package de.csicar.ning

import android.os.Bundle
import android.util.Log
import android.view.SubMenu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.internal.view.SupportMenuItem.SHOW_AS_ACTION_ALWAYS
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.launch
import androidx.navigation.ui.navigateUp
import de.csicar.ning.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(), NetworkFragment.OnListFragmentInteractionListener {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val viewModel: ScanViewModel by viewModels()
    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navController = findNavController(R.id.nav_host_fragment)
        binding.drawerNavigation.setupWithNavController(navController)
        setSupportActionBar(binding.toolbar)
        appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.deviceFragment, R.id.appPreferenceFragment))
            .setDrawerLayout(binding.mainDrawerLayout)
            .build()
        setupActionBarWithNavController(navController, appBarConfiguration)

        val interfaceMenu = binding.drawerNavigation.menu.addSubMenu(getString(R.string.interfaces_submenu))

        viewModel.fetchAvailableInterfaces().forEach { nic ->
            interfaceMenu.add("${nic.interfaceName} - ${nic.address.hostAddress}/${nic.prefix}").also {
                it.setOnMenuItemClickListener {
                    val bundle = bundleOf("interface_name" to nic.interfaceName)
                    findNavController(R.id.nav_host_fragment).navigate(R.id.deviceFragment, bundle)
                    binding.mainDrawerLayout.closeDrawers()
                    true
                }
                it.setIcon(R.drawable.ic_settings_ethernet_white_24dp)
                it.isCheckable = true
                it.isEnabled = true
            }
        }
        val preferences = binding.drawerNavigation.menu.add(getString(R.string.preferences_submenu))
        preferences.setIcon(R.drawable.ic_settings_white_24dp)
        preferences.setOnMenuItemClickListener {
            navController.navigate(R.id.appPreferenceFragment)
            binding.mainDrawerLayout.closeDrawers()
            true
        }
    }


    override fun onListFragmentInteraction(item: DeviceWithName?, view: View) {
        val bundle = bundleOf("deviceId" to item?.deviceId, "deviceIp" to item?.ip)
        findNavController(R.id.nav_host_fragment).navigate(R.id.deviceInfoFragment, bundle)
    }
}
