package com.osfans.trime

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import com.osfans.trime.Function.deploy
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class SchemaDialog(context: Context, token: IBinder? = null) : CoroutineScope {
    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var checkedSchemaItems: BooleanArray? = null
    private var schemaItems: Array<String?>? = null
    private var schemas: List<Map<String, String>?>? = null
    private var schemaNames: Array<String?>? = null
    private var mContext: Context? = null
    private var mToken: IBinder? = null
    private var mProgressDialog: ProgressDialog? = null
    private val TAG = SchemaDialog::class.java.simpleName

    init {
        mContext = context
        mToken = token
        showProgressDialog()
        execute()
    }

    fun execute() = launch {
        onPreExecute()
        val result = doInBackground()
        onPostExecute(result)
    }

    private fun onPreExecute() { }

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO){
        initSchema()
        delay(1000) // Simulate async work
        return@withContext "OK"
    }

    private fun onPostExecute(result: String) {
        mProgressDialog?.dismiss()
        showDialog()
    }

    private class SortByName: Comparator<Map<String, String>?> {
        override fun compare(o1: Map<String, String>?, o2: Map<String, String>?): Int {
            val s1: String? = o1?.get("schema_id")
            val s2: String? = o2?.get("schema_id")
            return s1!!.compareTo(s2!!)
        }
    }

    private fun selectSchema(context: Context) {
        val checkedIds: ArrayList<String> = ArrayList<String>()
        for ((i, b) in checkedSchemaItems!!.withIndex()) {
            if (b == true) checkedIds.add(schemaItems?.get(i)!!)
        }
        val n = checkedIds.size
        if (n > 0) {
            val schema_id_list = arrayOfNulls<String>(n)
            checkedIds.toArray<String>(schema_id_list)
            Rime.select_schemas(schema_id_list)
            deploy(context)
        }
    }

    private fun showProgressDialog() {
        mProgressDialog = ProgressDialog(mContext)
        mProgressDialog!!.setMessage(mContext!!.getString(R.string.schemas_progress))
        mProgressDialog!!.setCancelable(false)
        if (mToken != null) {
            val window = mProgressDialog?.window
            val lp = window?.attributes
            lp?.token = mToken
            lp?.type = Trime.getDialogType()
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
        mProgressDialog?.show()
    }

    private fun initSchema() {
        schemas = Rime.get_available_schema_list()
        if (schemas == null || schemas!!.size == 0) {
            //不能在線程中使用Toast
            //Toast.makeText(mContext, R.string.no_schemas, Toast.LENGTH_LONG).show();
            return
        }
        Collections.sort<Map<String, String>?>(schemas, SortByName())
        val selected_schemas = Rime.get_selected_schema_list()
        val selected_Ids: MutableList<String?> = ArrayList()
        val n = schemas!!.size
        schemaNames = arrayOfNulls(n)
        var schema_id: String
        checkedSchemaItems = BooleanArray(n)
        schemaItems = arrayOfNulls(n)
        if (selected_schemas.size > 0) {
            for (m in selected_schemas) {
                selected_Ids.add(m["schema_id"])
            }
        }
        for ((i, m) in schemas!!.withIndex()) {
            schemaNames!![i] = m?.get("name")!!
            schema_id = m["schema_id"].toString()
            schemaItems!![i] = schema_id
            checkedSchemaItems!![i] = selected_Ids.contains(schema_id)
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(mContext)
                .setTitle(R.string.pref_schemas)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, null)
        if (schemas == null || schemas!!.size == 0) {
            builder.setMessage(R.string.no_schemas)
        } else {
            builder.setMultiChoiceItems(
                    schemaNames,
                    checkedSchemaItems
            ) { di, id, isChecked -> checkedSchemaItems!![id] = isChecked }
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(
                    android.R.string.ok
            ) { di, id ->
                mProgressDialog!!.setMessage(mContext!!.getString(R.string.deploy_progress))
                mProgressDialog!!.show()
                Thread {
                    try {
                        selectSchema(mContext!!)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Select Schema$ex")
                    } finally {
                        mProgressDialog!!.dismiss()
                        System.exit(0) //清理內存
                    }
                }.start()
            }
        }
        val mDialog = builder.create()
        if (mToken != null) {
            val window = mDialog.window
            val lp = window?.attributes
            lp?.token = mToken
            lp?.type = Trime.getDialogType()
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
        mDialog.show()
    }

}