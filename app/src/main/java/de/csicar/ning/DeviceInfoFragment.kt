package de.csicar.ning

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import de.csicar.ning.scanner.PortScanner
import de.csicar.ning.ui.RecyclerViewCommon
import de.csicar.ning.util.AppPreferences
import de.csicar.ning.util.CopyUtil
import kotlinx.android.synthetic.main.fragment_port_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
        val copyUtil = CopyUtil(view)

        val deviceIpTextView = view.findViewById<TextView>(R.id.deviceIpTextView)
        val deviceNameTextView = view.findViewById<TextView>(R.id.deviceNameTextView)
        val deviceHwAddressTextView = view.findViewById<TextView>(R.id.deviceHwAddressTextView)
        val deviceVendorTextView = view.findViewById<TextView>(R.id.deviceVendorTextView)

        copyUtil.makeTextViewCopyable((deviceIpTextView))
        copyUtil.makeTextViewCopyable(deviceNameTextView)
        copyUtil.makeTextViewCopyable(deviceHwAddressTextView)
        copyUtil.makeTextViewCopyable(deviceVendorTextView)

        viewModel.deviceDao.getById(argumentDeviceId).observe(this, Observer {
            fetchInfo(it.asDevice)
            deviceIpTextView.text = it.ip.hostAddress
            deviceNameTextView.text = it.deviceName
            deviceHwAddressTextView.text =
                it.hwAddress?.getAddress(AppPreferences(this).hideMacDetails)
            deviceVendorTextView.text = it.vendorName
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


            override fun onLongClickListener(view: View, value: Port): Boolean {
                viewModel.viewModelScope.launch(context = Dispatchers.IO) {
                    val ip = viewModel.deviceDao.getByIdNow(value.deviceId).ip
                    withContext(Dispatchers.Main) {
                        copyUtil.copyText("${ip.hostAddress}:${value.port}")
                    }
                }
                return true
            }

            override fun bindItem(view: View): (value: Port) -> Unit {
                val portNumberTextView: TextView = view.portNumberTextView
                val protocolTextView: TextView = view.protocolTextView
                val serviceTextView: TextView = view.serviceNameTextView

                copyUtil.makeTextViewCopyable(portNumberTextView)
                copyUtil.makeTextViewCopyable(protocolTextView)
                copyUtil.makeTextViewCopyable(serviceTextView)

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