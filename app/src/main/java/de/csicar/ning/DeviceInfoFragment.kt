package de.csicar.ning

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import de.csicar.ning.scanner.PortScanner
import de.csicar.ning.ui.RecyclerViewCommon
import kotlinx.android.synthetic.main.fragment_port_item.view.*
import kotlinx.coroutines.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DeviceInfoFragment.OnListFragmentInteractionListener] interface.
 */
class DeviceInfoFragment : Fragment() {
    lateinit var viewModel: ScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_deviceinfo_list, container, false)
        viewModel = ViewModelProviders.of(activity!!).get(ScanViewModel::class.java)
        val recyclerView = view.findViewById<RecyclerViewCommon>(R.id.list)
        val argumentDeviceId = arguments?.getLong("deviceId")!!

        viewModel.deviceDao.getById(argumentDeviceId).observe(this, Observer {
            fetchInfo(it.asDevice)
            view.findViewById<TextView>(R.id.deviceIpTextView).text = it.ip.hostAddress
            view.findViewById<TextView>(R.id.deviceNameTextView).text = it.deviceName
            view.findViewById<TextView>(R.id.deviceHwAddressTextView).text = it.hwAddress?.getAddress(AppPreferences(this).hideMacDetails)
            view.findViewById<TextView>(R.id.deviceVendorTextView).text = it.vendorName
        })

        val ports = viewModel.portDao.getAllForDevice(argumentDeviceId)


        recyclerView.setHandler(context!!, this, object :
            RecyclerViewCommon.Handler<Port>(R.layout.fragment_port_item, ports) {
            override fun shareIdentity(a: Port, b: Port) = a.port == b.port
            override fun areContentsTheSame(a: Port, b: Port) = a == b
            override fun onClickListener(view: View, value: Port) {
                viewModel.viewModelScope.launch(context = Dispatchers.IO) {
                    val ip = viewModel.deviceDao.getByIdNow(value.deviceId).ip
                    withContext(Dispatchers.Main) {
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("http://${ip}:${value.port}")
                        }.also {
                            startActivity(it)
                        }
                    }
                }
            }

            override fun bindItem(view: View): (value: Port) -> Unit {
                val portNumberTextView: TextView = view.portNumberTextView
                val protocolTextView: TextView = view.protocolTextView
                val serviceTextView: TextView = view.serviceNameTextView

                return { item ->
                    portNumberTextView.text = item.port.toString()
                    protocolTextView.text = item.protocol.toString()
                    serviceTextView.text = item.description?.serviceName

                }
            }

        })
        return view
    }

    fun fetchInfo(device: Device) {
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PortScanner(device.ip).scanPorts().forEach {
                    launch {
                        val result = it.await()
                        if (result.isOpen) {
                            viewModel.portDao.upsert(
                                Port(
                                    0,
                                    result.port,
                                    result.protocol,
                                    device.deviceId
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}