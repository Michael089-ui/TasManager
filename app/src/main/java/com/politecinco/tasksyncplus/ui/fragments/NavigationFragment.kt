package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.politecinco.tasksyncplus.R
import com.politecinco.tasksyncplus.databinding.FragmentNavigationBinding

/**
 * Fragmento del sidebar (solo categorías). Reenvía clicks a la Activity para filtrar.
 */
// Sidebar de categorías; reenvía clics para filtrar lista.
// Hecho por: Daniel Castrillon
class NavigationFragment : Fragment() {

    private var _binding: FragmentNavigationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavigationBinding.inflate(inflater, container, false)

        val taskListFragment = requireActivity()
            .supportFragmentManager
            .findFragmentById(R.id.content_fragment_container) as? TaskListFragment

        binding.btnAllTasks.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.ALL)
        }
        binding.btnPendingTasks.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.PENDING)
        }
        binding.btnCompletedTasks.setOnClickListener {
            taskListFragment?.setFilter(TaskListFragment.TaskFilter.COMPLETED)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}