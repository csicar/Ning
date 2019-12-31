package de.csicar.ning

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import de.csicar.ning.scanner.getArpTableFromFile
import de.csicar.ning.scanner.pingIpAddresses
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException


class MainActivity : AppCompatActivity(), DeviceFragment.OnListFragmentInteractionListener {
    lateinit var viewModel: ScanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration.Builder(navController.graph)
            .setDrawerLayout(null)
            .setFallbackOnNavigateUpListener { false }
            .build()
        findViewById<Toolbar>(R.id.toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        viewModel = ViewModelProviders.of(this).get(ScanViewModel::class.java)
        viewModel.viewModelScope.launch {

            viewModel.startScan()
        }
    }


    override fun onListFragmentInteraction(item: Device?, view: View) {
        val bundle = bundleOf("deviceId" to item?.deviceId, "deviceIp" to item?.ip)
        nav_host_fragment.findNavController().navigate(R.id.deviceInfoFragment, bundle)
    }
}
