package com.politecinco.tasksyncplus.data.repository

import androidx.lifecycle.LiveData
import com.politecinco.tasksyncplus.data.database.TaskDao
import com.politecinco.tasksyncplus.data.model.Task

class TaskRepository(private val taskDao: TaskDao) {

    fun getAllTasks(): LiveData<List<Task>> = taskDao.getAllTasks()
    fun getPendingTasks(): LiveData<List<Task>> = taskDao.getPendingTasks()
    fun getCompletedTasks(): LiveData<List<Task>> = taskDao.getCompletedTasks()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun toggleTaskCompletion(task: Task) {
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updatedTask)
    }
}