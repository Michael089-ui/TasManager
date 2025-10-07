package com.politecinco.tasksyncplus.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import com.politecinco.tasksyncplus.ui.model.TaskFilter
import kotlinx.coroutines.launch

/**
 * ViewModel compartido entre los fragmentos de navegación y lista de tareas.
 * Administra la fuente de datos y operaciones CRUD sobre las tareas almacenadas en Room.
 */
class SharedTaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _currentFilter = MutableLiveData(TaskFilter.ALL)
    val currentFilter: LiveData<TaskFilter> = _currentFilter

    val allTasks: LiveData<List<Task>> = repository.getAllTasks()
    val pendingTasks: LiveData<List<Task>> = repository.getPendingTasks()
    val completedTasks: LiveData<List<Task>> = repository.getCompletedTasks()

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    fun createTask(task: Task) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    suspend fun createTaskAndGetId(task: Task): Long {
        return repository.insertTask(task)
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    suspend fun getTaskById(id: Long): Task? {
        return repository.getTaskById(id)
    }

    class Factory(
        private val repository: TaskRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SharedTaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SharedTaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${'$'}{modelClass.name}")
        }
    }
}