package dev.chieppa.stretch.frame

/**
 * Marks the parts of the surface API that only something drawing a surface calls, such as the render loop answering a
 * [SurfacePickQueue]. Apps that show a surface never need them.
 */
@RequiresOptIn("For code that draws a surface, not code that shows one.", RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class SurfaceImplementation
