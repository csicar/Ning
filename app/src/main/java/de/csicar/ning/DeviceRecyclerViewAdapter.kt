package de.csicar.ning

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil


import de.csicar.ning.NetworkFragment.OnListFragmentInteractionListener

import kotlinx.android.synthetic.main.fragment_device.view.*

class DeviceRecyclerViewAdapter(
    private var mValues: List<DeviceWithName>,
    private val mListener: OnListFragmentInteractionListener?,
    private val onDataChanged: ((List<DeviceWithName>) -> Unit)? = null
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
        holder.deviceNameTextView.text = item.deviceName

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    fun updateData(newValue: List<DeviceWithName>) {
        val diffUtil = DiffUtil.calculateDiff(DiffCalculator(mValues, newValue))
        this.mValues = newValue
        diffUtil.dispatchUpdatesTo(this)
        onDataChanged?.let { it(newValue) }
        //this.notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val ipTextView: TextView = mView.ipTextView
        val macTextView: TextView = mView.macTextView
        val vendorTextView: TextView = mView.vendorTextView
        val deviceNameTextView: TextView = mView.deviceNameTextView

        override fun toString(): String {
            return super.toString() + " '" + macTextView.text + "'"
        }
    }

    class DiffCalculator(
        private val old: List<DeviceWithName>,
        private val new: List<DeviceWithName>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition].deviceId == new[newItemPosition].deviceId

        override fun getOldListSize() = old.size

        override fun getNewListSize() = new.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] === new[newItemPosition]

    }
}
