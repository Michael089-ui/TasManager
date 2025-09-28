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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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
//Mejoramietno de scroll-aware para FAB
//Hecho por: Juan Pacheco
    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClicked = { task ->
                onTaskClicked(task)
            },
            onTaskLongClicked = { task ->
                // Opcional: añadir funcionalidad de long click
                true
            }
        )
        
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            // Mejora: añadir decoraciones o optimizaciones si es necesario
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            navigateToCreateTask()
        }
        
        // Opcional: añadir comportamiento scroll-aware al FAB
        binding.tasksRecyclerView.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0 && binding.fabAddTask.isShown) {
                        binding.fabAddTask.hide()
                    } else if (dy < 0 && !binding.fabAddTask.isShown) {
                        binding.fabAddTask.show()
                    }
                }
            }
        )
    }

    private fun observeTaskList() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Combina el filtro actual con la lista de tareas
                combine(
                    taskViewModel.allTasks,
                    taskViewModel.currentFilter
                ) { tasks, filter ->
                    filterTasks(tasks, filter)
                }.collect { filteredTasks ->
                    taskAdapter.submitList(filteredTasks)
                    handleEmptyState(filteredTasks.isEmpty())
                }
            }
        }
    }

    private fun filterTasks(tasks: List<Task>, filter: TaskFilter): List<Task> {
        return when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }
    }

    private fun handleEmptyState(isEmpty: Boolean) {
        binding.emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.tasksRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // Cambia el filtro de manera más eficiente
    fun setFilter(filter: TaskFilter) {
        if (filter == currentFilter) return
        currentFilter = filter
        taskViewModel.setFilter(filter)
    }

    private fun onTaskClicked(task: Task) {
        navigateToTaskDetail(task.id)
    }

    private fun navigateToCreateTask() {
        // TODO: Implementar navegación a creación de tarea
        // findNavController().navigate(R.id.action_taskListFragment_to_createTaskFragment)
    }

    private fun navigateToTaskDetail(taskId: Long) {
        // TODO: Implementar navegación a detalle de tarea
        // findNavController().navigate(TaskListFragmentDirections.actionTaskListFragmentToTaskDetailFragment(taskId))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar listeners para evitar memory leaks
        binding.tasksRecyclerView.clearOnScrollListeners()
        _binding = null
    }
}