package com.politecinco.tasksyncplus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import com.politecinco.tasksyncplus.ui.fragments.TaskListFragment.TaskFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//Mejoramietno de TaskViewModel.kt ya que se realizaron cambios en el TasklistFragment.kt
//Hecho por: Juan Pacheco

@HiltViewModel /
class TaskViewModel @Inject constructor(
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

    fun toggleTaskCompletion(taskId: Long) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    companion object {
        class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(repository) as T
            }
        }
    }
}