package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.HeadlessGpu
import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** Runs compute shaders on a real adapter; runs only with -Pgpu. */
class ComputeGpuTest {
    private val doubling = """
        @group(0) @binding(0) var<storage, read> input: array<f32>;
        @group(0) @binding(1) var<storage, read_write> output: array<f32>;

        @compute @workgroup_size(64)
        fn main(@builtin(global_invocation_id) id: vec3<u32>) {
            if (id.x < arrayLength(&input)) {
                output[id.x] = input[id.x] * 2.0;
            }
        }
    """.trimIndent()

    private val input = FloatArray(1000) { it * 0.5f - 100f }

    @Test
    fun doublesAnArrayAndReadsItBack() = runTest {
        assertEquals(input.map { it * 2f }, doubled { pass, _ -> pass.dispatchWorkgroups(workgroups(input.size)) }.toList())
    }

    @Test
    fun dispatchesAsManyWorkgroupsAsABufferSays() = runTest {
        val doubled = doubled { pass, device ->
            val counts = device.createBuffer(12, BufferUsage.Indirect + BufferUsage.CopyDst)
            device.queue.writeBuffer(counts, intArrayOf(workgroups(input.size), 1, 1))
            pass.dispatchWorkgroupsIndirect(counts)
        }
        assertEquals(input.map { it * 2f }, doubled.toList())
    }

    @Test
    fun copiesPartOfOneBufferIntoAnother() = runTest {
        withDevice { device ->
            val bytes = 16L
            device.createBuffer(bytes, BufferUsage.CopySrc + BufferUsage.CopyDst).use { source ->
                device.createBuffer(bytes, BufferUsage.CopyDst + BufferUsage.MapRead).use { readback ->
                    device.queue.writeBuffer(source, intArrayOf(1, 2, 3, 4))
                    device.createCommandEncoder().use { encoder ->
                        encoder.copyBufferToBuffer(source, readback, size = 8, sourceOffset = 8, destinationOffset = 4)
                        device.queue.submit(encoder.finish())
                    }
                    assertContentEquals(intArrayOf(0, 3, 4, 0), ints(device.readBuffer(readback)))
                }
            }
        }
    }

    /** [input] through the doubling shader, dispatched by [dispatch], read back. */
    private suspend fun doubled(dispatch: (GpuComputePass, GpuDevice) -> Unit): FloatArray = withDevice { device ->
        val bytes = input.size * 4L
        val shader = device.createShaderModule(doubling)
        val pipeline = device.createComputePipeline(ComputePipelineSpec(shader, "main"))
        val source = device.createBuffer(bytes, BufferUsage.Storage + BufferUsage.CopyDst)
        val results = device.createBuffer(bytes, BufferUsage.Storage + BufferUsage.CopySrc)
        val readback = device.createBuffer(bytes, BufferUsage.CopyDst + BufferUsage.MapRead)
        val layout = pipeline.bindGroupLayout(0)
        val group = device.createBindGroup(layout, listOf(BufferBinding(0, source), BufferBinding(1, results)))
        layout.close()
        device.queue.writeBuffer(source, input)
        device.createCommandEncoder().use { encoder ->
            val pass = encoder.beginComputePass()
            pass.setPipeline(pipeline)
            pass.setBindGroup(0, group)
            dispatch(pass, device)
            pass.end()
            encoder.copyBufferToBuffer(results, readback)
            device.queue.submit(encoder.finish())
        }
        val out = floats(device.readBuffer(readback))
        listOf(group, readback, results, source, pipeline, shader).forEach { it.close() }
        out
    }

    /** Runs [block] on a device of its own with no surface, failing on any error WebGPU reports. */
    private suspend fun <T> withDevice(block: suspend (GpuDevice) -> T): T {
        val errors = mutableListOf<String>()
        return HeadlessGpu.open(GpuOptions(onError = { synchronized(errors) { errors += it } })).use { gpu ->
            block(gpu.device).also { assertEquals(emptyList(), synchronized(errors) { errors.toList() }) }
        }
    }

    private fun workgroups(count: Int) = (count + 63) / 64

    private fun floats(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().let { FloatArray(it.remaining()).apply(it::get) }

    private fun ints(bytes: ByteArray) = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().let { IntArray(it.remaining()).apply(it::get) }
}
