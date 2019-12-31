package de.csicar.ning


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_deviceinfo.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class DeviceInfoRecyclerViewAdapter(
    private var mValues: List<Port>
) : RecyclerView.Adapter<DeviceInfoRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_deviceinfo, parent, false)
        return ViewHolder(view)
    }

    fun updateData(value : List<Port>) {
        this.mValues = value
        this.notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mIdView.text = item.port.toString()
        holder.mContentView.text = item.protocol.toString()

        with(holder.mView) {
            tag = item
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.ipTextView
        val mContentView: TextView = mView.macTextView

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
