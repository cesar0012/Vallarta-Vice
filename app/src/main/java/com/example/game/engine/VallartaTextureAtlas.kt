package com.example.game.engine

import android.graphics.*
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates high-resolution, mipmapped photorealistic 2D OpenGL ES 3.0 textures at startup:
 *  - Weathered Puerto Vallarta Coastal Asphalt with aggregate & tire wear tracks
 *  - Authentic Malecón Pebble Mosaic & Mexican Cantera Stone Promenade
 *  - Beachfront Luxury Resort Facade (stucco, recessed balconies, sliding glass & warm drapes)
 *  - Modern Glass Condo Skyscraper Facade (Peninsula / Grand Venetian / Mousai)
 *  - Authentic Puerto Vallarta Colonial Stucco + Shuttered Arches + Terracotta Trim
 *  - Tropical Beach Sand & Lush Sierra Madre Jungle Canopy texture
 *  - Crisp Illuminated 3D Hotel & Landmark Signboard Texture Atlas (32 real PV places!)
 */
class VallartaTextureSet {

    val texRoad: Int
    val texMalecon: Int
    val texResort: Int
    val texCondo: Int
    val texColonial: Int
    val texTerrain: Int
    val texEmpedrado: Int
    val texSand: Int
    val texGrass: Int
    val texSigns: Int

    /**
     * Maps building IDs -> slot index (0..31) in the 4x8 Hotel Signboard Atlas!
     */
    val signSlots: Map<String, Int>

    init {
        texRoad = uploadTexture(createAsphaltBitmap())
        texMalecon = uploadTexture(createMaleconMosaicBitmap())
        texResort = uploadTexture(createResortFacadeBitmap())
        texCondo = uploadTexture(createGlassCondoBitmap())
        texColonial = uploadTexture(createColonialFacadeBitmap())
        texTerrain = uploadTexture(createTerrainBitmap())
        texEmpedrado = uploadTexture(createVallartaEmpedradoBitmap())
        texSand = uploadTexture(createBeachSandBitmap())
        texGrass = uploadTexture(createTropicalGrassBitmap())

        val (signBmp, slotMap) = createHotelSignAtlas()
        texSigns = uploadTexture(signBmp, repeat = false)
        signSlots = slotMap
    }

    fun uvBoundsForSignSlot(slot: Int): FloatArray {
        val cols = 4
        val rows = 8
        val col = slot % cols
        val row = slot / cols
        val u0 = col.toFloat() / cols + 0.004f
        val u1 = (col + 1).toFloat() / cols - 0.004f
        val v0 = row.toFloat() / rows + 0.004f
        val v1 = (row + 1).toFloat() / rows - 0.004f
        return floatArrayOf(u0, v0, u1, v1)
    }

    private fun uploadTexture(bitmap: Bitmap, repeat: Boolean = true): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val id = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        val wrap = if (repeat) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        bitmap.recycle()
        return id
    }

    private fun createAsphaltBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(36, 39, 46))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(42)

        // Realistic worn dark tire tracks along lanes
        p.color = Color.argb(46, 16, 18, 22)
        for (x in intArrayOf(95, 165, 345, 415)) {
            c.drawRect(x - 20f, 0f, x + 20f, s.toFloat(), p)
        }
        // High-contrast coastal volcanic mineral aggregate speckles
        for (i in 0 until 18000) {
            val v = rnd.nextInt(26, 78)
            p.color = Color.rgb(v, v + 2, v + 5)
            val px = rnd.nextFloat() * s
            val py = rnd.nextFloat() * s
            val r = rnd.nextFloat() * 2.0f + 0.6f
            c.drawRect(px, py, px + r, py + r, p)
        }
        // Subtle sunlit tar repair seams
        p.color = Color.argb(90, 18, 20, 24)
        p.strokeWidth = 2.0f
        for (k in 0 until 12) {
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            c.drawLine(x, y, x + rnd.nextFloat() * 52f - 26f, y + rnd.nextFloat() * 82f, p)
        }
        return bmp
    }

    private fun createMaleconMosaicBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(212, 198, 174)) // Warm Mexican cantera sandstone base
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(101)

        // Warm stone tile grid with alternating Huichol pebble-mosaic medallion bands!
        val tile = 64
        for (gy in 0 until s step tile) {
            for (gx in 0 until s step tile) {
                val shift = rnd.nextInt(-10, 11)
                val warm = ((gx / tile + gy / tile) % 2 == 0)
                p.color = if (warm) {
                    Color.rgb((218 + shift).coerceIn(0, 255), (199 + shift).coerceIn(0, 255), (170 + shift).coerceIn(0, 255))
                } else {
                    Color.rgb((196 + shift).coerceIn(0, 255), (176 + shift).coerceIn(0, 255), (150 + shift).coerceIn(0, 255))
                }
                c.drawRect(gx + 2f, gy + 2f, gx + tile - 2f, gy + tile - 2f, p)

                // Iconic Puerto Vallarta Malecón geometric river-stone inlay accents!
                if ((gx / tile) % 2 == 0 && (gy / tile) % 2 == 0) {
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = 3f
                    p.color = Color.rgb(112, 96, 80)
                    c.drawCircle(gx + tile * 0.5f, gy + tile * 0.5f, tile * 0.28f, p)
                    p.style = Paint.Style.FILL
                    p.color = Color.rgb(168, 92, 64)
                    c.drawCircle(gx + tile * 0.5f, gy + tile * 0.5f, tile * 0.10f, p)
                }
            }
        }
        // Subtle stone speckle
        for (i in 0 until 6000) {
            val shade = rnd.nextInt(140, 225)
            p.color = Color.argb(42, shade, shade - 8, shade - 18)
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            c.drawRect(x, y, x + 2f, y + 2f, p)
        }
        return bmp
    }

    /**
     * Beachfront Puerto Vallarta Luxury Resort Facade (Marriott, Sheraton, Fiesta Americana, Barceló, Krystal):
     * Warm coastal stucco, recessed shadowy open-air balconies, glass terrace doors, white rails & subtle plants!
     */
    private fun createResortFacadeBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(236, 224, 206))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(7)

        // Subtle warm plaster noise
        for (i in 0 until 5000) {
            val v = rnd.nextInt(212, 245)
            p.color = Color.argb(45, v, v - 8, v - 18)
            c.drawRect(rnd.nextFloat() * s, rnd.nextFloat() * s, rnd.nextFloat() * s + 3f, rnd.nextFloat() * s + 2f, p)
        }

        val floors = 4
        val bays = 4
        val fH = s / floors.toFloat()
        val bW = s / bays.toFloat()

        for (fl in 0 until floors) {
            val y0 = fl * fH
            // Concrete balcony floor slab overhang + drop shadow
            p.color = Color.rgb(245, 240, 232)
            c.drawRect(0f, y0 + fH - 16f, s.toFloat(), y0 + fH, p)
            p.color = Color.argb(95, 42, 32, 26)
            c.drawRect(0f, y0, s.toFloat(), y0 + 14f, p)

            for (b in 0 until bays) {
                val x0 = b * bW + 10f
                val x1 = (b + 1) * bW - 10f
                val wy0 = y0 + 18f
                val wy1 = y0 + fH - 20f

                // Recessed shaded balcony cavity
                p.color = Color.rgb(72, 62, 58)
                c.drawRect(x0, wy0, x1, wy1, p)

                // Reflective glass patio sliding doors (some warm sunlit interior curtains!)
                val litRoom = rnd.nextFloat() > 0.55f
                val grad = LinearGradient(
                    x0, wy0, x1, wy1,
                    if (litRoom) Color.rgb(220, 186, 132) else Color.rgb(42, 86, 118),
                    if (litRoom) Color.rgb(122, 92, 68) else Color.rgb(22, 48, 72),
                    Shader.TileMode.CLAMP
                )
                p.shader = grad
                c.drawRect(x0 + 8f, wy0 + 6f, x1 - 8f, wy1 - 4f, p)
                p.shader = null

                // Sliding door mullion center bar
                p.color = Color.rgb(220, 218, 214)
                c.drawRect((x0 + x1) * 0.5f - 2f, wy0 + 6f, (x0 + x1) * 0.5f + 2f, wy1 - 4f, p)

                // Balcony glass & white railing along lower third
                val railTop = wy1 - (wy1 - wy0) * 0.36f
                p.color = Color.argb(120, 160, 215, 235)
                c.drawRect(x0 + 2f, railTop, x1 - 2f, wy1, p)
                p.color = Color.rgb(248, 245, 240)
                c.drawRect(x0, railTop - 3f, x1, railTop + 3f, p)
                // Vertical balusters
                for (rx in 0..4) {
                    val bx = x0 + (x1 - x0) * (rx / 4f)
                    c.drawRect(bx - 1.5f, railTop, bx + 1.5f, wy1, p)
                }

                // Occasional tropical terracotta planter / bougainvillea splash!
                if (rnd.nextFloat() > 0.68f) {
                    p.color = Color.rgb(38, 128, 56)
                    c.drawCircle(x0 + 18f, railTop + 6f, 8f, p)
                    p.color = Color.rgb(228, 42, 112)
                    c.drawCircle(x0 + 21f, railTop + 4f, 4f, p)
                }
            }
        }
        return bmp
    }

    /**
     * Modern Oceanfront Glass Condo Skyscraper (Grand Venetian, Peninsula, Icon Vallarta, Mousai):
     * Sleek reflective ocean-blue architectural glass, cantilevered crisp white concrete decks, dark mullions.
     */
    private fun createGlassCondoBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(24, 46, 68))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(33)

        val floors = 5
        val fH = s / floors.toFloat()
        for (fl in 0 until floors) {
            val y0 = fl * fH
            // Sweeping sky-reflecting ocean glass band
            val glassGrad = LinearGradient(
                0f, y0, s.toFloat(), y0 + fH,
                Color.rgb(48, 124, 166),
                Color.rgb(18, 48, 82),
                Shader.TileMode.CLAMP
            )
            p.shader = glassGrad
            c.drawRect(0f, y0 + 10f, s.toFloat(), y0 + fH - 18f, p)
            p.shader = null

            // Individual luxury apartment panes with warm interior depth hints
            val cols = 6
            val cW = s / cols.toFloat()
            for (col in 0 until cols) {
                val cx0 = col * cW
                if (rnd.nextFloat() > 0.58f) {
                    p.color = Color.argb(95, 255, 212, 146)
                    c.drawRect(cx0 + 6f, y0 + 18f, cx0 + cW - 6f, y0 + fH - 24f, p)
                }
                // Dark aluminum curtain-wall mullion
                p.color = Color.rgb(28, 36, 46)
                c.drawRect(cx0 - 2.5f, y0 + 10f, cx0 + 2.5f, y0 + fH - 18f, p)
            }

            // Crisp cantilevered white travertine balcony slab & tinted glass railing
            p.color = Color.argb(110, 145, 220, 240)
            c.drawRect(0f, y0 + fH - 36f, s.toFloat(), y0 + fH - 18f, p)
            p.color = Color.rgb(244, 246, 248)
            c.drawRect(0f, y0 + fH - 18f, s.toFloat(), y0 + fH, p)
            // Soft underside balcony shadow
            p.color = Color.argb(85, 10, 18, 28)
            c.drawRect(0f, y0, s.toFloat(), y0 + 11f, p)
        }
        return bmp
    }

    /**
     * Authentic Puerto Vallarta Colonial & Zona Romántica Facade (Hotel Rosita, Centro, Olas Altas):
     * Textured lime stucco, arched Mexican colonnade windows, cedar wood shutters, wrought iron railings & terracotta cornice!
     */
    private fun createColonialFacadeBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(242, 232, 216))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(99)

        // Authentic hand-troweled lime stucco texture
        for (i in 0 until 8000) {
            val v = rnd.nextInt(215, 248)
            p.color = Color.argb(42, v, v - 8, v - 18)
            c.drawRect(rnd.nextFloat() * s, rnd.nextFloat() * s, rnd.nextFloat() * s + 3f, rnd.nextFloat() * s + 3f, p)
        }

        // Terracotta stone cornice & dado base band
        p.color = Color.rgb(196, 98, 62)
        c.drawRect(0f, 0f, s.toFloat(), 24f, p)
        c.drawRect(0f, s - 34f, s.toFloat(), s.toFloat(), p)

        // 2x3 Arched Mexican Colonial Windows with wooden shutters & wrought-iron balconies!
        val rows = 2
        val cols = 3
        val rH = (s - 64f) / rows
        val cW = s / cols.toFloat()
        for (r in 0 until rows) {
            for (col in 0 until cols) {
                val cx = col * cW + cW * 0.5f
                val cyTop = 44f + r * rH
                val wW = 62f
                val wH = 128f

                // Whitewashed stone arch frame
                p.color = Color.rgb(250, 246, 238)
                c.drawArc(cx - wW * 0.58f, cyTop, cx + wW * 0.58f, cyTop + wW * 1.1f, 180f, 180f, true, p)
                c.drawRect(cx - wW * 0.58f, cyTop + wW * 0.52f, cx + wW * 0.58f, cyTop + wH + 8f, p)

                // Dark reflective colonial glass interior
                p.color = if (rnd.nextFloat() > 0.5f) Color.rgb(54, 48, 42) else Color.rgb(186, 146, 92)
                c.drawArc(cx - wW * 0.45f, cyTop + 8f, cx + wW * 0.45f, cyTop + wW, 180f, 180f, true, p)
                c.drawRect(cx - wW * 0.45f, cyTop + wW * 0.5f, cx + wW * 0.45f, cyTop + wH, p)

                // Rich dark cedar louvered shutters flanking window
                p.color = Color.rgb(92, 52, 32)
                c.drawRect(cx - wW * 0.85f, cyTop + 26f, cx - wW * 0.50f, cyTop + wH, p)
                c.drawRect(cx + wW * 0.50f, cyTop + 26f, cx + wW * 0.85f, cyTop + wH, p)

                // Ornate black wrought-iron Juliette balcony railing!
                p.color = Color.rgb(28, 28, 32)
                val bTop = cyTop + wH - 36f
                c.drawRect(cx - wW * 0.72f, bTop, cx + wW * 0.72f, bTop + 4f, p)
                c.drawRect(cx - wW * 0.72f, cyTop + wH, cx + wW * 0.72f, cyTop + wH + 5f, p)
                for (bar in -3..3) {
                    val bx = cx + bar * 13f
                    c.drawRect(bx - 1.5f, bTop, bx + 1.5f, cyTop + wH, p)
                }
            }
        }
        return bmp
    }

    private fun createTerrainBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(225, 222, 214))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(77)
        // Rich organic noise (modulated by vertex sand/jungle colors in shader!)
        for (i in 0 until 16000) {
            val v = rnd.nextInt(178, 250)
            p.color = Color.argb(65, v, v, v)
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            val sz = rnd.nextFloat() * 5f + 1.5f
            c.drawCircle(x, y, sz, p)
        }
        // Subtle wind-rippled beach / hillside grain lines
        p.strokeWidth = 2f
        for (y in 0 until s step 12) {
            p.color = Color.argb(26, 150, 145, 130)
            for (x in 0 until s step 24) {
                val dy = sin(x * 0.08f + y * 0.05f) * 3f
                c.drawLine(x.toFloat(), y + dy, x + 22f, y - dy, p)
            }
        }
        return bmp
    }

    /**
     * Authentic Puerto Vallarta "Empedrado" river-stone cobblestone street texture!
     * Rounded coastal river cobbles (basalt gray, warm sandstone, ochre, pale granite) embedded
     * in sun-baked mortar — iconic of Centro, Zona Romántica, 5 de Diciembre & Conchas Chinas!
     */
    private fun createVallartaEmpedradoBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(92, 84, 74)) // Sun-baked sandy mortar joints
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(314)

        // Fine sandy mortar speckle
        for (i in 0 until 7000) {
            val v = rnd.nextInt(68, 114)
            p.color = Color.rgb(v, v - 6, v - 14)
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            c.drawRect(x, y, x + 2.2f, y + 2.2f, p)
        }

        // Dense hand-laid rows of rounded Puerto Vallarta river stones ("piedra bola")
        val rows = 14
        val cols = 12
        val rowH = s / rows.toFloat()
        val colW = s / cols.toFloat()
        val stonePalette = arrayOf(
            intArrayOf(154, 145, 132), // Warm coastal granite
            intArrayOf(132, 126, 118), // River basalt gray
            intArrayOf(172, 152, 128), // Warm Vallarta sandstone
            intArrayOf(118, 112, 108), // Deep volcanic pebble
            intArrayOf(164, 138, 116), // Sun-warmed ochre river rock
            intArrayOf(144, 140, 134)  // Pale limestone cobble
        )

        for (r in 0 until rows) {
            val stagger = if (r % 2 == 1) colW * 0.48f else 0f
            for (col in -1..cols) {
                val baseX = col * colW + stagger + (rnd.nextFloat() - 0.5f) * 6f
                val baseY = (r + 0.5f) * rowH + (rnd.nextFloat() - 0.5f) * 4f
                val rx = colW * (0.37f + rnd.nextFloat() * 0.09f)
                val ry = rowH * (0.36f + rnd.nextFloat() * 0.09f)
                val colRgb = stonePalette[rnd.nextInt(stonePalette.size)]
                val jitter = rnd.nextInt(-14, 15)
                val sr = (colRgb[0] + jitter).coerceIn(55, 220)
                val sg = (colRgb[1] + jitter).coerceIn(50, 215)
                val sb = (colRgb[2] + jitter).coerceIn(45, 205)

                // Dark crevice rim under cobble
                p.color = Color.argb(165, 38, 34, 30)
                c.drawOval(baseX - rx + 1.5f, baseY - ry + 2.2f, baseX + rx + 1.5f, baseY + ry + 2.2f, p)

                // Main rounded river cobble body
                p.color = Color.rgb(sr, sg, sb)
                c.drawOval(baseX - rx, baseY - ry, baseX + rx, baseY + ry, p)

                // Worn sunlit smooth top highlight (polished by decades of tires & sandals!)
                p.color = Color.argb(115, (sr + 44).coerceAtMost(250), (sg + 42).coerceAtMost(248), (sb + 38).coerceAtMost(242))
                c.drawOval(
                    baseX - rx * 0.62f, baseY - ry * 0.62f,
                    baseX + rx * 0.34f, baseY + ry * 0.30f,
                    p
                )
            }
        }
        return bmp
    }

    /**
     * Photorealistic Seamless Golden Puerto Vallarta Beach Sand texture:
     * Soft organic sunlit dune shading, warm golden-ivory quartz grains, fine coastal shells & subtle dark volcanic mineral flecks!
     */
    private fun createBeachSandBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(232, 212, 174))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(512)

        // Seamless soft multi-scale dune variation (wrapped edges so tiling is invisible!)
        for (i in 0 until 420) {
            val cx = rnd.nextFloat() * s
            val cy = rnd.nextFloat() * s
            val rad = rnd.nextFloat() * 44f + 16f
            val warm = rnd.nextBoolean()
            p.color = if (warm) Color.argb(26, 246, 226, 184) else Color.argb(24, 198, 172, 132)
            for (ox in floatArrayOf(-s.toFloat(), 0f, s.toFloat())) {
                for (oy in floatArrayOf(-s.toFloat(), 0f, s.toFloat())) {
                    c.drawCircle(cx + ox, cy + oy, rad, p)
                }
            }
        }
        // Subtle fine micro-grain golden quartz, ivory shell dust & rare dark Pacific volcanic flecks
        for (i in 0 until 38000) {
            val roll = rnd.nextFloat()
            p.color = when {
                roll < 0.045f -> Color.argb(95, 132, 110, 84)   // Subtle volcanic sand speck
                roll < 0.24f -> Color.argb(120, 252, 245, 228)  // Sunlit quartz & shell micro-highlight
                else -> {
                    val v = rnd.nextInt(208, 244)
                    Color.argb(105, v, (v - 18).coerceAtLeast(165), (v - 54).coerceAtLeast(125))
                }
            }
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            val r = rnd.nextFloat() * 1.6f + 0.5f
            c.drawRect(x, y, x + r, y + r, p)
        }
        return bmp
    }

    /**
     * Rich Lush Tropical Sierra Madre Foliage & Marina Vallarta Golf Turf texture:
     * Seamless multi-tone emerald, sunlit tropical lime & deep forest soil depth!
     */
    private fun createTropicalGrassBitmap(): Bitmap {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(58, 114, 48))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(808)

        // Seamless dappled tropical meadow & canopy light patches
        for (i in 0 until 520) {
            val cx = rnd.nextFloat() * s
            val cy = rnd.nextFloat() * s
            val rad = rnd.nextFloat() * 28f + 10f
            p.color = when (rnd.nextInt(3)) {
                0 -> Color.argb(42, 34, 82, 28)
                1 -> Color.argb(44, 84, 148, 54)
                else -> Color.argb(38, 108, 156, 58)
            }
            for (ox in floatArrayOf(-s.toFloat(), 0f, s.toFloat())) {
                for (oy in floatArrayOf(-s.toFloat(), 0f, s.toFloat())) {
                    c.drawCircle(cx + ox, cy + oy, rad, p)
                }
            }
        }
        // Fine organic grass blades & tropical fern texture
        p.strokeWidth = 1.6f
        for (i in 0 until 18000) {
            val x = rnd.nextFloat() * s
            val y = rnd.nextFloat() * s
            val g = rnd.nextInt(94, 178)
            p.color = Color.argb(122, (g * 0.44f).toInt(), g, (g * 0.30f).toInt())
            val dx = (rnd.nextFloat() - 0.5f) * 5.5f
            val dy = -rnd.nextFloat() * 7.5f - 1.5f
            c.drawLine(x, y, x + dx, y + dy, p)
        }
        return bmp
    }

    /**
     * Generates a crisp 1024x1024 4x8 Illuminated 3D Signboard Atlas featuring REAL Puerto Vallarta
     * hotel names, resorts, clubs, and coastal landmarks so players instantly recognize every spot!
     */
    private fun createHotelSignAtlas(): Pair<Bitmap, Map<String, Int>> {
        val w = 1024
        val h = 1024
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(14, 22, 36))

        data class SignSpec(
            val buildingId: String,
            val title: String,
            val subtitle: String,
            val bgHex: Int,
            val accentHex: Int,
            val textHex: Int = Color.WHITE
        )

        val specs = listOf(
            SignSpec("pvr_terminal", "AEROPUERTO PVR", "PUERTO VALLARTA INTL", Color.rgb(14, 42, 82), Color.rgb(0, 214, 204)),
            SignSpec("marriott_pv", "MARRIOTT", "PUERTO VALLARTA RESORT", Color.rgb(92, 18, 30), Color.rgb(245, 198, 92)),
            SignSpec("westin_pv", "THE WESTIN", "RESORT & SPA MARINA", Color.rgb(142, 62, 34), Color.rgb(252, 220, 168)),
            SignSpec("velas_north", "VELAS VALLARTA", "SUITE RESORT • MARINA", Color.rgb(24, 52, 74), Color.rgb(242, 196, 102)),
            SignSpec("faro_marina", "EL FARO", "MARINA VALLARTA", Color.rgb(156, 28, 30), Color.WHITE),
            SignSpec("galerias_pv", "GALERÍAS", "VALLARTA MALL", Color.rgb(26, 36, 58), Color.rgb(255, 112, 58)),
            SignSpec("krystal_pv", "KRYSTAL", "PUERTO VALLARTA", Color.rgb(16, 78, 128), Color.rgb(112, 232, 245)),
            SignSpec("hilton_pv", "HILTON", "VALLARTA RIVIERA", Color.rgb(12, 36, 76), Color.rgb(185, 222, 255)),
            SignSpec("peninsula_1", "PENINSULA", "TOWERS VALLARTA", Color.rgb(18, 28, 42), Color.rgb(0, 220, 210)),
            SignSpec("grand_venetian_1", "GRAND VENETIAN", "BEACHFRONT TOWERS", Color.rgb(22, 46, 68), Color.rgb(250, 205, 110)),
            SignSpec("la_isla", "LA ISLA", "SHOPPING VILLAGE", Color.rgb(10, 96, 108), Color.rgb(255, 214, 92)),
            SignSpec("fiesta_americana", "FIESTA AMERICANA", "PUERTO VALLARTA & SPA", Color.rgb(132, 42, 24), Color.rgb(255, 198, 86)),
            SignSpec("icon_vallarta_1", "ICON VALLARTA", "BY PHILIPPE STARCK", Color.rgb(16, 18, 24), Color.rgb(255, 255, 255)),
            SignSpec("secrets_bay", "SECRETS", "VALLARTA BAY RESORT", Color.rgb(42, 24, 62), Color.rgb(246, 198, 102)),
            SignSpec("sheraton_pv", "SHERATON", "BUGANVILIAS RESORT", Color.rgb(118, 28, 46), Color.rgb(255, 214, 126)),
            SignSpec("villa_premiere", "VILLA PREMIERE", "BOUTIQUE HOTEL", Color.rgb(16, 64, 84), Color.rgb(242, 226, 186)),
            SignSpec("buenaventura", "BUENAVENTURA", "GRAND HOTEL & SPA", Color.rgb(168, 66, 26), Color.rgb(255, 228, 162)),
            SignSpec("hotel_rosita", "HOTEL ROSITA", "MALECÓN • DESDE 1948", Color.rgb(148, 46, 28), Color.rgb(255, 224, 152)),
            SignSpec("mandala_pv", "MANDALA", "LA VAQUITA • MALECÓN", Color.rgb(34, 10, 48), Color.rgb(255, 38, 128)),
            SignSpec("senor_frogs", "SEÑOR FROG'S", "PASEO DÍAZ ORDAZ", Color.rgb(18, 118, 52), Color.rgb(255, 216, 46)),
            SignSpec("los_arcos_malecon", "LOS ARCOS", "ANFITEATRO DEL MALECÓN", Color.rgb(86, 56, 36), Color.rgb(255, 216, 148)),
            SignSpec("parroquia", "GUADALUPE", "PARROQUIA HISTÓRICA", Color.rgb(122, 42, 26), Color.rgb(255, 214, 118)),
            SignSpec("muelle_muertos", "MUELLE LOS MUERTOS", "ZONA ROMÁNTICA • PV", Color.rgb(10, 32, 58), Color.rgb(0, 232, 224)),
            SignSpec("playa_los_muertos_hotel", "PLAYA LOS MUERTOS", "OLAS ALTAS BEACHFRONT", Color.rgb(182, 74, 34), Color.rgb(255, 224, 148)),
            SignSpec("blue_chairs", "BLUE CHAIRS", "RESORT BY THE SEA", Color.rgb(12, 78, 168), Color.rgb(98, 226, 255)),
            SignSpec("tropicana_pv", "TROPICANA", "HOTEL PLAYA LOS MUERTOS", Color.rgb(206, 78, 22), Color.rgb(255, 232, 128)),
            SignSpec("almar_resort", "ALMAR RESORT", "LUXURY BEACHFRONT", Color.rgb(38, 14, 48), Color.rgb(255, 58, 142)),
            SignSpec("pinnacle_resorts", "PINNACLE", "ZONA ROMÁNTICA SUITES", Color.rgb(22, 34, 52), Color.rgb(240, 198, 112)),
            SignSpec("grand_miramar", "GRAND MIRAMAR", "CONCHAS CHINAS", Color.rgb(18, 46, 74), Color.rgb(245, 210, 128)),
            SignSpec("hyatt_ziva", "HYATT ZIVA", "PUERTO VALLARTA", Color.rgb(14, 62, 102), Color.rgb(86, 228, 218)),
            SignSpec("hotel_mousai", "HOTEL MOUSAI", "GARZA BLANCA PRESERVE", Color.rgb(24, 16, 36), Color.rgb(255, 52, 108)),
            SignSpec("barcelo_pv", "BARCELÓ", "MISMALOYA • LOS ARCOS", Color.rgb(14, 58, 96), Color.rgb(255, 206, 86))
        )

        val cols = 4
        val rows = 8
        val cellW = w / cols.toFloat()
        val cellH = h / rows.toFloat()

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 16f
        }

        val slotMap = HashMap<String, Int>()
        specs.forEachIndexed { idx, spec ->
            slotMap[spec.buildingId] = idx
            val col = idx % cols
            val row = idx / cols
            val x0 = col * cellW
            val y0 = row * cellH
            val rect = RectF(x0 + 4f, y0 + 4f, x0 + cellW - 4f, y0 + cellH - 4f)

            bgPaint.color = spec.bgHex
            c.drawRoundRect(rect, 12f, 12f, bgPaint)

            borderPaint.color = spec.accentHex
            c.drawRoundRect(rect, 12f, 12f, borderPaint)

            // Subtle architectural gold/neon accent bar
            bgPaint.color = spec.accentHex
            c.drawRect(x0 + 24f, y0 + cellH * 0.62f, x0 + cellW - 24f, y0 + cellH * 0.65f, bgPaint)

            // Fit title dynamically
            titlePaint.color = spec.textHex
            titlePaint.textSize = if (spec.title.length > 13) 26f else 33f
            c.drawText(spec.title, x0 + cellW * 0.5f, y0 + cellH * 0.48f, titlePaint)

            subPaint.color = spec.accentHex
            c.drawText(spec.subtitle, x0 + cellW * 0.5f, y0 + cellH * 0.84f, subPaint)
        }

        return bmp to slotMap
    }
}
