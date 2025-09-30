package com.politecinco.tasksyncplus.ui.model

/**
 * Daniel Castrillon: Modelo ligero para tareas temporales creadas desde el panel rápido.
 * No se persiste en base de datos; vive solo en memoria por sesión.
 * Diseñado para ser eficiente y simple para tareas de uso inmediato.
 */
data class QuickTaskUiModel(
    // Daniel Castrillon: ID único para identificar la tarea en el adaptador
    val id: Long,
    // Daniel Castrillon: Título obligatorio de la tarea rápida
    val title: String,
    // Daniel Castrillon: Descripción opcional para detalles adicionales
    val description: String?,
    // Daniel Castrillon: Estado de completado para toggle visual
    val isCompleted: Boolean
)