package de.csicar.ning

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import de.csicar.ning.ui.RecyclerViewCommon
import kotlinx.android.synthetic.main.fragment_device.view.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [NetworkFragment.OnListFragmentInteractionListener] interface.
 */
class NetworkFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null
    private val viewModel by lazy {
        ViewModelProviders.of(activity!!).get(ScanViewModel::class.java)
    }
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var emptyListInfo: View

    private lateinit var argumentInterfaceName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network_list, container, false)
        emptyListInfo = view.findViewById<View>(R.id.swipeDownViewImage)
        swipeRefreshLayout = view.findViewById(R.id.swipeDownView)
        argumentInterfaceName = arguments?.getString("interface_name")!!


        viewModel.devices.observe(this, Observer {
            emptyListInfo.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        })

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        viewModel.scanProgress.observe(this, Observer {
            when (it) {
                is ScanRepository.ScanProgress.ScanFinished -> {
                    progressBar.visibility = View.INVISIBLE
                    swipeRefreshLayout.isRefreshing = false
                }
                is ScanRepository.ScanProgress.ScanRunning -> {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = (it.progress * 1000.0).roundToInt()
                }
                is ScanRepository.ScanProgress.ScanNotStarted -> progressBar.visibility =
                    View.INVISIBLE
            }
        })

        val devicesList = view.findViewById<RecyclerViewCommon>(R.id.devicesList)
        devicesList.setHandler(
            context!!,
            this,
            object : RecyclerViewCommon.Handler<DeviceWithName>(
                R.layout.fragment_device,
                viewModel.devices
            ) {
                override fun bindItem(view: View): (DeviceWithName) -> Unit {
                    val ipTextView: TextView = view.ipTextView
                    val macTextView: TextView = view.macTextView
                    val vendorTextView: TextView = view.vendorTextView
                    val deviceNameTextView: TextView = view.deviceNameTextView
                    return { item ->
                        ipTextView.text = item.ip.hostAddress
                        macTextView.text = item.hwAddress?.address
                        vendorTextView.text = item.vendorName
                        deviceNameTextView.text = item.deviceName
                    }
                }

                override fun onClickListener(view: View, value: DeviceWithName) {
                    listener?.onListFragmentInteraction(value, view)
                }

                override fun shareIdentity(a: DeviceWithName, b: DeviceWithName) =
                    a.deviceId == b.deviceId

                override fun areContentsTheSame(a: DeviceWithName, b: DeviceWithName) = a == b

            })

        swipeRefreshLayout.setOnRefreshListener {
            runScan()
        }

        return view
    }
    private fun runScan() {
        viewModel.viewModelScope.launch {
            viewModel.startScan(argumentInterfaceName)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: DeviceWithName?, view: View)
    }
}
