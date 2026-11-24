package dev.chieppa.stretch.gpu

/**
 * The wgpu handles behind a [GpuContext], as addresses, for an engine with its own wgpu bindings that draws on the
 * same device: [instance] and [device]. They stay valid while the context does; an engine that keeps them longer takes
 * its own references.
 */
class NativeGpuHandles internal constructor(val instance: Long, val device: Long)

/**
 * The wgpu handles of one [GpuFrame], as addresses: the [encoder] its commands go into and the [output] texture view
 * the frame shows, single-sampled, in the context's format at the frame's size. Drawing into [output] on [encoder]
 * replaces [GpuFrame.beginPass] for an engine that records its own passes. Valid only during the frame.
 */
class NativeFrameHandles internal constructor(val encoder: Long, val output: Long)

/** This context's wgpu instance and device, for an engine that draws on them. Desktop only. */
val GpuContext.nativeHandles: NativeGpuHandles
    get() = NativeGpuHandles(device.instance.address(), device.handle.address())

/** This frame's wgpu command encoder and output view, for an engine that draws into them. Desktop only. */
val GpuFrame.nativeHandles: NativeFrameHandles
    get() = NativeFrameHandles(encoder.handle.address(), output.handle.address())
