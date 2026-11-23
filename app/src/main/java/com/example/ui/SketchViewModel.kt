package com.example.ui

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SketchRepository
import com.example.engine.BrushEngine
import com.example.math.RoomUtils
import com.example.math.SnapGuides
import com.example.math.StrokeMath
import com.example.model.BrushKind
import com.example.model.Collaborator
import com.example.model.GuideMode
import com.example.model.Layer
import com.example.model.SketchPoint
import com.example.model.Stroke
import com.example.model.SymmetryMode
import com.example.model.ToolType
import com.example.model.TouchSheetTab
import com.example.theme.VellumColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.round

data class StudioUiState(
    val roomId: String = "studio-1",
    val activeTool: ToolType = ToolType.PEN,
    val colorArgb: Int = VellumColors.Swatches.first(),
    val strokeSize: Float = 4f,
    val stabilization: Float = 0.5f,
    val pressureEnabled: Boolean = true,
    val activeBrush: String = BrushKind.HARD_ROUND.id,
    val showGrid: Boolean = true,
    val showPaperGrain: Boolean = true,
    val referenceUri: Uri? = null,
    val showStartupSplash: Boolean = true,
    val showTutorial: Boolean = true,
    val showRoomModal: Boolean = false,
    val showCustomColorDialog: Boolean = false,
    val zoom: Float = 1f,
    val pan: SketchPoint = SketchPoint(0f, 0f),
    val symmetryMode: SymmetryMode = SymmetryMode.NONE,
    val guideMode: GuideMode = GuideMode.NONE,
    val canvasWidth: Float = 1200f,
    val canvasHeight: Float = 760f,
    val symmetryCenter: SketchPoint = SketchPoint(600f, 380f),
    val touchPanMode: Boolean = false,
    val activeTouchSheet: TouchSheetTab? = null,
    val activeLayerId: String = "layer-1",
    val selectedStrokeId: String? = null,
    val snapGuides: SnapGuides = SnapGuides(),
    val strokes: List<Stroke> = emptyList(),
    val layers: List<Layer> = listOf(Layer("layer-1", "Layer 1", true)),
    val collaborators: List<Collaborator> = emptyList(),
    val connected: Boolean = true,
    val localUserId: String = "",
    val canUndo: Boolean = false,
    val snackbarMessage: String? = null
)

private data class LocalStudioControls(
    val roomId: String = "studio-1",
    val activeTool: ToolType = ToolType.PEN,
    val colorArgb: Int = VellumColors.Swatches.first(),
    val strokeSize: Float = 4f,
    val stabilization: Float = 0.5f,
    val pressureEnabled: Boolean = true,
    val activeBrush: String = BrushKind.HARD_ROUND.id,
    val showGrid: Boolean = true,
    val showPaperGrain: Boolean = true,
    val referenceUri: Uri? = null,
    val showStartupSplash: Boolean = true,
    val showTutorial: Boolean = true,
    val showRoomModal: Boolean = false,
    val showCustomColorDialog: Boolean = false,
    val zoom: Float = 1f,
    val pan: SketchPoint = SketchPoint(0f, 0f),
    val symmetryMode: SymmetryMode = SymmetryMode.NONE,
    val guideMode: GuideMode = GuideMode.NONE,
    val canvasWidth: Float = 1200f,
    val canvasHeight: Float = 760f,
    val symmetryCenter: SketchPoint = SketchPoint(600f, 380f),
    val hasCustomSymmetryCenter: Boolean = false,
    val touchPanMode: Boolean = false,
    val activeTouchSheet: TouchSheetTab? = null,
    val activeLayerId: String = "layer-1",
    val selectedStrokeId: String? = null,
    val snapGuides: SnapGuides = SnapGuides(),
    val snackbarMessage: String? = null
)

class SketchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SketchRepository(application)
    private val controls = MutableStateFlow(LocalStudioControls())

    init {
        repository.joinRoom("studio-1")
    }

    val uiState: StateFlow<StudioUiState> = combine(
        controls,
        repository.strokes,
        repository.layers,
        repository.collaborators,
        repository.connected
    ) { ctrl, strokes, layers, collaborators, connected ->
        val resolvedActiveLayerId = if (layers.isNotEmpty() && layers.none { it.id == ctrl.activeLayerId }) {
            layers.last().id
        } else {
            ctrl.activeLayerId
        }
        val lastUserStrokeId = StrokeMath.getLastUserStrokeId(strokes, repository.localUser.id)
        StudioUiState(
            roomId = ctrl.roomId,
            activeTool = ctrl.activeTool,
            colorArgb = ctrl.colorArgb,
            strokeSize = ctrl.strokeSize,
            stabilization = ctrl.stabilization,
            pressureEnabled = ctrl.pressureEnabled,
            activeBrush = ctrl.activeBrush,
            showGrid = ctrl.showGrid,
            showPaperGrain = ctrl.showPaperGrain,
            referenceUri = ctrl.referenceUri,
            showStartupSplash = ctrl.showStartupSplash,
            showTutorial = ctrl.showTutorial,
            showRoomModal = ctrl.showRoomModal,
            showCustomColorDialog = ctrl.showCustomColorDialog,
            zoom = ctrl.zoom,
            pan = ctrl.pan,
            symmetryMode = ctrl.symmetryMode,
            guideMode = ctrl.guideMode,
            canvasWidth = ctrl.canvasWidth,
            canvasHeight = ctrl.canvasHeight,
            symmetryCenter = ctrl.symmetryCenter,
            touchPanMode = ctrl.touchPanMode,
            activeTouchSheet = ctrl.activeTouchSheet,
            activeLayerId = resolvedActiveLayerId,
            selectedStrokeId = if (ctrl.activeTool == ToolType.SELECT) ctrl.selectedStrokeId else null,
            snapGuides = ctrl.snapGuides,
            strokes = strokes,
            layers = layers,
            collaborators = collaborators,
            connected = connected,
            localUserId = repository.localUser.id,
            canUndo = lastUserStrokeId != null,
            snackbarMessage = ctrl.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudioUiState(
            localUserId = repository.localUser.id,
            collaborators = listOf(repository.localUser)
        )
    )

    fun dismissSplash() {
        controls.update { it.copy(showStartupSplash = false) }
    }

    fun setShowTutorial(show: Boolean) {
        controls.update { it.copy(showTutorial = show) }
    }

    fun setShowRoomModal(show: Boolean) {
        controls.update { it.copy(showRoomModal = show) }
    }

    fun setShowCustomColorDialog(show: Boolean) {
        controls.update { it.copy(showCustomColorDialog = show) }
    }

    fun selectTool(tool: ToolType) {
        controls.update {
            it.copy(
                activeTool = tool,
                touchPanMode = false,
                selectedStrokeId = if (tool == ToolType.SELECT) it.selectedStrokeId else null
            )
        }
    }

    fun selectColor(colorArgb: Int) {
        controls.update {
            it.copy(
                colorArgb = colorArgb,
                activeTool = if (it.activeTool == ToolType.ERASER) ToolType.PEN else it.activeTool,
                touchPanMode = false
            )
        }
    }

    fun setStrokeSize(size: Float) {
        controls.update { it.copy(strokeSize = StrokeMath.clamp(size, 1f, 32f)) }
    }

    fun setStabilization(value: Float) {
        controls.update { it.copy(stabilization = StrokeMath.clamp(value, 0f, 0.85f)) }
    }

    fun togglePressure() {
        controls.update { it.copy(pressureEnabled = !it.pressureEnabled) }
    }

    fun selectBrush(brushId: String) {
        controls.update { it.copy(activeBrush = brushId, touchPanMode = false) }
    }

    fun toggleGrid() {
        controls.update { it.copy(showGrid = !it.showGrid) }
    }

    fun togglePaperGrain() {
        controls.update { it.copy(showPaperGrain = !it.showPaperGrain) }
    }

    fun setGuideMode(mode: GuideMode) {
        controls.update { it.copy(guideMode = mode) }
    }

    fun setReferenceUri(uri: Uri?) {
        controls.update { it.copy(referenceUri = uri) }
    }

    fun toggleTouchPan() {
        controls.update { it.copy(touchPanMode = !it.touchPanMode) }
    }

    fun toggleTouchSheet(tab: TouchSheetTab) {
        controls.update {
            it.copy(activeTouchSheet = if (it.activeTouchSheet == tab) null else tab)
        }
    }

    fun closeTouchSheet() {
        controls.update { it.copy(activeTouchSheet = null) }
    }

    fun openToolsSheet() {
        controls.update { it.copy(activeTouchSheet = TouchSheetTab.TOOLS) }
    }

    fun setSelectedStrokeId(strokeId: String?) {
        controls.update { it.copy(selectedStrokeId = strokeId) }
    }

    fun setSnapGuides(guides: SnapGuides) {
        controls.update { it.copy(snapGuides = guides) }
    }

    fun onCanvasResize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        controls.update {
            if (!it.hasCustomSymmetryCenter) {
                it.copy(
                    canvasWidth = width,
                    canvasHeight = height,
                    symmetryCenter = SketchPoint(width / 2f, height / 2f)
                )
            } else {
                it.copy(canvasWidth = width, canvasHeight = height)
            }
        }
    }

    fun recenterSymmetry() {
        controls.update {
            val worldCenter = StrokeMath.screenToWorld(
                SketchPoint(it.canvasWidth / 2f, it.canvasHeight / 2f),
                it.pan,
                it.zoom
            )
            it.copy(symmetryCenter = worldCenter, hasCustomSymmetryCenter = true)
        }
    }

    fun setSymmetryMode(mode: SymmetryMode) {
        controls.update {
            val nextCenter = if (mode != SymmetryMode.NONE && it.symmetryMode == SymmetryMode.NONE) {
                StrokeMath.screenToWorld(
                    SketchPoint(it.canvasWidth / 2f, it.canvasHeight / 2f),
                    it.pan,
                    it.zoom
                )
            } else {
                it.symmetryCenter
            }
            it.copy(
                symmetryMode = mode,
                symmetryCenter = nextCenter,
                hasCustomSymmetryCenter = mode != SymmetryMode.NONE || it.hasCustomSymmetryCenter
            )
        }
    }

    fun setPanAndZoom(pan: SketchPoint, zoom: Float) {
        controls.update {
            it.copy(pan = pan, zoom = StrokeMath.clamp(zoom, 0.25f, 4f))
        }
    }

    fun zoomStep(delta: Float) {
        controls.update {
            val nextZoom = StrokeMath.clamp(round((it.zoom + delta) * 100f) / 100f, 0.25f, 4f)
            if (nextZoom == it.zoom) return@update it
            val focalX = it.canvasWidth / 2f
            val focalY = it.canvasHeight / 2f
            val worldX = (focalX - it.pan.x) / it.zoom
            val worldY = (focalY - it.pan.y) / it.zoom
            it.copy(
                zoom = nextZoom,
                pan = SketchPoint(focalX - worldX * nextZoom, focalY - worldY * nextZoom)
            )
        }
    }

    fun resetView() {
        controls.update { it.copy(zoom = 1f, pan = SketchPoint(0f, 0f)) }
    }

    fun selectLayer(layerId: String) {
        controls.update { it.copy(activeLayerId = layerId) }
    }

    fun addLayer() {
        val nextNum = repository.layers.value.size + 1
        val newLayer = Layer(
            id = "layer-${System.currentTimeMillis()}",
            name = "Layer $nextNum",
            visible = true
        )
        repository.createLayer(newLayer)
        controls.update { it.copy(activeLayerId = newLayer.id) }
    }

    fun toggleLayerVisibility(layerId: String) {
        repository.toggleLayer(layerId)
    }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        repository.moveLayer(fromIndex, toIndex)
    }

    fun startStroke(stroke: Stroke) {
        repository.startStroke(stroke)
    }

    fun appendPoints(strokeId: String, points: List<SketchPoint>) {
        repository.appendPoints(strokeId, points)
    }

    fun endStroke(stroke: Stroke) {
        repository.endStroke(stroke)
    }

    fun undo() {
        val lastId = StrokeMath.getLastUserStrokeId(repository.strokes.value, repository.localUser.id)
        if (lastId != null) {
            repository.undoStroke(lastId)
        }
    }

    fun clearCanvas() {
        repository.clearCanvas()
        controls.update { it.copy(selectedStrokeId = null, snapGuides = SnapGuides()) }
    }

    fun switchToRoom(rawInput: String) {
        val normalized = RoomUtils.normalizeRoomCode(rawInput)
        if (normalized.isEmpty()) return
        controls.update {
            it.copy(
                roomId = normalized,
                selectedStrokeId = null,
                snapGuides = SnapGuides(),
                snackbarMessage = "Joined room $normalized"
            )
        }
        repository.joinRoom(normalized)
    }

    fun createNewRoom() {
        val code = StrokeMath.generateRoomCode()
        switchToRoom(code)
    }

    fun clearSnackbar() {
        controls.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(msg: String) {
        controls.update { it.copy(snackbarMessage = msg) }
    }

    fun exportCanvasPng() {
        val state = uiState.value
        val app = getApplication<Application>()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val exportW = state.canvasWidth.toInt().coerceIn(480, 2400)
                    val exportH = state.canvasHeight.toInt().coerceIn(320, 2400)
                    val bitmap = Bitmap.createBitmap(exportW, exportH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    val strokesBitmap = Bitmap.createBitmap(exportW, exportH, Bitmap.Config.ARGB_8888)
                    val strokesCanvas = Canvas(strokesBitmap)
                    strokesCanvas.save()
                    strokesCanvas.translate(state.pan.x, state.pan.y)
                    strokesCanvas.scale(state.zoom, state.zoom)
                    val visibleStrokes = StrokeMath.getVisibleStrokesInLayerOrder(state.strokes, state.layers)
                    for (stroke in visibleStrokes) {
                        BrushEngine.renderBrushStrokeToCanvas(strokesCanvas, stroke, state.symmetryCenter)
                    }
                    strokesCanvas.restore()
                    canvas.drawBitmap(strokesBitmap, 0f, 0f, null)
                    strokesBitmap.recycle()

                    val fileName = "vellum-${state.roomId}-${System.currentTimeMillis()}.png"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Vellum")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                            app.contentResolver.openOutputStream(uri)?.use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            app.contentResolver.update(uri, values, null, null)
                        }
                    }

                    val exportsDir = File(app.cacheDir, "exports").apply { mkdirs() }
                    val outFile = File(exportsDir, fileName)
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    bitmap.recycle()
                    FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", outFile)
                }
            }

            result.onSuccess { fileUri ->
                runCatching {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Export Vellum Sketch").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(chooser)
                }
                controls.update { it.copy(snackbarMessage = "Exported PNG for ${state.roomId}") }
            }.onFailure {
                controls.update { it.copy(snackbarMessage = "Unable to export PNG right now") }
            }
        }
    }

    fun shareQrPng() {
        val state = uiState.value
        val app = getApplication<Application>()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val qr = RoomUtils.generateQrBitmap(RoomUtils.buildRoomShareUrl(state.roomId), 512)
                        ?: return@runCatching null
                    val exportsDir = File(app.cacheDir, "exports").apply { mkdirs() }
                    val outFile = File(exportsDir, "vellum-room-${state.roomId}-qr.png")
                    FileOutputStream(outFile).use { out ->
                        qr.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    qr.recycle()
                    FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", outFile)
                }
            }
            result.onSuccess { fileUri ->
                if (fileUri != null) {
                    runCatching {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = Intent.createChooser(shareIntent, "Share Room QR Code").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        app.startActivity(chooser)
                    }
                } else {
                    controls.update { it.copy(snackbarMessage = "Unable to generate QR image") }
                }
            }.onFailure {
                controls.update { it.copy(snackbarMessage = "Unable to share QR code right now") }
            }
        }
    }
}
