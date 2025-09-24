package com.politecinco.tasksyncplus.ui.viewmodel

import androidx.lifecycle.*
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.data.repository.TaskRepository
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: LiveData<List<Task>> = repository.getAllTasks()
    val pendingTasks: LiveData<List<Task>> = repository.getPendingTasks()
    val completedTasks: LiveData<List<Task>> = repository.getCompletedTasks()

    fun insertTask(task: Task) = viewModelScope.launch { repository.insertTask(task) }
    fun updateTask(task: Task) = viewModelScope.launch { repository.updateTask(task) }
    fun deleteTask(task: Task) = viewModelScope.launch { repository.deleteTask(task) }
    fun toggleTaskCompletion(task: Task) = viewModelScope.launch { repository.toggleTaskCompletion(task) }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}