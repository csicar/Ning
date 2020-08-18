package de.csicar.ning.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

class CopyUtil(val rootView: View) {

    fun makeTextViewCopyable(textView: TextView) {
        textView.setOnLongClickListener {
            val textContent = textView.text.toString()
            copyText(textContent)
        }
    }

    fun copyText(text: String): Boolean {
        val clipboard = rootView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(text, text)
        clipboard?.setPrimaryClip(clip)
        val snackbar = Snackbar.make(rootView, "Copied \"$text\" to the Clipboard", Snackbar.LENGTH_LONG)
        snackbar.show()
        return true
    }
}

