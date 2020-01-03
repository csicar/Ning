package de.csicar.ning.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class UsefulRecyclerView : RecyclerView {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        ctx,
        attrs,
        defStyleAttr
    )

    abstract class Handler<T>(val data: LiveData<List<T>>) {
        abstract fun bindItem(view: View): (value: T) -> Unit
        abstract fun getLayout(): Int
        open fun onClickListener(view: View, value :T) {}

        open fun shareIdentity(a: T, b: T) = false
        open fun areContentTheSame(a: T, b: T) = a === b

    }

    fun <T> setHandler(context: Context, owner: LifecycleOwner, handler: Handler<T>) {
        layoutManager = LinearLayoutManager(context)
        val adapter = UsefulViewAdapter(listOf(), handler)
        this.adapter = adapter
        handler.data.observe(owner, Observer {
            val diffUtil = DiffUtil.calculateDiff(DiffCalculator(adapter.values, it, handler))
            adapter.values = it
            diffUtil.dispatchUpdatesTo(adapter)
        })
    }

    data class UsefulViewHolder<T>(val view: View, val binder: (value: T) -> Unit) :
        ViewHolder(view)

    class UsefulViewAdapter<T>(var values: List<T>, val handler: Handler<T>) :
        RecyclerView.Adapter<UsefulViewHolder<T>>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsefulViewHolder<T> {
            val view = LayoutInflater.from(parent.context)
                .inflate(handler.getLayout(), parent, false)
            return UsefulViewHolder(view, handler.bindItem(view))
        }

        override fun getItemCount() = values.size

        override fun onBindViewHolder(holder: UsefulViewHolder<T>, i: Int) {
            val value = values[i]
            holder.binder(value)
            holder.view.tag = value
            holder.view.setOnClickListener {
                handler.onClickListener(it, it.tag as T)
            }
        }

    }

    class DiffCalculator<T>(
        private val old: List<T>,
        private val new: List<T>,
        private val handler: Handler<T>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            handler.shareIdentity(old[oldItemPosition], new[newItemPosition])

        override fun getOldListSize() = old.size

        override fun getNewListSize() = new.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            handler.areContentTheSame(old[oldItemPosition], new[newItemPosition])

    }
}