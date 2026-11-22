package com.example.game.engine

import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GpuMesh(meshData: MeshData) {
    val vaoId: Int
    private val vboId: Int
    private val iboId: Int
    val indexCount: Int = meshData.indices.size

    init {
        val vaos = IntArray(1)
        val bufs = IntArray(2)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(2, bufs, 0)
        vaoId = vaos[0]
        vboId = bufs[0]
        iboId = bufs[1]

        GLES30.glBindVertexArray(vaoId)

        val vBuf: FloatBuffer = ByteBuffer.allocateDirect(meshData.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(meshData.vertices); position(0) }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            meshData.vertices.size * 4,
            vBuf,
            GLES30.GL_STATIC_DRAW
        )

        val iBuf: IntBuffer = ByteBuffer.allocateDirect(meshData.indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .apply { put(meshData.indices); position(0) }

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, iboId)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            meshData.indices.size * 4,
            iBuf,
            GLES30.GL_STATIC_DRAW
        )

        // 12 floats per vertex: pos(3), normal(3), uv(2), color(3), matId(1)
        val stride = 12 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, stride, 8 * 4)
        GLES30.glEnableVertexAttribArray(4)
        GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, stride, 11 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
    }
}

object GlShaders {

    fun compileProgram(vertSrc: String, fragSrc: String): Int {
        val v = compileShader(GLES30.GL_VERTEX_SHADER, vertSrc)
        val f = compileShader(GLES30.GL_FRAGMENT_SHADER, fragSrc)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, v)
        GLES30.glAttachShader(prog, f)
        GLES30.glLinkProgram(prog)
        val status = IntArray(1)
        GLES30.glGetProgramiv(prog, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("VallartaGL", "Link error: ${GLES30.glGetProgramInfoLog(prog)}")
        }
        GLES30.glDeleteShader(v)
        GLES30.glDeleteShader(f)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src)
        GLES30.glCompileShader(s)
        val status = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e("VallartaGL", "Shader compile error: ${GLES30.glGetShaderInfoLog(s)}")
        }
        return s
    }

    // ==================== 1. PBR-INSPIRED WORLD ARCHITECTURE / TERRAIN / VEHICLES / HUMANOIDS SHADER ====================
    const val WORLD_VERT = """#version 300 es
precision highp float;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in vec3 aColor;
layout(location = 4) in float aMatId;

uniform mat4 uMVP;
uniform mat4 uModel;
uniform vec3 uTint;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;
out vec3 vColor;
out float vMatId;

void main() {
    vec4 wp = uModel * vec4(aPos, 1.0);
    vWorldPos = wp.xyz;
    vNormal = normalize(mat3(uModel) * aNormal);
    vUv = aUv;
    vColor = aColor * uTint;
    vMatId = aMatId;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

    const val WORLD_FRAG = """#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;
in vec3 vColor;
in float vMatId;

uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyTopColor;
uniform vec3 uSkyHorizonColor;
uniform float uNightFactor;
uniform float uTime;

uniform sampler2D uTexRoad;
uniform sampler2D uTexMalecon;
uniform sampler2D uTexResort;
uniform sampler2D uTexCondo;
uniform sampler2D uTexColonial;
uniform sampler2D uTexTerrain;
uniform sampler2D uTexEmpedrado;
uniform sampler2D uTexSand;
uniform sampler2D uTexGrass;

out vec4 fragColor;

vec3 acesToneMap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 L = normalize(uSunDir);
    vec3 H = normalize(L + V);

    int mat = int(vMatId + 0.5);
    vec3 baseCol = vColor;
    float specPower = 28.0;
    float specStrength = 0.14;
    float fresnelStrength = 0.08;
    float emissiveBoost = 0.0;

    if (mat == 0) {
        // Organic matte fabric, skin, foliage & architectural plaster with soft micro-sheen & wrap scattering
        float micro = sin(vWorldPos.x * 22.0 + vWorldPos.z * 19.0) * cos(vWorldPos.y * 24.0) * 0.025;
        baseCol *= (1.0 + micro);
        specPower = 34.0;
        specStrength = 0.16;
        fresnelStrength = 0.14;
    } else if (mat == 1) {
        // Clearcoat automotive metallic paint with sharp dual-lobe specular + horizon reflection
        specPower = 136.0;
        specStrength = 1.28;
        fresnelStrength = 0.52;
    } else if (mat == 2) {
        // Metallic chrome / alloy rims / bronze Malecón sculptures
        specPower = 168.0;
        specStrength = 1.48;
        fresnelStrength = 0.76;
    } else if (mat == 3) {
        // Tinted automotive & hotel curtain-wall glass catching tropical sky & sunset
        specPower = 180.0;
        specStrength = 1.42;
        fresnelStrength = 0.86;
    } else if (mat == 4) {
        // Emissive headlights, taillights, Muelle glowing ribs, hotel accent lighting
        emissiveBoost = 1.0;
    } else if (mat == 5) {
        // Crisp Puerto Vallarta coastal asphalt with fine aggregate micro-grain & subtle wet sheen
        vec3 roadNear = texture(uTexRoad, vWorldPos.xz * 0.13).rgb;
        vec3 roadFar = texture(uTexRoad, vWorldPos.xz * 0.028).rgb;
        vec3 roadTex = mix(roadNear, roadFar, 0.28);
        baseCol = roadTex * (vColor * 3.6);
        specPower = 72.0;
        specStrength = 0.30 + uNightFactor * 0.32;
        fresnelStrength = 0.16;
    } else if (mat == 6) {
        // Beachfront Luxury Resort Facade — rich architectural stucco micro-bump + reflective balcony glazing!
        vec2 fUv = vec2((abs(N.x) > abs(N.z) ? vWorldPos.z : vWorldPos.x) * 0.068, -vWorldPos.y * 0.068);
        vec3 tex = texture(uTexResort, fUv).rgb;
        float stuccoBump = (sin(vWorldPos.x * 7.5 + vWorldPos.z * 6.8) * cos(vWorldPos.y * 9.2)) * 0.045;
        N = normalize(N + vec3(stuccoBump, stuccoBump * 0.5, -stuccoBump));
        baseCol = tex * mix(vec3(1.0), vColor, 0.78);
        float isGlass = smoothstep(0.54, 0.20, dot(tex, vec3(0.33)));
        specPower = 96.0;
        specStrength = 0.20 + isGlass * 0.92;
        fresnelStrength = 0.12 + isGlass * 0.48;
        if (uNightFactor > 0.2 && tex.r > tex.b + 0.12 && tex.r > 0.60) {
            emissiveBoost = uNightFactor * 0.76;
        }
    } else if (mat == 7) {
        // Authentic Malecón pebble mosaic & Mexican cantera sandstone promenade
        vec3 stone = texture(uTexMalecon, vWorldPos.xz * 0.16).rgb;
        float relief = (dot(stone, vec3(0.333)) - 0.5) * 0.14;
        N = normalize(N + vec3(relief, 0.0, relief * 0.7));
        baseCol = stone * mix(vec3(1.0), vColor, 0.32);
        specPower = 48.0;
        specStrength = 0.24;
    } else if (mat == 8) {
        // Pure-3D Tri-Biome PBR Terrain: Anti-Tiling Golden Wind-Rippled Sand + Lush Sierra Madre Grass + Coastal Rock Cliffs!
        float greenMask = smoothstep(0.03, 0.18, vColor.g - max(vColor.r, vColor.b));
        float sandMask = smoothstep(0.68, 0.84, vColor.r) * smoothstep(0.58, 0.74, vColor.g) * (1.0 - greenMask);
        float wetSand = smoothstep(0.75, 0.60, vColor.r) * smoothstep(0.66, 0.56, vColor.g) * (1.0 - greenMask) * smoothstep(0.45, 0.68, vColor.r);
        float slopeRock = smoothstep(0.92, 0.66, N.y) * smoothstep(4.5, 16.0, vWorldPos.y);

        mat2 rotUv = mat2(0.80, -0.60, 0.60, 0.80);
        vec2 uvFar = rotUv * vWorldPos.xz;

        vec3 sandNear = texture(uTexSand, vWorldPos.xz * 0.085).rgb;
        vec3 sandMid = texture(uTexSand, uvFar * 0.029).rgb;
        vec3 sandFar = texture(uTexSand, vWorldPos.xz * 0.008).rgb;
        vec3 sandBase = mix(mix(sandNear, sandMid, 0.42), sandFar, 0.22);
        float duneWave = sin(vWorldPos.x * 1.45 + sin(vWorldPos.z * 0.42) * 1.8) * 0.055 * sandMask;
        N = normalize(N + vec3(duneWave, 0.0, duneWave * 0.45));
        vec3 sandCol = sandBase * mix(vec3(1.0), vColor * 1.07, 0.46);
        sandCol = mix(sandCol, sandCol * vec3(0.74, 0.70, 0.64), wetSand * 0.75);

        vec3 grassNear = texture(uTexGrass, vWorldPos.xz * 0.095).rgb;
        vec3 grassFar = texture(uTexGrass, uvFar * 0.019).rgb;
        vec3 grassCol = mix(grassNear, grassFar, 0.45) * (0.76 + vColor * 0.82);

        vec3 earthNear = texture(uTexTerrain, vWorldPos.xz * 0.09).rgb;
        vec3 earthFar = texture(uTexTerrain, uvFar * 0.017).rgb;
        float eGrain = dot(mix(earthNear, earthFar, 0.42), vec3(0.333));
        vec3 urbanCol = vColor * (0.80 + 0.36 * eGrain);
        vec3 cliffCol = vec3(0.42, 0.37, 0.32) * (0.72 + 0.52 * eGrain);

        baseCol = mix(urbanCol, sandCol, sandMask);
        baseCol = mix(baseCol, grassCol, greenMask);
        baseCol = mix(baseCol, cliffCol, slopeRock * 0.78);

        float qHash = fract(sin(dot(floor(vWorldPos.xz * 9.0), vec2(127.1, 311.7))) * 43758.5453);
        float sparkle = smoothstep(0.965, 1.0, qHash) * sandMask * (1.0 - wetSand) * 0.55;
        specPower = mix(mix(24.0, 64.0, sandMask), 165.0, wetSand);
        specStrength = mix(mix(0.08, 0.22 + sparkle, sandMask), 1.05, wetSand);
        fresnelStrength = mix(0.06, 0.56, wetSand);
    } else if (mat == 9) {
        // Modern Oceanfront Glass Condo Skyscraper (Peninsula, Grand Venetian, Icon, Mousai)
        vec2 fUv = vec2((abs(N.x) > abs(N.z) ? vWorldPos.z : vWorldPos.x) * 0.058, -vWorldPos.y * 0.058);
        vec3 tex = texture(uTexCondo, fUv).rgb;
        baseCol = tex * mix(vec3(1.0), vColor, 0.56);
        float isGlass = smoothstep(0.66, 0.32, dot(tex, vec3(0.33)));
        specPower = 140.0;
        specStrength = 0.32 + isGlass * 1.24;
        fresnelStrength = 0.18 + isGlass * 0.72;
        if (uNightFactor > 0.2 && tex.r > 0.54 && tex.g > 0.46 && tex.b < 0.48) {
            emissiveBoost = uNightFactor * 0.84;
        }
    } else if (mat == 10) {
        // Authentic Puerto Vallarta Colonial Stucco + Shuttered Arches — hand-troweled plaster micro-relief!
        vec2 fUv = vec2((abs(N.x) > abs(N.z) ? vWorldPos.z : vWorldPos.x) * 0.082, -vWorldPos.y * 0.090);
        vec3 tex = texture(uTexColonial, fUv).rgb;
        float plaster = sin(vWorldPos.x * 10.0 + vWorldPos.z * 9.0) * cos(vWorldPos.y * 11.0) * 0.055;
        N = normalize(N + vec3(plaster, plaster * 0.4, -plaster));
        baseCol = tex * mix(vec3(1.0), vColor, 0.78);
        specPower = 26.0;
        specStrength = 0.14;
        if (uNightFactor > 0.25 && tex.r > 0.60 && tex.g > 0.45 && tex.b < 0.36) {
            emissiveBoost = uNightFactor * 0.68;
        }
    } else if (mat == 11) {
        // Shimmering Resort Beachfront Infinity Pool Water with animated caustics!
        float c1 = sin(vWorldPos.x * 1.8 + uTime * 2.6 + sin(vWorldPos.z * 1.4));
        float c2 = cos(vWorldPos.z * 2.1 - uTime * 2.2 + cos(vWorldPos.x * 1.6));
        float caustic = smoothstep(0.35, 0.95, (c1 * c2) * 0.5 + 0.5);
        baseCol = mix(vec3(0.04, 0.62, 0.74), vec3(0.45, 0.94, 0.98), caustic * 0.55);
        specPower = 156.0;
        specStrength = 1.50;
        fresnelStrength = 0.72;
        emissiveBoost = uNightFactor * 0.45;
    } else if (mat == 12) {
        // Authentic Puerto Vallarta "Empedrado" rounded river-stone cobblestones with 3D stone normal bump!
        vec3 cobble = texture(uTexEmpedrado, vWorldPos.xz * 0.30).rgb;
        float stonePeak = smoothstep(0.34, 0.68, dot(cobble, vec3(0.333)));
        float bump = (stonePeak - 0.5) * 0.22;
        N = normalize(N + vec3(bump, 0.0, bump * 0.8));
        baseCol = cobble * mix(vec3(1.0), vColor, 0.22);
        specPower = 58.0;
        specStrength = 0.16 + stonePeak * (0.32 + uNightFactor * 0.24);
        fresnelStrength = 0.12 + stonePeak * 0.14;
    }

    vec3 finalCol;
    if (emissiveBoost > 0.94) {
        finalCol = baseCol * 1.25;
    } else {
        float NdotL = max(dot(N, L), 0.0);
        // Soft wrap diffuse + warm subsurface bounce eliminates harsh faceted polygon edges on characters & foliage!
        float wrapDiff = max((dot(N, L) + 0.42) / 1.42, 0.0);
        float hemi = N.y * 0.5 + 0.5;
        vec3 groundBounce = mix(vec3(0.32, 0.26, 0.20), vec3(0.09, 0.11, 0.16), uNightFactor);
        vec3 skyAmb = mix(uSkyHorizonColor * 0.58, uSkyTopColor * 0.68, hemi);
        vec3 ambient = mix(groundBounce, skyAmb, clamp(hemi * 1.12, 0.0, 1.0));
        ambient = max(ambient, vec3(0.15, 0.18, 0.25));

        float groundAo = mix(0.82, 1.0, smoothstep(1.6, 5.4, vWorldPos.y));
        float wrapWeight = (mat == 0) ? 0.52 : 0.30;
        vec3 diffuse = uSunColor * mix(NdotL, wrapDiff, wrapWeight) * (1.16 - uNightFactor * 0.60);
        float spec = pow(max(dot(N, H), 0.0), specPower) * specStrength;

        vec3 R = reflect(-V, N);
        float reflH = clamp(R.y * 0.5 + 0.5, 0.0, 1.0);
        vec3 envRefl = mix(uSkyHorizonColor, uSkyTopColor, smoothstep(0.32, 0.85, reflH));
        float sunRefl = pow(max(dot(R, L), 0.0), 38.0) * 0.56;
        envRefl += uSunColor * sunRefl * (1.0 - uNightFactor * 0.6);
        float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.2) * fresnelStrength;

        // Subtle sunlit tropical rim light for crisp 3D depth separation on characters, cars & resorts
        float rim = pow(1.0 - max(dot(N, V), 0.0), 3.8) * (0.18 - uNightFactor * 0.10);
        vec3 rimCol = mix(uSkyHorizonColor, uSunColor, 0.45) * rim;

        vec3 lit = (baseCol * (ambient + diffuse) * groundAo) + uSunColor * spec + envRefl * fresnel + rimCol;
        finalCol = mix(lit, baseCol * 1.18, emissiveBoost);
    }

    finalCol = acesToneMap(finalCol * 1.10);

    float dist = length(uCameraPos - vWorldPos);
    float fogFactor = clamp((dist - 240.0) / 860.0, 0.0, 0.86);
    vec3 fogCol = acesToneMap(uSkyHorizonColor * 1.04);
    finalCol = mix(finalCol, fogCol, fogFactor * fogFactor);

    fragColor = vec4(finalCol, 1.0);
}
"""

    // ==================== 3. AAA ORGANIC GERSTNER + FBM PACIFIC OCEAN SHADER (BAHÍA DE BANDERAS!) ====================
    const val OCEAN_VERT = """#version 300 es
precision highp float;

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in vec3 aColor;
layout(location = 4) in float aMatId;

uniform mat4 uMVP;
uniform float uTime;

out vec3 vWorldPos;
out vec3 vWaveNormal;
out float vShoreDist;
out float vCrestHeight;

void addGerstner(
    vec2 xz, vec2 dir, float steepness, float wavelength, float speed, float shoreScale,
    inout vec3 disp, inout vec3 tangent, inout vec3 binormal
) {
    float k = 6.2831853 / wavelength;
    float a = (steepness / k) * shoreScale;
    float f = k * (dot(dir, xz) - speed * uTime);
    float cf = cos(f);
    float sf = sin(f);

    disp.x += dir.x * (a * cf);
    disp.y += a * sf;
    disp.z += dir.y * (a * cf);

    float wa = k * a;
    tangent += vec3(
        -dir.x * dir.x * (wa * sf),
        dir.x * (wa * cf),
        -dir.x * dir.y * (wa * sf)
    );
    binormal += vec3(
        -dir.x * dir.y * (wa * sf),
        dir.y * (wa * cf),
        -dir.y * dir.y * (wa * sf)
    );
}

void main() {
    float shore = aColor.r; // 0.0 at shoreline -> 1.0 deep bay
    float waveAmp = 0.16 + 0.84 * smoothstep(0.0, 0.24, shore);

    vec3 disp = vec3(0.0);
    vec3 tangent = vec3(1.0, 0.0, 0.0);
    vec3 binormal = vec3(0.0, 0.0, 1.0);

    // 4 smooth rolling Pacific swells arriving from the West-Southwest into Banderas Bay
    addGerstner(aPos.xz, normalize(vec2(0.92, 0.38)),  0.18, 52.0, 4.8, waveAmp, disp, tangent, binormal);
    addGerstner(aPos.xz, normalize(vec2(0.76, -0.65)), 0.14, 31.0, 3.8, waveAmp, disp, tangent, binormal);
    addGerstner(aPos.xz, normalize(vec2(0.96, -0.26)), 0.11, 17.5, 3.1, waveAmp, disp, tangent, binormal);
    addGerstner(aPos.xz, normalize(vec2(0.45, 0.89)),  0.08, 10.5, 2.4, waveAmp, disp, tangent, binormal);

    vec3 p = aPos + disp;
    vWorldPos = p;
    vWaveNormal = normalize(cross(binormal, tangent));
    vShoreDist = shore;
    vCrestHeight = disp.y;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""

    const val OCEAN_FRAG = """#version 300 es
precision highp float;

in vec3 vWorldPos;
in vec3 vWaveNormal;
in float vShoreDist;
in float vCrestHeight;

uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyTopColor;
uniform vec3 uSkyHorizonColor;
uniform float uTime;
uniform float uNightFactor;

out vec4 fragColor;

vec3 acesToneMap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Organic 4-octave rotated FBM ocean heightfield — ZERO plaid/grid artifacts!
float oceanFbm(vec2 p) {
    const mat2 m = mat2(0.80, -0.60, 0.60, 0.80);
    float v = 0.0;
    float amp = 0.50;
    v += amp * vnoise(p); p = m * p * 2.04 + vec2(1.7, 9.2); amp *= 0.50;
    v += amp * vnoise(p); p = m * p * 2.07 + vec2(8.3, 2.8); amp *= 0.50;
    v += amp * vnoise(p); p = m * p * 2.03 + vec2(3.1, 5.4); amp *= 0.50;
    v += amp * vnoise(p);
    return v;
}

float waveHeight(vec2 xz) {
    vec2 p1 = xz * 0.14 + vec2(-uTime * 0.55, uTime * 0.22);
    vec2 p2 = xz * 0.31 + vec2(uTime * 0.38, -uTime * 0.62);
    return oceanFbm(p1) * 0.62 + oceanFbm(p2) * 0.38;
}

void main() {
    float camDist = length(uCameraPos - vWorldPos);
    // Distance-attenuated organic finite-difference wave normal (crisp near shore, silky smooth on horizon!)
    float detailFade = 1.0 - smoothstep(140.0, 720.0, camDist);
    float eps = 0.35;
    float hC = waveHeight(vWorldPos.xz);
    float hX = waveHeight(vWorldPos.xz + vec2(eps, 0.0));
    float hZ = waveHeight(vWorldPos.xz + vec2(0.0, eps));
    vec3 microN = vec3((hC - hX) * 0.72 * detailFade, 0.0, (hC - hZ) * 0.72 * detailFade);
    vec3 N = normalize(vWaveNormal + microN);

    vec3 V = normalize(uCameraPos - vWorldPos);
    vec3 L = normalize(uSunDir);
    vec3 H = normalize(L + V);

    // Photorealistic Beer-Lambert Bahía de Banderas water absorption:
    // sunlit crystal turquoise-aqua shallows -> rich Caribbean-Pacific emerald -> deep cobalt blue bay
    vec3 crystalShallow = mix(vec3(0.11, 0.74, 0.72), vec3(0.02, 0.18, 0.26), uNightFactor);
    vec3 midEmerald = mix(vec3(0.02, 0.40, 0.56), vec3(0.01, 0.11, 0.22), uNightFactor);
    vec3 deepPacific = mix(vec3(0.01, 0.13, 0.34), vec3(0.006, 0.035, 0.11), uNightFactor);

    float shallowBlend = smoothstep(0.0, 0.09, vShoreDist);
    float deepBlend = smoothstep(0.07, 0.44, vShoreDist);
    vec3 waterCol = mix(crystalShallow, midEmerald, shallowBlend);
    waterCol = mix(waterCol, deepPacific, deepBlend);

    // Sunlit shallow turquoise caustics shimmer near the beach
    float caustic = smoothstep(0.45, 0.85, hC) * (1.0 - shallowBlend) * (1.0 - uNightFactor * 0.8);
    waterCol += vec3(0.10, 0.18, 0.14) * caustic;

    // Physical Subsurface Scattering (SSS) glowing emerald light through backlit ocean swells
    float backLight = pow(clamp(dot(V, -L + N * 0.45), 0.0, 1.0), 3.0);
    float crestMask = smoothstep(0.06, 0.58, vCrestHeight + (hC - 0.5) * 0.25);
    vec3 sssCol = vec3(0.06, 0.78, 0.74) * (backLight * 0.48 + crestMask * 0.28) * (1.0 - uNightFactor * 0.78);
    waterCol += sssCol;

    // Realistic Schlick Fresnel sky & sunset horizon reflection
    float cosTheta = max(dot(N, V), 0.0);
    float fresnel = 0.025 + 0.975 * pow(1.0 - cosTheta, 4.6);
    vec3 R = reflect(-V, N);
    vec3 skyRefl = mix(uSkyHorizonColor, uSkyTopColor, smoothstep(0.0, 0.58, clamp(R.y, 0.0, 1.0)));
    vec3 col = mix(waterCol, skyRefl, clamp(fresnel * 0.84, 0.0, 0.86));

    // Silky GGX-style solar glitter path dancing across organic micro-waves
    float NdotH = max(dot(N, H), 0.0);
    float sunGlintTight = pow(NdotH, 420.0) * (3.6 - uNightFactor * 2.0);
    float sunGlintMid = pow(NdotH, 68.0) * 0.42 * (1.0 - uNightFactor * 0.7);
    col += uSunColor * (sunGlintTight + sunGlintMid);

    // Natural organic FBM shoreline whitewash & cellular sea-foam lace (ZERO zebra stripes!)
    float foamNoise = oceanFbm(vWorldPos.xz * 0.42 + vec2(-uTime * 0.45, uTime * 0.18));
    float fineLace = vnoise(vWorldPos.xz * 1.85 - vec2(uTime * 0.7, -uTime * 0.4));
    float organicWave = sin(vShoreDist * 62.0 - uTime * 2.4 + foamNoise * 5.2);
    float breakingSurf = smoothstep(0.055, 0.004, vShoreDist) * smoothstep(0.28, 0.82, organicWave * 0.55 + foamNoise * 0.65);
    float shoreWash = smoothstep(0.016, 0.0, vShoreDist) * smoothstep(0.22, 0.68, foamNoise * 0.7 + fineLace * 0.35);
    float openWhitecap = smoothstep(0.62, 0.92, vCrestHeight * 0.7 + foamNoise * 0.55) * smoothstep(0.04, 0.25, vShoreDist) * 0.38;
    float foam = clamp(breakingSurf * 0.78 + shoreWash * 0.94 + openWhitecap, 0.0, 0.95);
    vec3 foamCol = mix(vec3(0.95, 0.97, 0.99), uSkyHorizonColor * 0.96, 0.16) * (1.0 - uNightFactor * 0.48);
    col = mix(col, foamCol, foam);

    col = acesToneMap(col * 1.08);

    float fogFactor = clamp((camDist - 260.0) / 920.0, 0.0, 0.86);
    col = mix(col, acesToneMap(uSkyHorizonColor * 1.04), fogFactor * fogFactor);

    fragColor = vec4(col, 1.0);
}
"""

    // ==================== 4. TROPICAL ATMOSPHERIC SKY DOME SHADER ====================
    const val SKY_VERT = """#version 300 es
precision highp float;

layout(location = 0) in vec2 aPos;
uniform mat4 uInvViewProj;
out vec3 vRayDir;

void main() {
    vec4 farPt = uInvViewProj * vec4(aPos, 1.0, 1.0);
    vec4 nearPt = uInvViewProj * vec4(aPos, -1.0, 1.0);
    vRayDir = normalize(farPt.xyz / farPt.w - nearPt.xyz / nearPt.w);
    gl_Position = vec4(aPos, 0.9999, 1.0);
}
"""

    const val SKY_FRAG = """#version 300 es
precision highp float;

in vec3 vRayDir;

uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyTopColor;
uniform vec3 uSkyMidColor;
uniform vec3 uSkyHorizonColor;
uniform float uNightFactor;
uniform float uTime;

out vec4 fragColor;

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec3 acesToneMap(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec3 dir = normalize(vRayDir);
    float h = clamp(dir.y, 0.0, 1.0);

    vec3 sky = mix(uSkyHorizonColor, uSkyMidColor, smoothstep(0.0, 0.25, h));
    sky = mix(sky, uSkyTopColor, smoothstep(0.20, 0.78, h));

    float sunDot = max(dot(dir, normalize(uSunDir)), 0.0);
    float sunDisc = smoothstep(0.9985, 0.9994, sunDot);
    float sunHalo = pow(sunDot, 12.0) * 0.52 + pow(sunDot, 58.0) * 0.65;
    sky += uSunColor * (sunHalo * (1.0 - uNightFactor * 0.55) + sunDisc * 1.6);

    if (dir.y > 0.04) {
        vec2 uv = dir.xz / (dir.y + 0.16) * 1.9 + vec2(uTime * 0.006, -uTime * 0.003);
        float c1 = sin(uv.x * 2.1 + cos(uv.y * 1.7)) * cos(uv.y * 2.3 - sin(uv.x * 1.3));
        float cloud = smoothstep(0.32, 0.86, c1 * 0.5 + 0.5) * smoothstep(0.04, 0.22, dir.y) * smoothstep(0.85, 0.35, dir.y);
        vec3 cloudCol = mix(uSkyHorizonColor * 1.15, vec3(1.0, 0.95, 0.90), 0.38) * (1.0 - uNightFactor * 0.65);
        sky = mix(sky, cloudCol, cloud * 0.42);
    }

    if (uNightFactor > 0.35 && dir.y > 0.08) {
        vec2 starGrid = floor(dir.xz / (dir.y + 0.25) * 95.0);
        float s = hash2(starGrid);
        if (s > 0.992) {
            float twinkle = 0.65 + 0.35 * sin(uTime * 3.5 + s * 60.0);
            sky += vec3(0.85, 0.92, 1.0) * uNightFactor * twinkle * smoothstep(0.08, 0.30, dir.y);
        }
    }

    fragColor = vec4(acesToneMap(sky * 1.05), 1.0);
}
"""
}
