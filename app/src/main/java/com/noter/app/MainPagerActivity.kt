package com.noter.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.noter.app.adapter.MainPagerAdapter
import com.noter.app.data.DataManager
import com.noter.app.databinding.ActivityMainPagerBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import android.widget.PopupMenu

class MainPagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainPagerBinding
    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var dataManager: DataManager

    // Регистрируем результат для выбора файла для экспорта
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                exportDataToFile(uri)
            }
        }
    }

    // Регистрируем результат для выбора файла для импорта
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { importDataFromFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = DataManager(this)
        setupViewPager()
    }

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Отключаем overscroll эффект
        binding.viewPager.overScrollMode = ViewPager2.OVER_SCROLL_NEVER
        
        // Устанавливаем начальную страницу (центральный основной экран)
        binding.viewPager.setCurrentItem(1, false)
    }

    // Menu is handled inside each fragment's info block now (MainFragment, OverdueNotesFragment, FutureNotesFragment)

    fun exportData() {
        try {
            exportDataToFile()
        } catch (e: Exception) {
            android.util.Log.e("MainPagerActivity", "Export error", e)
            Toast.makeText(this, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
        }
    }

    fun importData() {
        try {
            importLauncher.launch("application/json")
        } catch (e: Exception) {
            android.util.Log.e("MainPagerActivity", "Import error", e)
            Toast.makeText(this, getString(R.string.import_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportDataToFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "taskora_backup.json")
        }
        exportLauncher.launch(intent)
    }

    private fun exportDataToFile(uri: Uri) {
        try {
            val jsonData = dataManager.exportAllData()
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonData)
                }
            }
            Toast.makeText(this, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("MainPagerActivity", "Export file error", e)
            Toast.makeText(this, getString(R.string.export_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun importDataFromFile(uri: Uri) {
        try {
            val jsonData = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (jsonData != null) {
                val success = dataManager.importAllData(jsonData)
                if (success) {
                    Toast.makeText(this, getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                    refreshFragments()
                } else {
                    Toast.makeText(this, getString(R.string.import_error), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainPagerActivity", "Import file error", e)
            Toast.makeText(this, getString(R.string.import_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshFragments() {
        android.util.Log.d("MainPagerActivity", "Refreshing fragments after import")
        
        // Обновляем все фрагменты (0: Overdue, 1: Main, 2: Future)
        val overdueFragment = pagerAdapter.getFragment(0) as? OverdueNotesFragment
        val mainFragment = pagerAdapter.getFragment(1) as? MainFragment
        val futureFragment = pagerAdapter.getFragment(2) as? FutureNotesFragment
        overdueFragment?.let { fragment ->
            android.util.Log.d("MainPagerActivity", "Refreshing OverdueNotesFragment")
            fragment.refreshOverdueNotes()
        }
        
        mainFragment?.let { fragment ->
            android.util.Log.d("MainPagerActivity", "Refreshing MainFragment")
            // Принудительно обновляем данные
            fragment.refreshData()
        }
        
        futureFragment?.let { fragment ->
            android.util.Log.d("MainPagerActivity", "Refreshing FutureNotesFragment")
            // Принудительно обновляем данные
            fragment.refreshData()
        }
        
        // Также обновляем адаптер ViewPager
        pagerAdapter.notifyDataSetChanged()
        android.util.Log.d("MainPagerActivity", "Fragments refresh completed")
    }
} 