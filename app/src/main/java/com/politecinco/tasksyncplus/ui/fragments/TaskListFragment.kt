package com.politecinco.tasksyncplus.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.politecinco.tasksyncplus.MainActivity
import com.politecinco.tasksyncplus.R
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.databinding.DialogQuickTaskBinding
import com.politecinco.tasksyncplus.databinding.DialogTaskFormBinding
import com.politecinco.tasksyncplus.databinding.FragmentTaskListBinding
import com.politecinco.tasksyncplus.ui.adapters.QuickTaskAdapter
import com.politecinco.tasksyncplus.ui.adapters.TaskAdapter
import com.politecinco.tasksyncplus.ui.model.QuickTaskUiModel
import com.politecinco.tasksyncplus.ui.model.TaskFilter
import com.politecinco.tasksyncplus.ui.viewmodel.SharedTaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Lista de tareas con filtros (ALL/PENDING/COMPLETED) usando un ViewModel compartido.
// Hecho por: Daniel Castrillon
class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedViewModel: SharedTaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var quickTaskAdapter: QuickTaskAdapter

    private var currentSource: LiveData<List<Task>>? = null
    private var currentFilter: TaskFilter = TaskFilter.ALL

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Daniel Castrillon: Lista mutable para almacenar las tareas rápidas en memoria
    private val quickTasks = mutableListOf<QuickTaskUiModel>()

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
        setupSharedViewModel()
        setupQuickTasksSection()
        setupRecyclerView()
        setupFab()
        observeFilterChanges()
    }

    private fun setupSharedViewModel() {
        sharedViewModel = (requireActivity() as MainActivity).sharedViewModel
        currentFilter = sharedViewModel.currentFilter.value ?: TaskFilter.ALL
        
        // Aplicar el filtro inicial inmediatamente
        val initialSource = when (currentFilter) {
            TaskFilter.ALL -> sharedViewModel.allTasks
            TaskFilter.PENDING -> sharedViewModel.pendingTasks
            TaskFilter.COMPLETED -> sharedViewModel.completedTasks
        }
        observeTasks(initialSource)
    }

    private fun observeFilterChanges() {
        sharedViewModel.currentFilter.observe(viewLifecycleOwner) { filter ->
            if (filter == currentFilter && currentSource != null) return@observe
            currentFilter = filter
            val source = when (filter) {
                TaskFilter.ALL -> sharedViewModel.allTasks
                TaskFilter.PENDING -> sharedViewModel.pendingTasks
                TaskFilter.COMPLETED -> sharedViewModel.completedTasks
            }
            observeTasks(source)
        }
    }

    // Daniel Castrillon: Método helper para cambiar dinámicamente la fuente de LiveData y evitar observadores duplicados
    private fun observeTasks(source: LiveData<List<Task>>) {
        currentSource?.removeObservers(viewLifecycleOwner)
        currentSource = source
        currentSource?.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
    }

    // Daniel Castrillon: Configuración completa de la sección de tareas rápidas
    private fun setupQuickTasksSection() {
        quickTaskAdapter = QuickTaskAdapter(
            onToggleCompletion = { task ->
                toggleQuickTaskCompletion(task)
            },
            onTaskClicked = { task ->
                convertQuickTaskToNormalTask(task)
            }
        )

        binding.quickTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quickTaskAdapter
            itemAnimator = null // Daniel Castrillon: Evitar parpadeos durante el toggle
        }

        binding.btnAddQuickTask.setOnClickListener {
            showQuickTaskDialog()
        }

        updateQuickTasksVisibility()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClicked = { task ->
                onTaskClicked(task)
            },
            onTaskCompletionToggled = { task ->
                toggleTaskCompletion(task)
            }
        )

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showTaskFormDialog()
        }

        binding.tasksRecyclerView.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
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

    private fun showTaskFormDialog(task: Task? = null) {
        val dialogBinding = DialogTaskFormBinding.inflate(layoutInflater)
        val isEditing = task != null

        dialogBinding.inputTaskTitle.setText(task?.title.orEmpty())
        dialogBinding.inputTaskDescription.setText(task?.description.orEmpty())

        var selectedDueDateMillis = parseDueDate(task?.dueDate) ?: System.currentTimeMillis()
        dialogBinding.inputTaskDueDate.setText(dateFormatter.format(Date(selectedDueDateMillis)))

        val openDatePicker: () -> Unit = {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date_picker_title)
                .setSelection(selectedDueDateMillis)
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                if (selection != null) {
                    selectedDueDateMillis = selection
                    dialogBinding.inputTaskDueDate.setText(dateFormatter.format(Date(selection)))
                    dialogBinding.containerTaskDueDate.error = null
                }
            }

            datePicker.show(childFragmentManager, "TaskDueDatePicker")
        }

        dialogBinding.inputTaskDueDate.apply {
            setOnClickListener { openDatePicker() }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    openDatePicker()
                }
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (isEditing) R.string.dialog_title_edit_task else R.string.dialog_title_create_task)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.dialog_negative_cancel, null)
            .setPositiveButton(R.string.dialog_positive_save, null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = dialogBinding.inputTaskTitle.text?.toString()?.trim().orEmpty()
                val description = dialogBinding.inputTaskDescription.text?.toString()?.trim().orEmpty()
                val dueDateText = dialogBinding.inputTaskDueDate.text?.toString()?.trim().orEmpty()

                var hasError = false

                if (title.isEmpty()) {
                    dialogBinding.containerTaskTitle.error = getString(R.string.task_form_error_title_required)
                    hasError = true
                } else {
                    dialogBinding.containerTaskTitle.error = null
                }

                if (dueDateText.isEmpty()) {
                    dialogBinding.containerTaskDueDate.error = getString(R.string.task_form_error_due_date_required)
                    hasError = true
                } else {
                    dialogBinding.containerTaskDueDate.error = null
                }

                if (hasError) {
                    return@setOnClickListener
                }

                val normalizedDescription = description.takeIf { it.isNotEmpty() }
                val now = System.currentTimeMillis()

                val taskToPersist = task?.copy(
                    title = title,
                    description = normalizedDescription,
                    dueDate = dueDateText,
                    updatedAt = now
                ) ?: Task(
                    title = title,
                    description = normalizedDescription,
                    dueDate = dueDateText
                )

                if (isEditing) {
                    sharedViewModel.updateTask(taskToPersist)
                    Snackbar.make(binding.root, R.string.task_update_success, Snackbar.LENGTH_SHORT).show()
                } else {
                    sharedViewModel.createTask(taskToPersist)
                    Snackbar.make(binding.root, R.string.task_creation_success, Snackbar.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun parseDueDate(dueDate: String?): Long? {
        if (dueDate.isNullOrBlank()) return null
        return runCatching { dateFormatter.parse(dueDate)?.time }.getOrNull()
    }

    fun setFilter(filter: TaskFilter) {
        sharedViewModel.setFilter(filter)
    }

    private fun onTaskClicked(task: Task) {
        navigateToTaskDetail(task.id)
    }

    // Daniel Castrillon: Método para alternar el estado de completado de una tarea rápida
    private fun toggleQuickTaskCompletion(task: QuickTaskUiModel) {
        val index = quickTasks.indexOfFirst { it.id == task.id }
        if (index == -1) return

        val updatedTask = task.copy(isCompleted = !task.isCompleted)
        quickTasks[index] = updatedTask
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
            id = System.currentTimeMillis() + Random.nextInt(0, 10_000),
            title = title,
            description = description?.takeIf { it.isNotBlank() },
            isCompleted = false
        )
        quickTasks.add(0, newTask)
        quickTaskAdapter.submitList(quickTasks.toList())
        updateQuickTasksVisibility()
    }

    // Daniel Castrillon: Método para controlar la visibilidad de elementos según el estado de las tareas rápidas
    private fun updateQuickTasksVisibility() {
        if (quickTasks.isEmpty()) {
            binding.quickTasksEmptyState.visibility = View.VISIBLE
            binding.quickTasksRecyclerView.visibility = View.GONE
        } else {
            binding.quickTasksEmptyState.visibility = View.GONE
            binding.quickTasksRecyclerView.visibility = View.VISIBLE
        }
    }


    private fun navigateToTaskDetail(taskId: Long) {
        try {
            Log.d("TaskListFragment", "Intentando navegar al detalle de tarea: $taskId")
            
            // Verificar que el fragment esté en un estado válido
            if (!isAdded || _binding == null) {
                Log.e("TaskListFragment", "Fragment no está en estado válido para navegación")
                return
            }
            
            // Usar FragmentManager directamente ya que no tenemos NavController configurado
            val taskDetailFragment = TaskDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong("taskId", taskId)
                }
            }
            
            Log.d("TaskListFragment", "Navegando usando FragmentManager")
            
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_fragment_container, taskDetailFragment)
                .addToBackStack("task_detail")
                .commit()
            
            Log.d("TaskListFragment", "Navegación completada")
            
        } catch (e: Exception) {
            Log.e("TaskListFragment", "Error durante la navegación", e)
            showSafeSnackbar("Error al abrir el detalle de la tarea")
        }
    }

    // Daniel Castrillon: Meedtodo para convertir una tarea rápida en tarea normal y navegar al detalle
    private fun convertQuickTaskToNormalTask(quickTask: QuickTaskUiModel) {
        lifecycleScope.launch {
            try {
                Log.d("TaskListFragment", "Iniciando conversión de tarea rápida: ${quickTask.title}")
                
                // Verificar que el binding esté disponible
                if (_binding == null) {
                    Log.e("TaskListFragment", "Binding no disponible durante la conversión")
                    return@launch
                }
                
                // Crear una tarea normal basada en la tarea rápida
                val normalTask = Task(
                    title = quickTask.title,
                    description = quickTask.description,
                    dueDate = dateFormatter.format(Date()), // Fecha actual como fecha de vencimiento
                    isCompleted = quickTask.isCompleted
                )
                
                Log.d("TaskListFragment", "Tarea normal creada: ${normalTask.title}")
                
                // Crear la tarea en la base de datos y obtener el ID
                val newTaskId = sharedViewModel.createTaskAndGetId(normalTask)
                
                Log.d("TaskListFragment", "ID de nueva tarea: $newTaskId")
                
                // Verificar que el ID sea válido
                if (newTaskId <= 0) {
                    Log.e("TaskListFragment", "ID de tarea inválido: $newTaskId")
                    showSafeSnackbar("Error: ID de tarea inválido")
                    return@launch
                }
                
                // Remover la tarea rápida de la lista
                val index = quickTasks.indexOfFirst { it.id == quickTask.id }
                if (index != -1) {
                    quickTasks.removeAt(index)
                    quickTaskAdapter.submitList(quickTasks.toList())
                    updateQuickTasksVisibility()
                    Log.d("TaskListFragment", "Tarea rápida removida de la lista")
                }
                
                // Mostrar mensaje de confirmación
                showSafeSnackbar("Tarea convertida. ID: $newTaskId")
                
                // Navegar al detalle de la nueva tarea
                Log.d("TaskListFragment", "Navegando al detalle de la tarea: $newTaskId")
                navigateToTaskDetail(newTaskId)
                
            } catch (e: Exception) {
                Log.e("TaskListFragment", "Error al convertir la tarea rápida", e)
                val errorMessage = "Error al convertir la tarea rápida: ${e.message}"
                showSafeSnackbar(errorMessage)
            }
        }
    }
    
    // Método auxiliar para mostrar Snackbar de forma segura
    private fun showSafeSnackbar(message: String) {
        try {
            if (_binding != null && isAdded && view != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            } else {
                Log.w("TaskListFragment", "No se puede mostrar Snackbar: $message")
            }
        } catch (e: Exception) {
            Log.e("TaskListFragment", "Error mostrando Snackbar: ${e.message}", e)
        }
    }
    
    // Método para toggle del estado de completado de una tarea
    private fun toggleTaskCompletion(task: Task) {
        try {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            sharedViewModel.updateTask(updatedTask)
            
            val message = if (updatedTask.isCompleted) {
                "Tarea completada: ${task.title}"
            } else {
                "Tarea marcada como pendiente: ${task.title}"
            }
            showSafeSnackbar(message)
            
        } catch (e: Exception) {
            Log.e("TaskListFragment", "Error al actualizar estado de tarea", e)
            showSafeSnackbar("Error al actualizar la tarea: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.tasksRecyclerView.clearOnScrollListeners()
        _binding = null
    }
}