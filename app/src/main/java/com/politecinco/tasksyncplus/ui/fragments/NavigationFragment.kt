package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.politecinco.tasksyncplus.R

/**
 * Fragmento del sidebar (solo categorías). Reenvía clicks a la Activity para filtrar.
 */
// Sidebar de categorías; reenvía clics para filtrar lista.
// Hecho por: Daniel Castrillon
class NavigationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_navigation, container, false)

        val btnAll = root.findViewById<LinearLayout>(R.id.btn_all_tasks)
        val btnPending = root.findViewById<LinearLayout>(R.id.btn_pending_tasks)
        val btnCompleted = root.findViewById<LinearLayout>(R.id.btn_completed_tasks)

        val taskListFragment = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.content_fragment_container) as? TaskListFragment

        btnAll.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.ALL)
        }
        btnPending.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.PENDING)
        }
        btnCompleted.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.COMPLETED)
        }

        return root
    }
}