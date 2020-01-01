package de.csicar.ning


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_port_item.view.*

/**
 * [RecyclerView.Adapter] that can display a [DummyItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 * TODO: Replace the implementation with code for your data type.
 */
class PortItemAdapter(
    private var mValues: List<Port>,
    private val mListener: (Port) -> Unit
) : RecyclerView.Adapter<PortItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_port_item, parent, false)
        return ViewHolder(view)
    }

    fun updateData(value : List<Port>) {
        this.mValues = value
        this.notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.portNumberTextView.text = item.port.toString()
        holder.protocolTextView.text = item.protocol.toString()
        holder.serviceTextView.text = item.description?.serviceName

        with(holder.mView) {
            tag = item
            setOnClickListener { mListener(item) }
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val portNumberTextView: TextView = mView.portNumberTextView
        val protocolTextView: TextView = mView.protocolTextView
        val serviceTextView: TextView = mView.serviceNameTextView

        override fun toString(): String {
            return super.toString() + " '" + protocolTextView.text + "'"
        }
    }
}
