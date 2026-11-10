package com.neerly.mobile.feature.vendor.earnings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes a payout file into the app cache and hands it to another app through the same
 * FileProvider as the door photos (`<app>.photos`, `payout-files/`). Nothing is kept on
 * shared storage; the shop saves it wherever they like from the chooser.
 */
internal object PayoutFiles {
    private fun write(context: Context, name: String, text: String): File {
        val dir = File(context.cacheDir, "payout-files").apply { mkdirs() }
        // Only one payout file is useful at a time — clear the last ones.
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, name).apply { writeText(text, Charsets.UTF_8) }
    }

    private fun uri(context: Context, f: File) = FileProvider.getUriForFile(context, "${context.packageName}.photos", f)

    /** Open the CSV in a spreadsheet app; with none installed, offer to share/save it instead. */
    fun openCsv(context: Context, name: String, text: String, chooserTitle: String) {
        val u = uri(context, write(context, name, text))
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(u, "text/csv").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { context.startActivity(Intent.createChooser(view, chooserTitle)) } catch (_: ActivityNotFoundException) { share(context, u, "text/csv", chooserTitle) }
    }

    /** Share or save the fee invoice (HTML) — mail it to the accountant, keep it in Drive. */
    fun shareInvoice(context: Context, name: String, html: String, chooserTitle: String) =
        share(context, uri(context, write(context, name, html)), "text/html", chooserTitle)

    private fun share(context: Context, u: android.net.Uri, type: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).setType(type).putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(Intent.createChooser(send, title)) }
    }
}
