package com.example.model

enum class ToolType(val label: String, val shortcut: String) {
    SELECT("Select", "V"),
    PEN("Pen", "P"),
    HIGHLIGHTER("Highlighter", "H"),
    ERASER("Eraser", "E"),
    LINE("Line", "L"),
    RECT("Rectangle", "R"),
    CIRCLE("Circle", "C")
}

enum class SymmetryMode(val label: String) {
    NONE("Off"),
    HORIZONTAL("Horizontal"),
    VERTICAL("Vertical"),
    RADIAL("Radial"),
    KALEIDOSCOPE("Kaleidoscope")
}

enum class GuideMode(val label: String) {
    NONE("Off"),
    PERSPECTIVE("Perspective"),
    RULER("Ruler")
}

enum class BrushKind(val id: String, val label: String, val shortLabel: String) {
    HARD_ROUND("hard-round", "Hard Round", "Ink"),
    HB_PENCIL("hb-pencil", "HB Pencil", "HB"),
    SOFT_AIRBRUSH("soft-airbrush", "Soft Airbrush", "Air");

    companion object {
        fun fromId(id: String): BrushKind {
            val normalized = id.trim().lowercase()
            return when (normalized) {
                "hb-pencil", "pencil" -> HB_PENCIL
                "soft-airbrush", "airbrush" -> SOFT_AIRBRUSH
                else -> HARD_ROUND
            }
        }
    }
}

enum class TouchSheetTab {
    TOOLS,
    CANVAS,
    LAYERS
}

data class SketchPoint(
    val x: Float,
    val y: Float,
    val pressure: Float? = null,
    val tiltX: Float? = null,
    val tiltY: Float? = null,
    val twist: Float? = null
)

data class Layer(
    val id: String,
    val name: String,
    val visible: Boolean = true
)

data class Stroke(
    val id: String,
    val userId: String,
    val layerId: String,
    val tool: ToolType,
    val colorArgb: Int,
    val size: Float,
    val opacity: Float,
    val points: List<SketchPoint>,
    val rotation: Float = 0f,
    val symmetry: SymmetryMode = SymmetryMode.NONE,
    val symmetryCenter: SketchPoint? = null,
    val brush: String = BrushKind.HARD_ROUND.id,
    val pressureSensitive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class Collaborator(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val cursor: SketchPoint? = null
)
