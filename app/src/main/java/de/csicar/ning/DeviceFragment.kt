package de.csicar.ning

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_device_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DeviceFragment.OnListFragmentInteractionListener] interface.
 */
class DeviceFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device_list, container, false)
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

        viewModel.deviceDao.getAll().observe(this@DeviceFragment, Observer {
            viewAdapter.updateData(it)
        })

        return view
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
