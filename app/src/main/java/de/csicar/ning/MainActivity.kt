package de.csicar.ning

import android.os.Bundle
import android.util.Log
import android.view.SubMenu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.internal.view.SupportMenuItem.SHOW_AS_ACTION_ALWAYS
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import androidx.navigation.ui.navigateUp


class MainActivity : AppCompatActivity(), NetworkFragment.OnListFragmentInteractionListener {
    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var viewModel: ScanViewModel
    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp(appBarConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navController = findNavController(R.id.nav_host_fragment)
        drawer_navigation.setupWithNavController(navController)
        setSupportActionBar(toolbar)
        appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.deviceFragment, R.id.appPreferenceFragment))
            .setDrawerLayout(main_drawer_layout)
            .build()
        setupActionBarWithNavController(navController, appBarConfiguration)

        viewModel = ViewModelProviders.of(this).get(ScanViewModel::class.java)

        val interfaceMenu = drawer_navigation.menu.addSubMenu(getString(R.string.interfaces_submenu))

        viewModel.fetchAvailableInterfaces().forEach { nic ->
            interfaceMenu.add("${nic.interfaceName} - ${nic.address.hostAddress}/${nic.prefix}").also {
                it.setOnMenuItemClickListener {
                    val bundle = bundleOf("interface_name" to nic.interfaceName)
                    nav_host_fragment.findNavController().navigate(R.id.deviceFragment, bundle)
                    main_drawer_layout.closeDrawers()
                    true
                }
                it.setIcon(R.drawable.ic_settings_ethernet_white_24dp)
                it.isCheckable = true
                it.isEnabled = true
            }
        }
        val preferences = drawer_navigation.menu.add(getString(R.string.preferences_submenu))
        preferences.setIcon(R.drawable.ic_settings_white_24dp)
        preferences.setOnMenuItemClickListener {
            navController.navigate(R.id.appPreferenceFragment)
            main_drawer_layout.closeDrawers()
            true
        }
    }


    override fun onListFragmentInteraction(item: DeviceWithName?, view: View) {
        val bundle = bundleOf("deviceId" to item?.deviceId, "deviceIp" to item?.ip)
        nav_host_fragment.findNavController().navigate(R.id.deviceInfoFragment, bundle)
    }
}
