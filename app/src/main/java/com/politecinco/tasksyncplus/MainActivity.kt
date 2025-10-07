package com.politecinco.tasksyncplus

import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.politecinco.tasksyncplus.data.database.TaskDatabase
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import com.politecinco.tasksyncplus.ui.fragments.TaskListFragment
import com.politecinco.tasksyncplus.ui.model.TaskFilter
import com.politecinco.tasksyncplus.ui.viewmodel.SharedTaskViewModel

// Menú de filtros horizontal fijo.
// Hecho por: Daniel Castrillon
class MainActivity : AppCompatActivity() {

    private val repository: TaskRepository by lazy {
        val database = TaskDatabase.getDatabase(applicationContext)
        TaskRepository(database.taskDao())
    }

    val sharedViewModel: SharedTaskViewModel by viewModels {
        SharedTaskViewModel.Factory(repository)
    }
    
    // Botones del menú de filtros
    private lateinit var btnAllTasks: LinearLayout
    private lateinit var btnPendingTasks: LinearLayout
    private lateinit var btnCompletedTasks: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupToolbar()
        setupFragments(savedInstanceState)
        setupFilterButtons()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            // Cargar el fragmento de lista de tareas
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_fragment_container, TaskListFragment())
                .commitNow()
        }
    }

    private fun setupFilterButtons() {
        btnAllTasks = findViewById(R.id.btn_all_tasks)
        btnPendingTasks = findViewById(R.id.btn_pending_tasks)
        btnCompletedTasks = findViewById(R.id.btn_completed_tasks)
        
        btnAllTasks.setOnClickListener {
            selectFilterButton(TaskFilter.ALL)
            sharedViewModel.setFilter(TaskFilter.ALL)
        }
        
        btnPendingTasks.setOnClickListener {
            selectFilterButton(TaskFilter.PENDING)
            sharedViewModel.setFilter(TaskFilter.PENDING)
        }
        
        btnCompletedTasks.setOnClickListener {
            selectFilterButton(TaskFilter.COMPLETED)
            sharedViewModel.setFilter(TaskFilter.COMPLETED)
        }
        
        // Seleccionar "Todas las tareas" por defecto
        selectFilterButton(TaskFilter.ALL)
    }
    
    private fun selectFilterButton(filter: TaskFilter) {
        // Resetear todos los fondos
        btnAllTasks.setBackgroundResource(R.drawable.selector_menu_item)
        btnPendingTasks.setBackgroundResource(R.drawable.selector_menu_item)
        btnCompletedTasks.setBackgroundResource(R.drawable.selector_menu_item)
        
        // Aplicar fondo seleccionado al botón correspondiente
        when (filter) {
            TaskFilter.ALL -> btnAllTasks.setBackgroundResource(R.drawable.selected_menu_item)
            TaskFilter.PENDING -> btnPendingTasks.setBackgroundResource(R.drawable.selected_menu_item)
            TaskFilter.COMPLETED -> btnCompletedTasks.setBackgroundResource(R.drawable.selected_menu_item)
        }
    }
}