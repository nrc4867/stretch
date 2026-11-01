package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.WGPUBlendComponent
import dev.chieppa.stretch.gpu.ffi.WGPUBlendState
import dev.chieppa.stretch.gpu.ffi.WGPUColorTargetState
import dev.chieppa.stretch.gpu.ffi.WGPUDepthStencilState
import dev.chieppa.stretch.gpu.ffi.WGPUFragmentState
import dev.chieppa.stretch.gpu.ffi.WGPUMultisampleState
import dev.chieppa.stretch.gpu.ffi.WGPUPrimitiveState
import dev.chieppa.stretch.gpu.ffi.WGPURenderPipelineDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUStencilFaceState
import dev.chieppa.stretch.gpu.ffi.WGPUVertexAttribute
import dev.chieppa.stretch.gpu.ffi.WGPUVertexBufferLayout
import dev.chieppa.stretch.gpu.ffi.WGPUVertexState
import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** Every sample of a pixel, as webgpu.h's default multisample mask. */
private const val ALL_SAMPLES = -1

/** [spec] as a WGPURenderPipelineDescriptor in [arena], with webgpu.h's defaults for what Stretch leaves out. */
internal fun renderPipelineDescriptor(arena: Arena, spec: RenderPipelineSpec): MemorySegment {
    val descriptor = arena.allocate(WGPURenderPipelineDescriptor.layout())
    arena.setString(WGPURenderPipelineDescriptor.label(descriptor), spec.label)

    val vertex = WGPURenderPipelineDescriptor.vertex(descriptor)
    WGPUVertexState.module(vertex, spec.shader.handle)
    arena.setString(WGPUVertexState.entryPoint(vertex), spec.vertexEntry)
    WGPUVertexState.bufferCount(vertex, spec.buffers.size.toLong())
    WGPUVertexState.buffers(
        vertex,
        arena.structs(spec.buffers.size, WGPUVertexBufferLayout.layout()) { i, layout ->
            val buffer = spec.buffers[i]
            WGPUVertexBufferLayout.stepMode(layout, buffer.stepMode.native)
            WGPUVertexBufferLayout.arrayStride(layout, buffer.stride)
            WGPUVertexBufferLayout.attributeCount(layout, buffer.attributes.size.toLong())
            WGPUVertexBufferLayout.attributes(
                layout,
                arena.structs(buffer.attributes.size, WGPUVertexAttribute.layout()) { j, attribute ->
                    val (format, offset, location) = buffer.attributes[j]
                    WGPUVertexAttribute.format(attribute, format.native)
                    WGPUVertexAttribute.offset(attribute, offset)
                    WGPUVertexAttribute.shaderLocation(attribute, location)
                },
            )
        },
    )

    val primitive = WGPURenderPipelineDescriptor.primitive(descriptor)
    WGPUPrimitiveState.topology(primitive, spec.topology.native)
    WGPUPrimitiveState.frontFace(primitive, wgpu_h.WGPUFrontFace_CCW())
    WGPUPrimitiveState.cullMode(primitive, spec.cullMode.native)

    spec.depth?.let { depth ->
        val state = arena.allocate(WGPUDepthStencilState.layout())
        WGPUDepthStencilState.format(state, depth.format.native)
        WGPUDepthStencilState.depthWriteEnabled(state, optional(depth.write))
        WGPUDepthStencilState.depthCompare(state, depth.compare.native)
        listOf(WGPUDepthStencilState.stencilFront(state), WGPUDepthStencilState.stencilBack(state)).forEach { face ->
            WGPUStencilFaceState.compare(face, CompareFunction.Always.native)
            WGPUStencilFaceState.failOp(face, wgpu_h.WGPUStencilOperation_Keep())
            WGPUStencilFaceState.depthFailOp(face, wgpu_h.WGPUStencilOperation_Keep())
            WGPUStencilFaceState.passOp(face, wgpu_h.WGPUStencilOperation_Keep())
        }
        WGPUDepthStencilState.stencilReadMask(state, ALL_SAMPLES)
        WGPUDepthStencilState.stencilWriteMask(state, ALL_SAMPLES)
        WGPURenderPipelineDescriptor.depthStencil(descriptor, state)
    }

    val multisample = WGPURenderPipelineDescriptor.multisample(descriptor)
    WGPUMultisampleState.count(multisample, spec.sampleCount)
    WGPUMultisampleState.mask(multisample, ALL_SAMPLES)

    spec.fragmentEntry?.let { entry ->
        val fragment = arena.allocate(WGPUFragmentState.layout())
        WGPUFragmentState.module(fragment, spec.shader.handle)
        arena.setString(WGPUFragmentState.entryPoint(fragment), entry)
        WGPUFragmentState.targetCount(fragment, spec.targets.size.toLong())
        WGPUFragmentState.targets(
            fragment,
            arena.structs(spec.targets.size, WGPUColorTargetState.layout()) { i, target ->
                val color = spec.targets[i]
                WGPUColorTargetState.format(target, color.format.native)
                WGPUColorTargetState.writeMask(target, COLOR_WRITE_ALL)
                color.blend?.let { blend ->
                    val state = arena.allocate(WGPUBlendState.layout())
                    blendComponent(WGPUBlendState.color(state), blend.color)
                    blendComponent(WGPUBlendState.alpha(state), blend.alpha)
                    WGPUColorTargetState.blend(target, state)
                }
            },
        )
        WGPURenderPipelineDescriptor.fragment(descriptor, fragment)
    }
    return descriptor
}

private fun blendComponent(struct: MemorySegment, component: BlendComponent) {
    WGPUBlendComponent.operation(struct, component.operation.native)
    WGPUBlendComponent.srcFactor(struct, component.srcFactor.native)
    WGPUBlendComponent.dstFactor(struct, component.dstFactor.native)
}
