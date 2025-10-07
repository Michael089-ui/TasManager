package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.politecinco.tasksyncplus.MainActivity
import com.politecinco.tasksyncplus.databinding.FragmentNavigationBinding
import com.politecinco.tasksyncplus.ui.model.TaskFilter

/**
 * Fragmento del sidebar (solo categorías). Reenvía clicks al ViewModel compartido para filtrar.
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
        val sharedViewModel = (requireActivity() as MainActivity).sharedViewModel

        binding.btnAllTasks.setOnClickListener {
            sharedViewModel.setFilter(TaskFilter.ALL)
        }
        binding.btnPendingTasks.setOnClickListener {
            sharedViewModel.setFilter(TaskFilter.PENDING)
        }
        binding.btnCompletedTasks.setOnClickListener {
            sharedViewModel.setFilter(TaskFilter.COMPLETED)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}