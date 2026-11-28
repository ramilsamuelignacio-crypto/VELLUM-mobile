package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.math.RoomUtils
import com.example.theme.VellumColors
import com.example.theme.VellumTheme
import com.example.ui.SketchViewModel
import com.example.ui.components.BrushSettingsMenu
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.LayerPanel
import com.example.ui.components.ReferenceOverlay
import com.example.ui.components.RoomShareModal
import com.example.ui.components.SketchCanvas
import com.example.ui.components.StartupSplash
import com.example.ui.components.ToolDock
import com.example.ui.components.TopBar
import com.example.ui.components.TouchBottomSheet
import com.example.ui.components.TouchControlBar
import com.example.ui.components.TouchQuickBar
import com.example.ui.components.TutorialModal

class MainActivity : ComponentActivity() {
    private var pendingDeepLinkRoom by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLinkRoom = extractRoomFromIntent(intent)
        setContent {
            VellumTheme {
                VellumStudioApp(
                    deepLinkRoom = pendingDeepLinkRoom,
                    onConsumeDeepLink = { pendingDeepLinkRoom = null },
                    activity = this
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractRoomFromIntent(intent)?.let { room ->
            pendingDeepLinkRoom = room
        }
    }

    private fun extractRoomFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val normalized = RoomUtils.normalizeRoomCode(data.toString())
        return normalized.ifEmpty { null }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun VellumStudioApp(
    deepLinkRoom: String?,
    onConsumeDeepLink: () -> Unit,
    activity: ComponentActivity,
    viewModel: SketchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = calculateWindowSizeClass(activity)
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deepLinkRoom) {
        if (!deepLinkRoom.isNullOrBlank()) {
            viewModel.switchToRoom(deepLinkRoom)
            onConsumeDeepLink()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    // Zero-permission Android Photo Picker for pinning Reference Image
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setReferenceUri(uri)
            }
        }
    )

    val launchReferencePicker = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Handle Back navigation cleanly for open sheets/modals
    BackHandler(enabled = uiState.showCustomColorDialog) {
        viewModel.setShowCustomColorDialog(false)
    }
    BackHandler(enabled = uiState.showBrushSettingsMenu) {
        viewModel.setShowBrushSettingsMenu(false)
    }
    BackHandler(enabled = uiState.activeTouchSheet != null) {
        viewModel.closeTouchSheet()
    }
    BackHandler(enabled = uiState.showRoomModal) {
        viewModel.setShowRoomModal(false)
    }
    BackHandler(enabled = uiState.showTutorial && !uiState.showStartupSplash) {
        viewModel.setShowTutorial(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VellumColors.StudioDesk)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = VellumColors.StudioDesk,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            val _unused = innerPadding
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopBar(
                    roomId = uiState.roomId,
                    connected = uiState.connected,
                    collaboratorCount = uiState.collaborators.size,
                    strokeCount = uiState.strokes.size,
                    isExpanded = isExpanded,
                    onNewRoom = viewModel::createNewRoom,
                    onExportPng = viewModel::exportCanvasPng,
                    onOpenRoomModal = { viewModel.setShowRoomModal(true) },
                    onOpenGuide = { viewModel.setShowTutorial(true) }
                )

                if (!isExpanded) {
                    TouchQuickBar(
                        activeTool = uiState.activeTool,
                        onSelectTool = viewModel::selectTool,
                        activeBrush = uiState.activeBrush,
                        onSelectBrush = viewModel::selectBrush,
                        colorArgb = uiState.colorArgb,
                        onSelectColor = viewModel::selectColor,
                        strokeSize = uiState.strokeSize,
                        onChangeSize = viewModel::setStrokeSize,
                        brushOpacity = uiState.brushOpacity,
                        onOpenBrushSettings = { viewModel.setShowBrushSettingsMenu(true) }
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isExpanded) {
                        ToolDock(
                            activeTool = uiState.activeTool,
                            onSelectTool = viewModel::selectTool,
                            colorArgb = uiState.colorArgb,
                            onSelectColor = viewModel::selectColor,
                            onOpenCustomColor = { viewModel.setShowCustomColorDialog(true) },
                            strokeSize = uiState.strokeSize,
                            onChangeSize = viewModel::setStrokeSize,
                            brushOpacity = uiState.brushOpacity,
                            onOpenBrushSettings = { viewModel.setShowBrushSettingsMenu(true) },
                            stabilization = uiState.stabilization,
                            onChangeStabilization = viewModel::setStabilization,
                            pressureEnabled = uiState.pressureEnabled,
                            onTogglePressure = viewModel::togglePressure,
                            activeBrush = uiState.activeBrush,
                            onSelectBrush = viewModel::selectBrush,
                            showGrid = uiState.showGrid,
                            onToggleGrid = viewModel::toggleGrid,
                            canUndo = uiState.canUndo,
                            onUndo = viewModel::undo,
                            onClear = viewModel::clearCanvas,
                            onPickReference = launchReferencePicker
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        SketchCanvas(
                            strokes = uiState.strokes,
                            layers = uiState.layers,
                            activeLayerId = uiState.activeLayerId,
                            localUserId = uiState.localUserId,
                            activeTool = uiState.activeTool,
                            activeBrush = uiState.activeBrush,
                            colorArgb = uiState.colorArgb,
                            strokeSize = uiState.strokeSize,
                            brushOpacity = uiState.brushOpacity,
                            stabilization = uiState.stabilization,
                            pressureEnabled = uiState.pressureEnabled,
                            showGrid = uiState.showGrid,
                            showPaperGrain = uiState.showPaperGrain,
                            zoom = uiState.zoom,
                            pan = uiState.pan,
                            symmetryMode = uiState.symmetryMode,
                            symmetryCenter = uiState.symmetryCenter,
                            guideMode = uiState.guideMode,
                            touchPanMode = uiState.touchPanMode,
                            selectedStrokeId = uiState.selectedStrokeId,
                            isExpanded = isExpanded,
                            onCanvasResize = viewModel::onCanvasResize,
                            onPanZoom = viewModel::setPanAndZoom,
                            onZoomStep = viewModel::zoomStep,
                            onResetView = viewModel::resetView,
                            onSelectStroke = viewModel::setSelectedStrokeId,
                            onCommitStroke = viewModel::commitStroke,
                            onUpdateStrokePreview = viewModel::updateStrokePreview,
                            onChangeSymmetryMode = viewModel::setSymmetryMode,
                            onRecenterSymmetry = viewModel::recenterSymmetry,
                            onChangeGuideMode = viewModel::setGuideMode
                        )

                        uiState.referenceUri?.let { refUri ->
                            ReferenceOverlay(
                                imageUri = refUri,
                                containerWidthPx = uiState.canvasWidth,
                                containerHeightPx = uiState.canvasHeight,
                                onClose = { viewModel.setReferenceUri(null) }
                            )
                        }
                    }

                    if (isExpanded) {
                        LayerPanel(
                            layers = uiState.layers,
                            activeLayerId = uiState.activeLayerId,
                            onSelectLayer = viewModel::selectLayer,
                            onAddLayer = viewModel::addLayer,
                            onToggleVisibility = viewModel::toggleLayerVisibility,
                            onMoveLayer = viewModel::moveLayer
                        )
                    }
                }

                if (!isExpanded) {
                    TouchControlBar(
                        activeSheet = uiState.activeTouchSheet,
                        onToggleSheet = viewModel::toggleTouchSheet,
                        touchPanMode = uiState.touchPanMode,
                        onToggleTouchPan = viewModel::toggleTouchPan,
                        activeTool = uiState.activeTool,
                        colorArgb = uiState.colorArgb,
                        canUndo = uiState.canUndo,
                        onUndo = viewModel::undo,
                        layerCount = uiState.layers.size
                    )
                }
            }
        }

        if (!isExpanded) {
            TouchBottomSheet(
                activeSheet = uiState.activeTouchSheet,
                onClose = viewModel::closeTouchSheet,
                activeTool = uiState.activeTool,
                onSelectTool = viewModel::selectTool,
                colorArgb = uiState.colorArgb,
                customColors = uiState.customColors,
                onSelectColor = viewModel::selectColor,
                onOpenCustomColor = { viewModel.setShowCustomColorDialog(true) },
                strokeSize = uiState.strokeSize,
                onChangeSize = viewModel::setStrokeSize,
                brushOpacity = uiState.brushOpacity,
                onChangeOpacity = viewModel::setBrushOpacity,
                onOpenBrushSettings = { viewModel.setShowBrushSettingsMenu(true) },
                stabilization = uiState.stabilization,
                onChangeStabilization = viewModel::setStabilization,
                pressureEnabled = uiState.pressureEnabled,
                onTogglePressure = viewModel::togglePressure,
                activeBrush = uiState.activeBrush,
                onSelectBrush = viewModel::selectBrush,
                symmetryMode = uiState.symmetryMode,
                onChangeSymmetryMode = viewModel::setSymmetryMode,
                onRecenterSymmetry = viewModel::recenterSymmetry,
                guideMode = uiState.guideMode,
                onChangeGuideMode = viewModel::setGuideMode,
                showGrid = uiState.showGrid,
                onToggleGrid = viewModel::toggleGrid,
                zoom = uiState.zoom,
                onZoomStep = viewModel::zoomStep,
                onResetView = viewModel::resetView,
                onPickReference = launchReferencePicker,
                onExportPng = viewModel::exportCanvasPng,
                onClearCanvas = viewModel::clearCanvas,
                layers = uiState.layers,
                activeLayerId = uiState.activeLayerId,
                onSelectLayer = viewModel::selectLayer,
                onAddLayer = viewModel::addLayer,
                onToggleLayerVisibility = viewModel::toggleLayerVisibility,
                onMoveLayer = viewModel::moveLayer
            )
        }

        if (uiState.showBrushSettingsMenu) {
            BrushSettingsMenu(
                activeTool = uiState.activeTool,
                activeBrush = uiState.activeBrush,
                onSelectBrush = viewModel::selectBrush,
                strokeSize = uiState.strokeSize,
                onChangeSize = viewModel::setStrokeSize,
                brushOpacity = uiState.brushOpacity,
                onChangeOpacity = viewModel::setBrushOpacity,
                colorArgb = uiState.colorArgb,
                customColors = uiState.customColors,
                onSelectColor = viewModel::selectColor,
                onOpenCustomColor = { viewModel.setShowCustomColorDialog(true) },
                stabilization = uiState.stabilization,
                onChangeStabilization = viewModel::setStabilization,
                pressureEnabled = uiState.pressureEnabled,
                onTogglePressure = viewModel::togglePressure,
                onDismiss = { viewModel.setShowBrushSettingsMenu(false) }
            )
        }

        if (uiState.showCustomColorDialog) {
            ColorPickerDialog(
                initialColorArgb = uiState.colorArgb,
                onConfirm = { chosen ->
                    viewModel.selectColor(chosen)
                    viewModel.setShowCustomColorDialog(false)
                },
                onDismiss = { viewModel.setShowCustomColorDialog(false) }
            )
        }

        if (uiState.showRoomModal) {
            RoomShareModal(
                roomId = uiState.roomId,
                collaboratorCount = uiState.collaborators.size,
                onClose = { viewModel.setShowRoomModal(false) },
                onJoinRoom = viewModel::switchToRoom,
                onCreateNewRoom = viewModel::createNewRoom,
                onShareQr = viewModel::shareQrPng
            )
        }

        if (uiState.showTutorial && !uiState.showStartupSplash) {
            TutorialModal(
                onClose = { viewModel.setShowTutorial(false) }
            )
        }

        if (uiState.showStartupSplash) {
            StartupSplash(
                onComplete = viewModel::dismissSplash
            )
        }
    }
}
