package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.politecinco.tasksyncplus.MainActivity
import com.politecinco.tasksyncplus.R
import com.politecinco.tasksyncplus.ui.model.TaskFilter
import com.politecinco.tasksyncplus.ui.viewmodel.SharedTaskViewModel

class NavigationSidebarFragment : Fragment() {

    private lateinit var sharedTaskViewModel: SharedTaskViewModel
    
    // Referencias a las vistas
    private lateinit var navAllTasks: LinearLayout
    private lateinit var navPendingTasks: LinearLayout
    private lateinit var navCompletedTasks: LinearLayout
    
    private lateinit var badgeAllTasks: TextView
    private lateinit var badgePendingTasks: TextView
    private lateinit var badgeCompletedTasks: TextView

    // Interface para comunicación con MainActivity
    interface NavigationListener {
        fun onNavigationItemSelected(filter: TaskFilter)
        fun onSettingsSelected()
        fun onAboutSelected()
        fun closeSidebar()
        fun navigateToTaskList()
    }

    private var navigationListener: NavigationListener? = null

    fun setNavigationListener(listener: NavigationListener) {
        this.navigationListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_navigation_sidebar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener el ViewModel compartido de la MainActivity
        val mainActivity = requireActivity() as MainActivity
        sharedTaskViewModel = mainActivity.sharedViewModel
        
        // Inicializar vistas
        initViews(view)
        
        // Configurar listeners
        setupClickListeners()
        
        // Observar cambios en las tareas para actualizar badges
        observeTaskCounts()
    }

    private fun initViews(view: View) {
        navAllTasks = view.findViewById(R.id.nav_all_tasks)
        navPendingTasks = view.findViewById(R.id.nav_pending_tasks)
        navCompletedTasks = view.findViewById(R.id.nav_completed_tasks)
        
        badgeAllTasks = view.findViewById(R.id.badge_all_tasks)
        badgePendingTasks = view.findViewById(R.id.badge_pending_tasks)
        badgeCompletedTasks = view.findViewById(R.id.badge_completed_tasks)
    }

    private fun setupClickListeners() {
        navAllTasks.setOnClickListener {
            navigationListener?.navigateToTaskList()
            navigationListener?.onNavigationItemSelected(TaskFilter.ALL)
            navigationListener?.closeSidebar()
        }

        navPendingTasks.setOnClickListener {
            navigationListener?.navigateToTaskList()
            navigationListener?.onNavigationItemSelected(TaskFilter.PENDING)
            navigationListener?.closeSidebar()
        }

        navCompletedTasks.setOnClickListener {
            navigationListener?.navigateToTaskList()
            navigationListener?.onNavigationItemSelected(TaskFilter.COMPLETED)
            navigationListener?.closeSidebar()
        }
    }

    private fun observeTaskCounts() {
        sharedTaskViewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            val allCount = tasks.size
            val pendingCount = tasks.count { !it.isCompleted }
            val completedCount = tasks.count { it.isCompleted }
            
            updateBadges(allCount, pendingCount, completedCount)
        }
    }

    private fun updateBadges(allCount: Int, pendingCount: Int, completedCount: Int) {
        badgeAllTasks.text = "Ver todas las tareas"
        badgePendingTasks.text = "Ver pendientes"
        badgeCompletedTasks.text = "Ver completadas"
    }

    override fun onDetach() {
        super.onDetach()
        navigationListener = null
    }
}