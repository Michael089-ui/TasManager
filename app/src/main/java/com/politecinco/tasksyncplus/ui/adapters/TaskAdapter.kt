package com.politecinco.tasksyncplus.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.politecinco.tasksyncplus.R
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.databinding.ItemTaskBinding

class TaskAdapter(
    private val onTaskClicked: (Task) -> Unit,
    private val onTaskCompletionToggled: (Task) -> Unit = {}
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task, onTaskCompletionToggled)
        holder.itemView.setOnClickListener { onTaskClicked(task) }
    }

    class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task, onTaskCompletionToggled: (Task) -> Unit) {
            binding.taskTitle.text = task.title
            binding.taskDescription.text = task.description

            // Configurar el círculo de completado
            binding.completionCircle.isSelected = task.isCompleted
            
            // Agregar click listener al círculo
            binding.completionCircle.setOnClickListener {
                onTaskCompletionToggled(task)
            }

            // Estilo visual según el estado
            if (task.isCompleted) {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskDescription.paintFlags = binding.taskDescription.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.7f
            } else {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskDescription.paintFlags = binding.taskDescription.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }
}