package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
// Daniel Castrillon: Importaciones agregadas para la funcionalidad de Quick Tasks
import com.politecinco.tasksyncplus.databinding.DialogQuickTaskBinding
import com.politecinco.tasksyncplus.databinding.FragmentTaskListBinding
import com.politecinco.tasksyncplus.data.database.TaskDatabase
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import com.politecinco.tasksyncplus.ui.adapters.QuickTaskAdapter
import com.politecinco.tasksyncplus.ui.adapters.TaskAdapter
import com.politecinco.tasksyncplus.ui.viewmodel.TaskViewModel
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.ui.model.QuickTaskUiModel
import kotlin.random.Random

// Lista de tareas con filtros (ALL/PENDING/COMPLETED) usando MediatorLiveData.
// Hecho por: Daniel Castrillon
class TaskListFragment : Fragment() {

    enum class TaskFilter { ALL, PENDING, COMPLETED }

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    // Daniel Castrillon: Adaptador agregado para manejar las tareas rápidas
    private lateinit var quickTaskAdapter: QuickTaskAdapter

    // Daniel Castrillon: Refactorización para gestionar manualmente la fuente LiveData sin usar Hilt o MediatorLiveData
    private var currentSource: LiveData<List<Task>>? = null
    private var currentFilter: TaskFilter = TaskFilter.ALL

    // Daniel Castrillon: Lista mutable para almacenar las tareas rápidas en memoria
    private val quickTasks = mutableListOf<QuickTaskUiModel>()

    // Daniel Castrillon: Método helper para cambiar dinámicamente la fuente de LiveData y evitar observadores duplicados
    private fun observeTasks(source: LiveData<List<Task>>) {
        // Daniel Castrillon: Removemos observadores anteriores para evitar memory leaks
        currentSource?.removeObservers(viewLifecycleOwner)
        currentSource = source
        currentSource?.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
    }

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
        // Daniel Castrillon: Configuración de la nueva sección de tareas rápidas
        setupQuickTasksSection()
        setupRecyclerView()
        setupFab()
        // Filtro por defecto: todas
        setFilter(TaskFilter.ALL)
    }

    private fun setupViewModel() {
        val database = TaskDatabase.getDatabase(requireContext())
        val repository = TaskRepository(database.taskDao())
        // Daniel Castrillon: Uso de Factory manual en lugar de inyección de dependencias con Hilt
        val factory = TaskViewModel.Factory(repository)
        taskViewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]
    }

    // Daniel Castrillon: Configuración completa de la sección de tareas rápidas
    private fun setupQuickTasksSection() {
        // Daniel Castrillon: Inicialización del adaptador con callback para toggle de completado
        quickTaskAdapter = QuickTaskAdapter { task ->
            toggleQuickTaskCompletion(task)
        }

        // Daniel Castrillon: Configuración del RecyclerView para tareas rápidas
        binding.quickTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quickTaskAdapter
            itemAnimator = null // Daniel Castrillon: Evitar parpadeos durante el toggle
        }

        // Daniel Castrillon: Configuración del botón para agregar nuevas tareas rápidas
        binding.btnAddQuickTask.setOnClickListener {
            showQuickTaskDialog()
        }

        // Daniel Castrillon: Actualizar visibilidad inicial de los elementos
        updateQuickTasksVisibility()
    }

    private fun setupRecyclerView() {
        // Daniel Castrillon: Simplificación del adaptador usando lambda en lugar de múltiples callbacks
        taskAdapter = TaskAdapter { task ->
            onTaskClicked(task)
        }

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

    private fun filterTasks(tasks: List<Task>, filter: TaskFilter): List<Task> {
        return when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }
    }

    // Cambia el filtro de manera más eficiente
    fun setFilter(filter: TaskFilter) {
        if (filter == currentFilter && currentSource != null) return
        currentFilter = filter
        taskViewModel.setFilter(filter)

        // Daniel Castrillon: Selección manual de la fuente LiveData según el filtro actual
        val source = when (filter) {
            TaskFilter.ALL -> taskViewModel.allTasks
            TaskFilter.PENDING -> taskViewModel.pendingTasks
            TaskFilter.COMPLETED -> taskViewModel.completedTasks
        }
        observeTasks(source)
    }

    private fun onTaskClicked(task: Task) {
        navigateToTaskDetail(task.id)
    }

    // Daniel Castrillon: Método para alternar el estado de completado de una tarea rápida
    private fun toggleQuickTaskCompletion(task: QuickTaskUiModel) {
        val index = quickTasks.indexOfFirst { it.id == task.id }
        if (index == -1) return

        // Daniel Castrillon: Crear copia inmutable con estado actualizado
        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        quickTasks[index] = updatedTask
        // Daniel Castrillon: Actualizar el adaptador con nueva lista inmutable
        quickTaskAdapter.submitList(quickTasks.toList())
    }

    // Daniel Castrillon: Método para mostrar el diálogo de creación de tareas rápidas
    private fun showQuickTaskDialog() {
        val context = requireContext()
        val dialogBinding = DialogQuickTaskBinding.inflate(layoutInflater)

        AlertDialog.Builder(context)
            .setTitle("Nueva tarea rápida")
            .setView(dialogBinding.root)
            .setPositiveButton("Crear") { dialog, _ ->
                // Daniel Castrillon: Obtener y limpiar los datos del formulario
                val title = dialogBinding.inputQuickTaskTitle.text?.toString()?.trim().orEmpty()
                val description = dialogBinding.inputQuickTaskDescription.text?.toString()?.trim()

                if (title.isNotEmpty()) {
                    addQuickTask(title, description)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Daniel Castrillon: Método para agregar una nueva tarea rápida a la lista
    private fun addQuickTask(title: String, description: String?) {
        val newTask = QuickTaskUiModel(
            // Daniel Castrillon: Generar ID único usando timestamp + random para evitar colisiones
            id = System.currentTimeMillis() + Random.nextInt(0, 10_000),
            title = title,
            // Daniel Castrillon: Solo guardar descripción si no está vacía
            description = description?.takeIf { it.isNotBlank() },
            isCompleted = false
        )
        // Daniel Castrillon: Agregar al inicio de la lista para mostrar las más recientes primero
        quickTasks.add(0, newTask)
        quickTaskAdapter.submitList(quickTasks.toList())
        updateQuickTasksVisibility()
    }

    // Daniel Castrillon: Método para controlar la visibilidad de elementos según el estado de las tareas rápidas
    private fun updateQuickTasksVisibility() {
        if (quickTasks.isEmpty()) {
            // Daniel Castrillon: Mostrar mensaje de estado vacío cuando no hay tareas rápidas
            binding.quickTasksEmptyState.visibility = View.VISIBLE
            binding.quickTasksRecyclerView.visibility = View.GONE
        } else {
            // Daniel Castrillon: Mostrar lista cuando hay tareas rápidas disponibles
            binding.quickTasksEmptyState.visibility = View.GONE
            binding.quickTasksRecyclerView.visibility = View.VISIBLE
        }
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