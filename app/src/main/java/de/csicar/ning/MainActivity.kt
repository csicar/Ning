package de.csicar.ning

import android.os.Bundle
import android.util.Log
import android.view.SubMenu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), NetworkFragment.OnListFragmentInteractionListener {
    lateinit var viewModel: ScanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration.Builder(navController.graph)
            .setDrawerLayout(null)
            .setFallbackOnNavigateUpListener { false }
            .build()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar
            .setupWithNavController(navController, appBarConfiguration)


        viewModel = ViewModelProviders.of(this).get(ScanViewModel::class.java)

        viewModel.currentNetworks.observe(this, Observer {
            val interfaceMenu = drawer_navigation.menu
            interfaceMenu.clear()
            it.forEach {
                interfaceMenu.add(it.interfaceName)
            }
        })
    }


    override fun onListFragmentInteraction(item: DeviceWithName?, view: View) {
        val bundle = bundleOf("deviceId" to item?.deviceId, "deviceIp" to item?.ip)
        nav_host_fragment.findNavController().navigate(R.id.deviceInfoFragment, bundle)
    }
}
