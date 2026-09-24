package com.aramin.spinner

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var wheel: WheelView
    private lateinit var tvResult: TextView
    private lateinit var lvOptions: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val items = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wheel = findViewById(R.id.wheel)
        tvResult = findViewById(R.id.tv_result)
        lvOptions = findViewById(R.id.lv_options)
        val etAdd = findViewById<EditText>(R.id.et_add)
        val btnSpin = findViewById<Button>(R.id.btn_spin)

        load()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        lvOptions.adapter = adapter

        wheel.onFinished = { winner ->
            tvResult.text = "🎉 $winner"
            tvResult.visibility = View.VISIBLE
        }

        btnSpin.setOnClickListener {
            if (items.size < 2) {
                toast("حداقل ۲ گزینه لازم است")
                return@setOnClickListener
            }
            tvResult.visibility = View.GONE
            wheel.spin()
        }

        findViewById<Button>(R.id.btn_add).setOnClickListener {
            val text = etAdd.text.toString().trim()
            if (text.isEmpty()) { toast("متن خالی است"); return@setOnClickListener }
            if (items.contains(text)) { toast("تکراری است"); return@setOnClickListener }
            items.add(text)
            adapter.notifyDataSetChanged()
            wheel.items = items
            etAdd.setText("")
            save()
        }

        lvOptions.onItemLongClickListener = { _, _, pos, _ ->
            AlertDialog.Builder(this)
                .setTitle(items[pos])
                .setMessage("حذف این گزینه؟")
                .setPositiveButton("حذف") { _, _ ->
                    items.removeAt(pos)
                    adapter.notifyDataSetChanged()
                    wheel.items = items
                    save()
                }
                .setNegativeButton("انصراف", null)
                .show()
            true
        }
    }

    private fun prefs() = getSharedPreferences("wheel", Context.MODE_PRIVATE)
    private fun save() {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        prefs().edit().putString("items", arr.toString()).apply()
    }
    private fun load() {
        val raw = prefs().getString("items", null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) items.add(arr.getString(i))
        } catch (_: Exception) {}
        wheel.items = items
    }
    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP, 0, 120)
        }.show()
}
