package dot.jcversa.novay.ui.player

import kotlin.math.hypot

/**
 * Modes de présentation du lecteur adaptatif (strictement séparés de l'état de lecture).
 */
enum class PlayerPresentationMode {
    HIDDEN,
    EMBEDDED_PORTRAIT,
    FLOATING_MINI,
    FULLSCREEN_LANDSCAPE,
}

enum class MiniPlayerCorner {
    TOP_START,
    TOP_END,
    BOTTOM_START,
    BOTTOM_END,
}

data class MiniPlayerBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
)

data class MiniPlayerSizePx(
    val width: Float,
    val height: Float,
)

/**
 * Maths pures de positionnement, collision aux bords sûrs, aimantation intelligente
 * aux coins les plus proches et détection de visibilité au défilement avec hystérésis.
 */
object MiniPlayerGeometry {

    const val DEFAULT_ASPECT_RATIO = 16f / 9f
    const val EXIT_TO_MINI_VISIBLE_THRESHOLD = 0.35f
    const val RETURN_TO_INLINE_VISIBLE_THRESHOLD = 0.65f
    const val SNAP_VELOCITY_PROJECTION_SEC = 0.085f

    /** Largeur responsive du mini-lecteur (dp), adaptée des petits écrans aux tablettes. */
    fun responsiveWidthDp(availableWidthDp: Float): Float = when {
        availableWidthDp <= 0f -> 172f
        availableWidthDp < 360f -> 156f
        availableWidthDp < 600f -> 178f
        availableWidthDp < 840f -> 208f
        else -> 236f
    }

    /** Clamp le ratio d'aspect pour que le mini-lecteur garde une géométrie ergonomique. */
    fun sanitizedAspectRatio(rawRatio: Float): Float =
        if (rawRatio.isFinite() && rawRatio > 0f) {
            rawRatio.coerceIn(1.25f, 2.25f)
        } else {
            DEFAULT_ASPECT_RATIO
        }

    /** Calcule le rectangle sûr de déplacement, en évitant encoches, dock et rail. */
    fun computeSafeBounds(
        containerWidthPx: Float,
        containerHeightPx: Float,
        miniSize: MiniPlayerSizePx,
        leftInsetPx: Float,
        topInsetPx: Float,
        rightInsetPx: Float,
        bottomInsetPx: Float,
    ): MiniPlayerBounds {
        val rawMinX = leftInsetPx
        val rawMinY = topInsetPx
        val rawMaxX = (containerWidthPx - rightInsetPx - miniSize.width).coerceAtLeast(rawMinX)
        val rawMaxY = (containerHeightPx - bottomInsetPx - miniSize.height).coerceAtLeast(rawMinY)
        return MiniPlayerBounds(
            minX = rawMinX,
            minY = rawMinY,
            maxX = rawMaxX,
            maxY = rawMaxY,
        )
    }

    fun clampOffset(
        x: Float,
        y: Float,
        bounds: MiniPlayerBounds,
    ): Pair<Float, Float> = Pair(
        x.coerceIn(bounds.minX, bounds.maxX),
        y.coerceIn(bounds.minY, bounds.maxY),
    )

    fun cornerOffset(
        corner: MiniPlayerCorner,
        bounds: MiniPlayerBounds,
    ): Pair<Float, Float> = when (corner) {
        MiniPlayerCorner.TOP_START -> bounds.minX to bounds.minY
        MiniPlayerCorner.TOP_END -> bounds.maxX to bounds.minY
        MiniPlayerCorner.BOTTOM_START -> bounds.minX to bounds.maxY
        MiniPlayerCorner.BOTTOM_END -> bounds.maxX to bounds.maxY
    }

    /** Choisit le coin sûr le plus proche en tenant compte d'une légère inertie au lâcher. */
    fun nearestCorner(
        x: Float,
        y: Float,
        velocityX: Float,
        velocityY: Float,
        bounds: MiniPlayerBounds,
    ): MiniPlayerCorner {
        val projectedX = (x + velocityX * SNAP_VELOCITY_PROJECTION_SEC).coerceIn(bounds.minX, bounds.maxX)
        val projectedY = (y + velocityY * SNAP_VELOCITY_PROJECTION_SEC).coerceIn(bounds.minY, bounds.maxY)

        return MiniPlayerCorner.entries.minBy { corner ->
            val (cx, cy) = cornerOffset(corner, bounds)
            hypot((projectedX - cx).toDouble(), (projectedY - cy).toDouble())
        }
    }

    /** Calcule la fraction visible [0f..1f] d'un item de LazyColumn. */
    fun computeVisibleFraction(
        itemOffset: Int,
        itemSize: Int,
        viewportStart: Int,
        viewportEnd: Int,
    ): Float {
        if (itemSize <= 0 || viewportEnd <= viewportStart) return 0f
        val itemStart = itemOffset
        val itemEnd = itemOffset + itemSize
        val visiblePx = (minOf(itemEnd, viewportEnd) - maxOf(itemStart, viewportStart)).coerceAtLeast(0)
        return (visiblePx.toFloat() / itemSize.toFloat()).coerceIn(0f, 1f)
    }

    /** Applique une hystérésis pour éviter tout scintillement lors des micro-défilements. */
    fun resolveInlineModeWithHysteresis(
        currentMode: PlayerPresentationMode,
        visibleFraction: Float,
    ): PlayerPresentationMode = when (currentMode) {
        PlayerPresentationMode.FULLSCREEN_LANDSCAPE -> PlayerPresentationMode.FULLSCREEN_LANDSCAPE
        PlayerPresentationMode.HIDDEN -> PlayerPresentationMode.HIDDEN
        PlayerPresentationMode.EMBEDDED_PORTRAIT -> {
            if (visibleFraction < EXIT_TO_MINI_VISIBLE_THRESHOLD) {
                PlayerPresentationMode.FLOATING_MINI
            } else {
                PlayerPresentationMode.EMBEDDED_PORTRAIT
            }
        }
        PlayerPresentationMode.FLOATING_MINI -> {
            if (visibleFraction > RETURN_TO_INLINE_VISIBLE_THRESHOLD) {
                PlayerPresentationMode.EMBEDDED_PORTRAIT
            } else {
                PlayerPresentationMode.FLOATING_MINI
            }
        }
    }
}
