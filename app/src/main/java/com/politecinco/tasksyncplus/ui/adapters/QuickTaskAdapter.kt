package com.politecinco.tasksyncplus.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.politecinco.tasksyncplus.R
import com.politecinco.tasksyncplus.databinding.ItemQuickTaskBinding
import com.politecinco.tasksyncplus.ui.model.QuickTaskUiModel

/**
 * Daniel Castrillon: Adaptador personalizado para la lista de tareas rápidas (temporales).
 * Implementa ListAdapter con DiffUtil para optimizar las actualizaciones de la UI.
 * Creado por: Daniel Castrillon
 */
class QuickTaskAdapter(
    // Daniel Castrillon: Callback para manejar el toggle de completado de tareas
    private val onToggleCompletion: (QuickTaskUiModel) -> Unit
) : ListAdapter<QuickTaskUiModel, QuickTaskAdapter.QuickTaskViewHolder>(QuickTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickTaskViewHolder {
        // Daniel Castrillon: Inflar el layout personalizado para items de tareas rápidas
        val binding = ItemQuickTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuickTaskViewHolder(binding, onToggleCompletion)
    }

    override fun onBindViewHolder(holder: QuickTaskViewHolder, position: Int) {
        // Daniel Castrillon: Vincular datos del modelo con la vista
        holder.bind(getItem(position))
    }

    class QuickTaskViewHolder(
        private val binding: ItemQuickTaskBinding,
        private val onToggleCompletion: (QuickTaskUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // Daniel Castrillon: Método para vincular datos de la tarea rápida con la vista
        fun bind(task: QuickTaskUiModel) {
            // Daniel Castrillon: Establecer el título de la tarea
            binding.quickTaskTitle.text = task.title

            // Daniel Castrillon: Mostrar u ocultar descripción según si existe contenido
            if (task.description.isNullOrBlank()) {
                binding.quickTaskDescription.visibility = android.view.View.GONE
            } else {
                binding.quickTaskDescription.visibility = android.view.View.VISIBLE
                binding.quickTaskDescription.text = task.description
            }

            // Daniel Castrillon: Cambiar icono según el estado de completado de la tarea
            val iconRes = if (task.isCompleted) {
                R.drawable.ic_quick_task_checked
            } else {
                R.drawable.ic_quick_task_unchecked
            }
            binding.quickTaskStatusIndicator.setImageResource(iconRes)

            // Daniel Castrillon: Configurar click listener para toggle de completado
            binding.quickTaskStatusIndicator.setOnClickListener {
                onToggleCompletion(task)
            }
        }
    }

    // Daniel Castrillon: Callback para optimizar actualizaciones del RecyclerView con DiffUtil
    private class QuickTaskDiffCallback : DiffUtil.ItemCallback<QuickTaskUiModel>() {
        // Daniel Castrillon: Comparar si son el mismo item basado en ID único
        override fun areItemsTheSame(oldItem: QuickTaskUiModel, newItem: QuickTaskUiModel): Boolean =
            oldItem.id == newItem.id

        // Daniel Castrillon: Comparar si el contenido ha cambiado para determinar si actualizar
        override fun areContentsTheSame(oldItem: QuickTaskUiModel, newItem: QuickTaskUiModel): Boolean =
            oldItem == newItem
    }
}