package com.aramin.spinner

import android.app.AlertDialog
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var wheel: WheelView
    private lateinit var tvResult: TextView
    private lateinit var lvOptions: ListView
    private lateinit var adapter: OptionAdapter
    private val items = mutableListOf<String>()

    private var soundPool: SoundPool? = null
    private var sndTick = 0
    private var sndWin = 0
    private var sndSpin = 0
    private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wheel = findViewById(R.id.wheel)
        tvResult = findViewById(R.id.tv_result)
        lvOptions = findViewById(R.id.lv_options)
        val etAdd = findViewById<EditText>(R.id.et_add)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
            .also { sp ->
                sndTick = sp.load(this, R.raw.tick, 1)
                sndWin = sp.load(this, R.raw.win, 1)
                sndSpin = sp.load(this, R.raw.spin, 1)
                sp.setOnLoadCompleteListener { _, _, _ -> ready = true }
            }

        load()
        adapter = OptionAdapter()
        lvOptions.adapter = adapter

        wheel.onTick = {
            if (ready) soundPool?.play(sndTick, 1f, 1f, 1, 0, 1f)
        }
        wheel.onFinished = { winner ->
            tvResult.text = "🎉 $winner"
            tvResult.visibility = View.VISIBLE
            if (ready) soundPool?.play(sndWin, 1f, 1f, 2, 0, 1f)
        }

        findViewById<Button>(R.id.btn_spin).setOnClickListener {
            if (items.size < 2) { toast("حداقل ۲ گزینه لازم است"); return@setOnClickListener }
            tvResult.visibility = View.GONE
            if (ready) soundPool?.play(sndSpin, 1f, 1f, 1, 0, 1f)
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
    }

    inner class OptionAdapter : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(p: Int) = items[p]
        override fun getItemId(p: Int) = p.toLong()
        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.option_row, parent, false)
            v.findViewById<TextView>(R.id.tv_name).text = items[pos]
            v.findViewById<ImageView>(R.id.btn_delete).setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(items[pos])
                    .setMessage("حذف این گزینه؟")
                    .setPositiveButton("حذف") { _, _ ->
                        if (pos < items.size) {
                            items.removeAt(pos)
                            adapter.notifyDataSetChanged()
                            wheel.items = items
                            save()
                        }
                    }
                    .setNegativeButton("انصراف", null)
                    .show()
            }
            return v
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool?.release()
    }

    private fun prefs() = getSharedPreferences("wheel", Context.MODE_PRIVATE)
    private fun save() {
        val arr = org.json.JSONArray()
        items.forEach { arr.put(it) }
        prefs().edit().putString("items", arr.toString()).apply()
    }
    private fun load() {
        val raw = prefs().getString("items", null) ?: return
        try {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) items.add(arr.getString(i))
        } catch (_: Exception) {}
        wheel.items = items
    }
    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP, 0, 120)
        }.show()
}
