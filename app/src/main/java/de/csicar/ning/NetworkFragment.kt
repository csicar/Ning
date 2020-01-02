package de.csicar.ning

import android.content.Context
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [NetworkFragment.OnListFragmentInteractionListener] interface.
 */
class NetworkFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null
    private val network by lazy { MutableLiveData<Network>() }
    private val viewModel by lazy {
        ViewModelProviders.of(activity!!).get(ScanViewModel::class.java)
    }
    lateinit var viewAdapter: DeviceRecyclerViewAdapter
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var argumentInterfaceName : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network_list, container, false)
        val emptyListInfo = view.findViewById<View>(R.id.swipeDownViewImage)
        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeDownView)
        argumentInterfaceName = arguments?.getString("interface_name")!!


        // Set the adapter
        viewAdapter = DeviceRecyclerViewAdapter(listOf(), listener) { list ->
            emptyListInfo.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        }

        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        viewModel.scanProgress.observe(this, Observer {
            when (it) {
                is ScanViewModel.ScanProgress.ScanFinished -> progressBar.visibility = View.GONE
                is ScanViewModel.ScanProgress.ScanRunning -> {
                    progressBar.progress = (it.progress * 1000.0).roundToInt()
                    if (it.progress > 0.9) swipeRefreshLayout.isRefreshing = false
                }
                is ScanViewModel.ScanProgress.ScanNotStarted -> progressBar.visibility = View.GONE
            }
        })

        val devicesList = view.findViewById<RecyclerView>(R.id.devicesList)
        with(devicesList) {
            layoutManager = LinearLayoutManager(view.context)
            adapter = viewAdapter
        }

        swipeRefreshLayout.setOnRefreshListener {
            runScan()
        }


        setupObserver()
        return view
    }

    private fun runScan() {
        viewModel.viewModelScope.launch {

            val network = viewModel.startScan(argumentInterfaceName)
            this@NetworkFragment.network.value = network
            setupObserver()
        }
    }

    private fun setupObserver() {
        val networkId = viewModel.networkId.value ?: return
        viewModel.deviceDao.getAll(networkId).observe(this@NetworkFragment, Observer {
            viewAdapter.updateData(it)
        })
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
