package com.harsh.parkinson.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harsh.parkinson.R
import com.harsh.parkinson.adapter.TremorDataAdapter
import com.harsh.parkinson.database.AppDatabase
import com.harsh.parkinson.database.ResultDao
import kotlinx.android.synthetic.main.tremorhistory.toolBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TremorDataHistory : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TremorDataAdapter
    private lateinit var resultDao: ResultDao
    lateinit var clear:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tremorhistory)
        clear=findViewById(R.id.clear)
        clear.setOnClickListener {
            clearHistory()
        }

        resultDao = AppDatabase.getInstance(this).resultDao()

        setToolBar()
        initRecyclerView()
        loadData()
    }

    private fun setToolBar() {
        setSupportActionBar(toolBar)
        supportActionBar?.title = "Tremors Data History"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
    }

    private fun initRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = TremorDataAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadData() {
        lifecycleScope.launch {
            val tremorList = withContext(Dispatchers.IO) {
                resultDao.getAllResults()
            }
            adapter.updateData(tremorList)
        }
    }
    private fun clearHistory() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                resultDao.deleteAllResults()
            }
            adapter.clearData()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

