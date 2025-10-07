package com.politecinco.tasksyncplus.ui.fragments

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.politecinco.tasksyncplus.data.model.Task
import com.politecinco.tasksyncplus.databinding.FragmentTaskDetailBinding
import com.politecinco.tasksyncplus.ui.viewmodel.SharedTaskViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Fragment para crear y editar detalles de tareas.
 * Implementa todas las funcionalidades requeridas:
 * - Edición de campos básicos
 * - Adjunto de imagen
 * - Nota de voz
 * - Exportar al calendario
 * - Integración con SharedTaskViewModel
 */
class TaskDetailFragment : Fragment() {

    private var _binding: FragmentTaskDetailBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedTaskViewModel by activityViewModels()
    
    private val taskId: Long by lazy { 
        arguments?.getLong("taskId", -1L) ?: -1L 
    }
    
    private var currentTask: Task? = null
    private var selectedImageUri: Uri? = null
    private var voiceRecorder: MediaRecorder? = null
    private var voicePlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private var voiceFilePath: String? = null
    
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    companion object {
        private const val TAG = "TaskDetailFragment"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    // Launcher para seleccionar imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            handleImageSelection(it)
        }
    }

    // Launcher para permisos de audio
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            toggleVoiceRecording()
        } else {
            showError("Permiso de grabación de audio requerido")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView iniciado")
        try {
            _binding = FragmentTaskDetailBinding.inflate(inflater, container, false)
            Log.d(TAG, "Binding creado exitosamente")
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreateView: ${e.message}", e)
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated iniciado")
        
        try {
            Log.d(TAG, "TaskId recibido: $taskId")
            
            setupUI()
            setupClickListeners()
            loadTaskData()
            
            Log.d(TAG, "Configuración completada exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en onViewCreated: ${e.message}", e)
            showError("Error al inicializar la pantalla: ${e.message}")
        }
    }

    private fun setupUI() {
        Log.d(TAG, "Configurando UI")
        
        try {
            // Configurar header
            binding.textTaskDetailHeader.text = if (taskId == -1L) {
                "Nueva Tarea"
            } else {
                "Editar Tarea"
            }
            
            // Configurar fecha por defecto si es nueva tarea
            if (taskId == -1L) {
                binding.inputTaskDueDate.setText(dateFormatter.format(Date()))
            }
            
            // Configurar estado inicial de adjuntos
            updateAttachmentUI()
            updateVoiceNoteUI()
            
            Log.d(TAG, "UI configurada")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando UI: ${e.message}", e)
            throw e
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Configurando click listeners")
        
        try {
            // Botones principales
            binding.buttonSave.setOnClickListener {
                Log.d(TAG, "Botón Guardar presionado")
                handleSave()
            }
            
            binding.buttonCancel.setOnClickListener {
                Log.d(TAG, "Botón Cancelar presionado")
                handleCancel()
            }
            
            binding.buttonExportCalendar.setOnClickListener {
                Log.d(TAG, "Botón Exportar presionado")
                handleExportToCalendar()
            }
            
            // Selector de fecha
            binding.inputTaskDueDate.setOnClickListener {
                showDatePicker()
            }
            
            // Adjunto de imagen
            binding.buttonAttachmentAction.setOnClickListener {
                handleAttachmentAction()
            }
            
            // Nota de voz
            binding.buttonVoiceNote.setOnClickListener {
                handleVoiceNoteAction()
            }
            
            Log.d(TAG, "Click listeners configurados")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando listeners: ${e.message}", e)
            throw e
        }
    }

    private fun loadTaskData() {
        if (taskId != -1L) {
            lifecycleScope.launch {
                try {
                    currentTask = sharedViewModel.getTaskById(taskId)
                    currentTask?.let { task ->
                        populateFields(task)
                    } ?: run {
                        showError("Tarea no encontrada")
                        parentFragmentManager.popBackStack()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cargando tarea: ${e.message}", e)
                    showError("Error cargando tarea: ${e.message}")
                }
            }
        }
    }
    
    private fun populateFields(task: Task) {
        try {
            binding.inputTaskTitle.setText(task.title)
            binding.inputTaskDescription.setText(task.description)
            binding.inputTaskDueDate.setText(task.dueDate)
            binding.inputTaskNotes.setText("") // Campo de notas adicionales
            
            // Configurar imagen si existe
            task.imagePath?.let { imagePath ->
                selectedImageUri = Uri.parse(imagePath)
                updateAttachmentUI()
            }
            
            // Configurar nota de voz si existe
            task.voiceNotePath?.let { voicePath ->
                voiceFilePath = voicePath
                updateVoiceNoteUI()
            }
            
            Log.d(TAG, "Campos poblados con datos de la tarea")
        } catch (e: Exception) {
            Log.e(TAG, "Error poblando campos: ${e.message}", e)
            showError("Error cargando datos de la tarea")
        }
    }

    private fun handleSave() {
        try {
            val title = binding.inputTaskTitle.text?.toString()?.trim().orEmpty()
            val description = binding.inputTaskDescription.text?.toString()?.trim().orEmpty()
            val dueDate = binding.inputTaskDueDate.text?.toString()?.trim().orEmpty()

            Log.d(TAG, "Datos a guardar - Título: $title, Descripción: $description, Fecha: $dueDate")

            if (title.isEmpty()) {
                showError("El título es requerido")
                return
            }
            
            if (dueDate.isEmpty()) {
                showError("La fecha de vencimiento es requerida")
                return
            }

            lifecycleScope.launch {
                try {
                    // Log para verificar el estado de la nota de voz antes de guardar
                    Log.d(TAG, "Guardando tarea con nota de voz: $voiceFilePath")
                    if (!voiceFilePath.isNullOrEmpty()) {
                        val audioFile = File(voiceFilePath!!)
                        Log.d(TAG, "Archivo de audio existe: ${audioFile.exists()}, Tamaño: ${audioFile.length()} bytes")
                    }
                    
                    val task = if (taskId == -1L) {
                        // Nueva tarea
                        Task(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            imagePath = selectedImageUri?.toString(),
                            voiceNotePath = voiceFilePath
                        )
                    } else {
                        // Actualizar tarea existente
                        currentTask?.copy(
                            title = title,
                            description = description,
                            dueDate = dueDate,
                            imagePath = selectedImageUri?.toString(),
                            voiceNotePath = voiceFilePath,
                            updatedAt = System.currentTimeMillis()
                        ) ?: return@launch
                    }
                    
                    Log.d(TAG, "Tarea a guardar - ID: ${task.id}, VoicePath: ${task.voiceNotePath}")
                    
                    if (taskId == -1L) {
                        sharedViewModel.createTask(task)
                        showSuccess("Tarea creada correctamente")
                    } else {
                        sharedViewModel.updateTask(task)
                        showSuccess("Tarea actualizada correctamente")
                    }
                    
                    // Navegar de vuelta
                    parentFragmentManager.popBackStack()

                } catch (e: Exception) {
                    Log.e(TAG, "Error guardando tarea: ${e.message}", e)
                    showError("Error al guardar: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar: ${e.message}", e)
            showError("Error al guardar: ${e.message}")
        }
    }

    private fun handleCancel() {
        try {
            Log.d(TAG, "Cancelando y navegando hacia atrás")
            cleanupMediaResources()
            parentFragmentManager.popBackStack()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cancelar: ${e.message}", e)
            showError("Error al cancelar: ${e.message}")
        }
    }

    // ========== FUNCIONALIDADES DE FECHA ==========
    
    private fun showDatePicker() {
        try {
            val currentDate = Calendar.getInstance()
            
            // Si hay una fecha ya seleccionada, usarla como inicial
            val dateText = binding.inputTaskDueDate.text?.toString()
            if (!dateText.isNullOrEmpty()) {
                try {
                    val selectedDate = dateFormatter.parse(dateText)
                    selectedDate?.let {
                        currentDate.time = it
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando fecha existente: ${e.message}")
                }
            }
            
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val formattedDate = dateFormatter.format(calendar.time)
                    binding.inputTaskDueDate.setText(formattedDate)
                    Log.d(TAG, "Fecha seleccionada: $formattedDate")
                },
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)
            )
            
            datePickerDialog.show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando selector de fecha: ${e.message}", e)
            showError("Error al mostrar selector de fecha")
        }
    }
    
    // ========== FUNCIONALIDADES DE IMAGEN ==========
    
    private fun handleAttachmentAction() {
        try {
            if (selectedImageUri != null) {
                // Si ya hay imagen, mostrar opciones
                showAttachmentOptions()
            } else {
                // Si no hay imagen, abrir selector
                openImagePicker()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando adjunto: ${e.message}", e)
            showError("Error al manejar adjunto")
        }
    }
    
    private fun showAttachmentOptions() {
        // Por simplicidad, por ahora solo permitir cambiar la imagen
        openImagePicker()
    }
    
    private fun openImagePicker() {
        try {
            imagePickerLauncher.launch(arrayOf("image/*"))
            Log.d(TAG, "Selector de imagen abierto")
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo selector de imagen: ${e.message}", e)
            showError("Error al abrir selector de imagen")
        }
    }
    
    private fun handleImageSelection(uri: Uri) {
        try {
            // Tomar permiso persistente para la URI
            requireContext().contentResolver.takePersistableUriPermission(
                uri, 
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            selectedImageUri = uri
            updateAttachmentUI()
            
            Log.d(TAG, "Imagen seleccionada: $uri")
            showSuccess("Imagen adjuntada correctamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando selección de imagen: ${e.message}", e)
            showError("Error al adjuntar imagen")
        }
    }
    
    private fun updateAttachmentUI() {
        try {
            if (selectedImageUri != null) {
                binding.textAttachmentName.text = "Imagen adjuntada"
                // Aquí podrías mostrar una vista previa si quisieras
            } else {
                binding.textAttachmentName.text = "Sin adjunto"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando UI de adjunto: ${e.message}", e)
        }
    }
    
    // ========== FUNCIONALIDADES DE VOZ ==========
    
    private fun handleVoiceNoteAction() {
        try {
            if (isRecording) {
                stopRecording()
            } else if (isPlaying) {
                stopPlaying()
            } else if (voiceFilePath != null) {
                startPlaying()
            } else {
                startRecording()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error manejando nota de voz: ${e.message}", e)
            showError("Error al manejar nota de voz")
        }
    }
    
    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        toggleVoiceRecording()
    }
    
    private fun toggleVoiceRecording() {
        try {
            if (isRecording) {
                stopRecording()
            } else {
                // Crear directorio para grabaciones si no existe
                val audioDir = File(requireContext().filesDir, "voice_notes")
                if (!audioDir.exists()) {
                    val created = audioDir.mkdirs()
                    Log.d(TAG, "Directorio de audio creado: $created")
                }
                
                // Crear archivo para la grabación
                val timestamp = System.currentTimeMillis()
                val audioFile = File(audioDir, "voice_note_$timestamp.3gp")
                voiceFilePath = audioFile.absolutePath
                
                Log.d(TAG, "Preparando grabación en: $voiceFilePath")
                
                voiceRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setOutputFile(voiceFilePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    
                    try {
                        prepare()
                        start()
                        isRecording = true
                        updateVoiceNoteUI()
                        Log.d(TAG, "Grabación iniciada exitosamente en: $voiceFilePath")
                        showSuccess("Grabación iniciada")
                    } catch (e: IOException) {
                        Log.e(TAG, "Error preparando grabación: ${e.message}", e)
                        showError("Error al iniciar grabación: ${e.message}")
                        voiceFilePath = null
                        isRecording = false
                        updateVoiceNoteUI()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en grabación: ${e.message}", e)
            showError("Error en grabación de voz: ${e.message}")
            voiceFilePath = null
            isRecording = false
            updateVoiceNoteUI()
        }
    }
    
    private fun stopRecording() {
        try {
            voiceRecorder?.apply {
                stop()
                release()
            }
            voiceRecorder = null
            isRecording = false
            updateVoiceNoteUI()
            
            // Verificar que el archivo se haya creado correctamente
            if (!voiceFilePath.isNullOrEmpty()) {
                val audioFile = File(voiceFilePath!!)
                if (audioFile.exists() && audioFile.length() > 0) {
                    Log.d(TAG, "Grabación detenida exitosamente. Archivo: $voiceFilePath, Tamaño: ${audioFile.length()} bytes")
                    showSuccess("Grabación guardada correctamente")
                } else {
                    Log.e(TAG, "El archivo de audio no se creó correctamente o está vacío")
                    showError("Error: El archivo de audio no se guardó correctamente")
                    voiceFilePath = null
                }
            } else {
                Log.e(TAG, "No hay ruta de archivo de audio")
                showError("Error: No se pudo determinar la ruta del archivo")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo grabación: ${e.message}", e)
            showError("Error al detener grabación: ${e.message}")
            voiceFilePath = null
        }
    }
    
    private fun startPlaying() {
        try {
            if (voiceFilePath.isNullOrEmpty()) {
                showError("No hay archivo de audio para reproducir")
                return
            }
            
            val audioFile = File(voiceFilePath!!)
            if (!audioFile.exists()) {
                showError("Archivo de audio no encontrado")
                Log.e(TAG, "Archivo de audio no existe: $voiceFilePath")
                return
            }
            
            Log.d(TAG, "Iniciando reproducción de: $voiceFilePath")
            
            voicePlayer = MediaPlayer().apply {
                setDataSource(voiceFilePath)
                prepare()
                start()
                
                setOnCompletionListener {
                    this@TaskDetailFragment.isPlaying = false
                    this@TaskDetailFragment.updateVoiceNoteUI()
                    Log.d(TAG, "Reproducción completada")
                }
            }
            
            isPlaying = true
            updateVoiceNoteUI()
            
            Log.d(TAG, "Reproducción iniciada exitosamente")
            showSuccess("Reproduciendo audio")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reproduciendo audio: ${e.message}", e)
            showError("Error al reproducir audio: ${e.message}")
            isPlaying = false
            updateVoiceNoteUI()
        }
    }
    
    private fun stopPlaying() {
        try {
            voicePlayer?.apply {
                stop()
                release()
            }
            voicePlayer = null
            isPlaying = false
            updateVoiceNoteUI()
            
            Log.d(TAG, "Reproducción detenida")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo reproducción: ${e.message}", e)
            showError("Error al detener reproducción")
        }
    }
    
    private fun updateVoiceNoteUI() {
        try {
            when {
                isRecording -> {
                    binding.textVoiceNoteDuration.text = "Grabando..."
                    // Aquí podrías actualizar el ícono para mostrar que está grabando
                }
                isPlaying -> {
                    binding.textVoiceNoteDuration.text = "Reproduciendo..."
                }
                voiceFilePath != null -> {
                    binding.textVoiceNoteDuration.text = "Nota de voz guardada"
                }
                else -> {
                    binding.textVoiceNoteDuration.text = "00:00"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando UI de voz: ${e.message}", e)
        }
    }
    
    // ========== FUNCIONALIDADES DE CALENDARIO ==========
    
    private fun handleExportToCalendar() {
        try {
            val title = binding.inputTaskTitle.text?.toString()?.trim()
            val description = binding.inputTaskDescription.text?.toString()?.trim()
            val dueDate = binding.inputTaskDueDate.text?.toString()?.trim()
            
            if (title.isNullOrEmpty()) {
                showError("Debe ingresar un título para exportar")
                return
            }
            
            if (dueDate.isNullOrEmpty()) {
                showError("Debe seleccionar una fecha para exportar")
                return
            }
            
            // Convertir fecha a timestamp
            val dateTimestamp = try {
                dateFormatter.parse(dueDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                Log.w(TAG, "Error parseando fecha para calendario: ${e.message}")
                System.currentTimeMillis()
            }
            
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description ?: "")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dateTimestamp)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dateTimestamp + (60 * 60 * 1000)) // 1 hora después
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }
            
            startActivity(intent)
            Log.d(TAG, "Intent de calendario enviado")
            showSuccess("Abriendo calendario...")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exportando a calendario: ${e.message}", e)
            showError("Error al exportar al calendario")
        }
    }
    
    // ========== MÉTODOS DE UTILIDAD ==========
    
    private fun cleanupMediaResources() {
        try {
            voiceRecorder?.apply {
                if (isRecording) {
                    stop()
                }
                release()
            }
            voiceRecorder = null
            
            voicePlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            voicePlayer = null
            
            isRecording = false
            isPlaying = false
            
            Log.d(TAG, "Recursos de media liberados")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando recursos: ${e.message}", e)
        }
    }
    
    private fun showError(message: String) {
        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando mensaje de error: ${e.message}", e)
        }
    }

    private fun showSuccess(message: String) {
        try {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando mensaje de éxito: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView iniciado")
        cleanupMediaResources()
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView completado")
    }
}