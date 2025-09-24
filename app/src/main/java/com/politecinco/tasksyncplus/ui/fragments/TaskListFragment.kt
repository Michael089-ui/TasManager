package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.politecinco.tasksyncplus.databinding.FragmentTaskListBinding
import com.politecinco.tasksyncplus.data.database.TaskDatabase
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import com.politecinco.tasksyncplus.ui.adapters.TaskAdapter
import com.politecinco.tasksyncplus.ui.viewmodel.TaskViewModel
import com.politecinco.tasksyncplus.ui.viewmodel.TaskViewModelFactory
import com.politecinco.tasksyncplus.data.model.Task

// Lista de tareas con filtros (ALL/PENDING/COMPLETED) usando MediatorLiveData.
// Hecho por: Daniel Castrillon
class TaskListFragment : Fragment() {

    enum class TaskFilter { ALL, PENDING, COMPLETED }

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    // LiveData mediador para cambiar la fuente según el filtro
    private val currentTasks = MediatorLiveData<List<Task>>()
    private var currentSource: LiveData<List<Task>>? = null
    private var currentFilter: TaskFilter = TaskFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupRecyclerView()
        setupFab()
        // Observa la lista actual (según filtro)
        currentTasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
        // Filtro por defecto: todas
        setFilter(TaskFilter.ALL)
    }

    private fun setupViewModel() {
        val database = TaskDatabase.getDatabase(requireContext())
        val repository = TaskRepository(database.taskDao())
        val factory = TaskViewModelFactory(repository)
        taskViewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { task ->
            onTaskClicked(task)
        }
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            // TODO: navigate to create task screen
        }
    }

    // Cambia la fuente observada según el filtro
    fun setFilter(filter: TaskFilter) {
        if (!this::taskViewModel.isInitialized) return
        if (filter == currentFilter && currentSource != null) return
        currentFilter = filter

        val newSource: LiveData<List<Task>> = when (filter) {
            TaskFilter.ALL -> taskViewModel.allTasks
            TaskFilter.PENDING -> taskViewModel.pendingTasks
            TaskFilter.COMPLETED -> taskViewModel.completedTasks
        }
        switchSource(newSource)
    }

    private fun switchSource(newSource: LiveData<List<Task>>) {
        currentSource?.let { src -> currentTasks.removeSource(src) }
        currentSource = newSource
        currentTasks.addSource(newSource) { list ->
            currentTasks.value = list
        }
    }

    private fun onTaskClicked(task: Task) {
        // TODO: navigate to detail screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentSource?.let { src -> currentTasks.removeSource(src) }
        currentSource = null
    }
}