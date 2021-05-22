@file:Suppress("DEPRECATION")

package com.osfans.trime.ui.dialogs

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.IBinder
import android.util.Log
import android.view.Window
import android.view.WindowManager
import com.osfans.trime.R
import com.osfans.trime.ime.Rime
import com.osfans.trime.ime.Trime
import com.osfans.trime.utils.Function
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

private val TITLE_TAG = SchemaDialog2::class.java.simpleName

/** Show the Schema List of Trime **/
class SchemaDialog2: CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var checkedSchemaItems: BooleanArray? = null
    private var schemaItems: Array<String?>? = null
    private var schemaNames: Array<String?>? = null
    private var availableSchemas: MutableList<Map<String, String>?>? = null
    private lateinit var loadingProgressDialog: ProgressDialog

    @Suppress("unused")
    constructor(context: Context): this(context, null)
    constructor(context: Context, token: IBinder?) {
        showLoadingProgressDialog(context, token)
        execute(context, token)
    }

    private class SortByName: Comparator<Map<String, String>?> {
        override fun compare(o1: Map<String, String>?, o2: Map<String, String>?): Int {
            val s1 = o1?.get("schema_id")
            val s2 = o2?.get("schema_id")
            return s1!!.compareTo(s2!!)

        }
    }

    private fun showLoadingProgressDialog(context: Context, token: IBinder?) {
        loadingProgressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.schemas_progress))
            setCancelable(false)
        }

        if (token != null) {
            val loadingProgressDialogWindow = loadingProgressDialog.window
            windowHandling(loadingProgressDialogWindow, token)
        }

        loadingProgressDialog.show()
    }

    private fun showSchemaDialog(context: Context, token: IBinder?) {
        val builder = AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_schemas)
            setCancelable(true)
            setPositiveButton(android.R.string.ok, null)
        }

        if (availableSchemas == null || availableSchemas!!.size == 0) {
            builder.setMessage(R.string.no_schemas)
        } else {
            builder.let {
                it.setMultiChoiceItems(schemaNames, checkedSchemaItems) { _, id, isChecked ->
                    checkedSchemaItems?.set(id, isChecked)
                }
                it.setNegativeButton(android.R.string.cancel, null)
                it.setPositiveButton(android.R.string.ok) { _, _ ->
                    loadingProgressDialog.apply {
                        setMessage(context.getString(R.string.deploy_progress))
                        show()
                    }
                    /*
                    Thread {
                        Runnable {
                            try {
                                selectSchema(context)
                            } catch (ex: Exception) {
                                Log.e(TITLE_TAG, "Select Schema $ex")
                            } finally {
                                loadingProgressDialog.dismiss()
                                exitProcess(0)
                            }
                        }
                    }*/
                    launch {
                        try {
                            selectSchema(context)
                        } catch (ex: Exception) {
                            Log.e(TITLE_TAG, "Select Schema $ex")
                        } finally {
                            loadingProgressDialog.dismiss()
                            exitProcess(0)
                        }
                    }
                }
            }

        }

        val schemaDialog = builder.create()
        if (token != null) {
            val schemaDialogWindow = schemaDialog.window
            windowHandling(schemaDialogWindow, token)
        }
        schemaDialog.show()
    }

    private fun windowHandling(window: Window?, token: IBinder?) {
        val layoutParams: WindowManager.LayoutParams? = window?.attributes
        layoutParams?.let {
            it.token = token
            it.type = Trime.getDialogType()
        }
        window?.let {
            it.attributes = layoutParams
            it.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    private fun initSchema() {
        availableSchemas = Rime.get_available_schema_list()
        Log.i(TITLE_TAG, availableSchemas.toString())
        if (availableSchemas == null || availableSchemas!!.size == 0) return
        availableSchemas?.sortWith(SortByName())

        val selectedSchemas: MutableList<Map<String, String>?>? = Rime.get_selected_schema_list()
        val selectedIds = ArrayList<String>()
        var schemaId: String?
        val n = availableSchemas!!.size
        schemaNames = arrayOfNulls(n)
        schemaItems = arrayOfNulls(n)
        checkedSchemaItems = BooleanArray(n)

        if (selectedSchemas?.isNotEmpty() == true) {
            for (o in selectedSchemas) {
                    o?.get("schema_id")?.let { selectedIds.add(it) }
                }
        }

        if (availableSchemas != null) {
            for ((i, o) in availableSchemas!!.withIndex()) {
                schemaNames?.set(i, o?.get("name"))
                schemaId = o?.get("schema_id")
                schemaItems?.set(i, schemaId)
                checkedSchemaItems?.set(i, selectedIds.contains(schemaId))
            }
        }

    }

    private fun selectSchema(context: Context) {
        val checkedIds = ArrayList<String>()
        for ((i, b) in checkedSchemaItems?.withIndex()!!) {
            if (b) schemaItems?.get(i)?.let { checkedIds.add(it) }
        }

        val n = checkedIds.size
        if (n > 0) {
            val schemaIdList = arrayOfNulls<String>(n)
            checkedIds.toArray(schemaIdList)
            Rime.select_schemas(schemaIdList)
            Function.deploy(context)
        }

    }

    private fun execute(context: Context, token: IBinder?) = launch {
        onPreExecute()
        doInBackground()
        onPostExecute(context, token)
    }

    private fun onPreExecute() {}

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
        initSchema()
        delay(1000)
        return@withContext "OK"
    }

    private fun onPostExecute(context: Context, token: IBinder?) {
        loadingProgressDialog.dismiss()
        showSchemaDialog(context, token)
    }
}