package de.csicar.ning

import android.content.Context
import android.opengl.Visibility
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

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
    lateinit var emptyListInfo : View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network_list, container, false)
        val emptyListInfo = view.findViewById<View>(R.id.swipeDownViewImage)
        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeDownView)


        // Set the adapter
        viewAdapter = DeviceRecyclerViewAdapter(listOf(), listener) { list ->
            emptyListInfo.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        }
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

            val network = viewModel.startScan(arguments?.getString("interface_name")!!)
            this@NetworkFragment.network.value = network
            setupObserver()
        }
        swipeRefreshLayout.isRefreshing = false
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
