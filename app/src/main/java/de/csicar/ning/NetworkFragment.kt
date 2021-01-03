package de.csicar.ning

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import de.csicar.ning.ui.RecyclerViewCommon
import de.csicar.ning.util.AppPreferences
import de.csicar.ning.util.CopyUtil
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
        
        val copyUtil = CopyUtil(view)


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
                    val deviceIcon: ImageView = view.device_icon

                    copyUtil.makeTextViewCopyable(macTextView)

                    return { item ->
                        ipTextView.text = item.ip.hostAddress
                        macTextView.text = item.hwAddress?.getAddress(
                            AppPreferences(
                                this@NetworkFragment
                            ).hideMacDetails)
                        vendorTextView.text = item.vendorName
                        deviceNameTextView.text = if (item.isScanningDevice) {
                            getString(R.string.this_device)
                        } else {
                            item.deviceName
                        }
                        deviceIcon.setImageResource(item.deviceType.icon)
                    }
                }

                override fun onClickListener(view: View, value: DeviceWithName) {
                    listener?.onListFragmentInteraction(value, view)
                }

                override fun onLongClickListener(view: View, value: DeviceWithName): Boolean {
                    return copyUtil.copyText(value.ip.hostAddress)
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
            val network = viewModel.startScan(argumentInterfaceName)
            val view = this@NetworkFragment.view
            if (network == null && view != null) {
                Snackbar.make(view, getString(R.string.error_network_not_found), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
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
