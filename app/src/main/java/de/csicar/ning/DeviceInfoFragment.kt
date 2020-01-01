package de.csicar.ning

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DeviceInfoFragment.OnListFragmentInteractionListener] interface.
 */
class DeviceInfoFragment : Fragment() {
    lateinit var viewModel: ScanViewModel
    lateinit var adapter: PortItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_deviceinfo_list, container, false)
        viewModel = ViewModelProviders.of(activity!!).get(ScanViewModel::class.java)
        viewModel.deviceDao.getById(arguments?.getLong("deviceId")!!).observe(this, Observer {
            fetchInfo(it)
            //activity!!.toolbar.findViewById<TextView>(R.id.title_detail).text = "${it.ip} ${it.deviceName}"

            viewModel.portDao.getAllForDevice(it.deviceId).observe(this, Observer {
                adapter.updateData(it)
            })
        })


        // Set the adapter
        adapter = PortItemAdapter(listOf()) { port ->
            viewModel.viewModelScope.launch {
                val device =
                    withContext(Dispatchers.IO) { viewModel.deviceDao.getByIdNow(port.deviceId) }
                when (port.port) {
                    8080 ->
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("http://${device.ip}:${port.port}")
                        }.also {
                            startActivity(it)
                        }
                    22 ->
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("ssh://${device.ip}:${port.port}")
                        }.also {
                            startActivity(it)
                        }
                }
            }
        }
        view.findViewById<RecyclerView>(R.id.list).also {

            it.layoutManager = LinearLayoutManager(context)
            it.adapter = adapter
        }
        return view
    }

    fun fetchInfo(device: Device) {
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                device.scanPorts().forEach {
                    launch {
                        val (port, isOpen) = it.await()
                        if (isOpen) {
                            viewModel.portDao.insert(
                                Port(
                                    0,
                                    port.port,
                                    Protocol.TCP,
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