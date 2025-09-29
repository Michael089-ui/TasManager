package com.politecinco.tasksyncplus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.ui.fragments.TaskListFragment.TaskFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Daniel Castrillon: Refactorización de TaskViewModel.kt para eliminar dependencias de Hilt
// y mejorar compatibilidad con los cambios en TaskListFragment.kt
// Mejoramiento original por: Juan Pacheco

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    val allTasks = repository.getAllTasks()
    val pendingTasks = repository.getPendingTasks()
    val completedTasks = repository.getCompletedTasks()

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
    }

    // Daniel Castrillon: Método modificado para recibir objeto Task completo en lugar de solo ID
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    // Daniel Castrillon: Método modificado para recibir objeto Task completo en lugar de solo ID
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Daniel Castrillon: Factory manual para ViewModelProvider reemplazando la inyección de dependencias con Hilt
    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repository) as T
        }
    }
}