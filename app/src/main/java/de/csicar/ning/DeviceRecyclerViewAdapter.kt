package de.csicar.ning

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView


import de.csicar.ning.DeviceFragment.OnListFragmentInteractionListener

import kotlinx.android.synthetic.main.fragment_device.view.*

class DeviceRecyclerViewAdapter(
    private var mValues: List<DeviceWithName>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<DeviceRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as DeviceWithName
            mListener?.onListFragmentInteraction(item, v)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.ipTextView.text = item.ip.toString()
        holder.macTextView.text = item.hwAddress?.address
        holder.vendorTextView.text = item.vendorName

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    fun updateData(value : List<DeviceWithName>) {
        this.mValues = value
        this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val ipTextView: TextView = mView.ipTextView
        val macTextView: TextView = mView.macTextView
        val vendorTextView: TextView = mView.vendorTextView

        override fun toString(): String {
            return super.toString() + " '" + macTextView.text + "'"
        }
    }
}
