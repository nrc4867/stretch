package dev.chieppa.stretch.gallery.gpu

/** A unit cube centred on the origin: four vertices per face, each a position and the face's normal. */
internal val CUBE_VERTICES: FloatArray = run {
    // Each face: its normal, then two axes across it.
    val faces = listOf(
        floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f),
        floatArrayOf(-1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 0f, -1f, 1f, 0f, 0f),
        floatArrayOf(0f, -1f, 0f, 0f, 0f, 1f, 1f, 0f, 0f),
        floatArrayOf(0f, 0f, 1f, 0f, 1f, 0f, 1f, 0f, 0f),
        floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f, -1f, 0f, 0f),
    )
    val corners = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f)
    faces.flatMap { f ->
        corners.flatMap { (u, v) ->
            // A corner is the normal plus u along the face's second axis and v along its first.
            val x = (f[0] + u * f[6] + v * f[3]) * 0.5f
            val y = (f[1] + u * f[7] + v * f[4]) * 0.5f
            val z = (f[2] + u * f[8] + v * f[5]) * 0.5f
            listOf(x, y, z, f[0], f[1], f[2])
        }
    }.toFloatArray()
}

/** Two counter-clockwise triangles per face of [CUBE_VERTICES]. */
internal val CUBE_INDICES: IntArray = IntArray(36) { i -> (i / 6) * 4 + intArrayOf(0, 1, 2, 0, 2, 3)[i % 6] }

internal const val CUBE_WGSL = """
struct Frame {
    viewProj: mat4x4<f32>,
    model: mat4x4<f32>,
    light: vec4<f32>,
    base: vec4<f32>,
    shade: vec4<f32>,
    params: vec4<f32>,
};

@group(0) @binding(0) var<uniform> frame: Frame;

struct VertexOut {
    @builtin(position) clip: vec4<f32>,
    @location(0) normal: vec3<f32>,
};

@vertex
fn vs_main(@location(0) position: vec3<f32>, @location(1) normal: vec3<f32>) -> VertexOut {
    var out: VertexOut;
    out.clip = frame.viewProj * frame.model * vec4<f32>(position, 1.0);
    out.normal = (frame.model * vec4<f32>(normal, 0.0)).xyz;
    return out;
}

@fragment
fn fs_main(in: VertexOut) -> @location(0) vec4<f32> {
    if (frame.params.x < 0.5) {
        return vec4<f32>(frame.base.rgb, 1.0);
    }
    let light = dot(normalize(in.normal), normalize(frame.light.xyz));
    if (light > 0.15) {
        return vec4<f32>(frame.base.rgb, 1.0);
    }
    return vec4<f32>(frame.shade.rgb, 1.0);
}
"""
