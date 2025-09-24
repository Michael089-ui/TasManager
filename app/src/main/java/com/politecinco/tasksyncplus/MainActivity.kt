package com.politecinco.tasksyncplus

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.politecinco.tasksyncplus.ui.fragments.NavigationFragment
import com.politecinco.tasksyncplus.ui.fragments.TaskListFragment

// Ajustes de UI (navbar a ancho completo, sidebar más ancho, filtro por pestañas).
// Hecho por: Daniel Castrillon
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // Cargamos los dos fragmentos inmediatamente
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_fragment_container, NavigationFragment())
                .replace(R.id.content_fragment_container, TaskListFragment())
                .commitNow()
        }

        // Referencias a las pestañas del navbar superior (que ahora ocupa todo el ancho)
        val tabMenu = findViewById<TextView>(R.id.tab_menu)
        val tabPending = findViewById<TextView>(R.id.tab_pending)
        val tabCompleted = findViewById<TextView>(R.id.tab_completed)

        // Obtenemos el fragmento de lista para aplicar el filtro
        val taskListFragment = supportFragmentManager.findFragmentById(R.id.content_fragment_container) as? TaskListFragment

        tabMenu.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.ALL)
        }
        tabPending.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.PENDING)
        }
        tabCompleted.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.COMPLETED)
        }
    }
}