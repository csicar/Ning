package de.csicar.ning

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [NetworkFragment.OnListFragmentInteractionListener] interface.
 */
class NetworkFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null
    private val network by lazy { MutableLiveData<Network>() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_network_list, container, false)
        val viewModel = ViewModelProviders.of(activity!!).get(ScanViewModel::class.java)

        val spinnerAdapter = ArrayAdapter<String>(context!!, R.layout.spinner_item, R.id.interface_select_title, arrayOf<String>())
        activity?.findViewById<Spinner>(R.id.interface_selection)?.adapter = spinnerAdapter

        // Set the adapter
        val viewAdapter = DeviceRecyclerViewAdapter(listOf(), listener)
        val devicesList = view.findViewById<RecyclerView>(R.id.devicesList)
        with(devicesList) {
            layoutManager = LinearLayoutManager(view.context)
            adapter = viewAdapter
        }

        viewModel.viewModelScope.launch {

            val network = viewModel.startScan("wlan0")
            this@NetworkFragment.network.value = network
            viewModel.deviceDao.getAll(network.networkId).observe(this@NetworkFragment, Observer {
                viewAdapter.updateData(it)
            })
        }



        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        network.observe(this@NetworkFragment, Observer {
            activity!!.toolbar.findViewById<TextView>(R.id.title_detail).text = it.interfaceName
        })

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
