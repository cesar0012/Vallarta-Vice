package com.example.game.engine

/**
 * 10-Bone Articulated HIGH-POLYGON Organic Humanoid Anatomy & Wardrobe Kit!
 * Every character (Protagonist, Beachgoers in Swimwear/Boardshorts/Hawaiian Shirts/Sundresses,
 * Puerto Vallarta Locals, and Policía Municipal) uses separate smoothly sculpted bones with
 * true waist, neck, shoulder, elbow, hip, and knee pivots (~2,200+ smooth triangles per humanoid)
 * so walking, sprinting, climbing, jumping, and 3-hit boxing combos animate with fluid, lifelike physics!
 */
data class HumanoidMeshSet(
    val headAndNeck: MeshData,
    val pelvisAndHips: MeshData,
    val upperTorso: MeshData,
    val leftUpperArm: MeshData,
    val leftForearmAndHand: MeshData,
    val rightUpperArm: MeshData,
    val rightForearmAndHand: MeshData,
    val leftThigh: MeshData,
    val leftCalfAndFoot: MeshData,
    val rightThigh: MeshData,
    val rightCalfAndFoot: MeshData
) {
    val totalTriangleCount: Int
        get() = headAndNeck.triangleCount + pelvisAndHips.triangleCount + upperTorso.triangleCount +
            leftUpperArm.triangleCount + leftForearmAndHand.triangleCount +
            rightUpperArm.triangleCount + rightForearmAndHand.triangleCount +
            leftThigh.triangleCount + leftCalfAndFoot.triangleCount +
            rightThigh.triangleCount + rightCalfAndFoot.triangleCount
}

enum class TopStyle {
    OPEN_JACKET,
    SHORT_SLEEVE_SHIRT,
    HAWAIIAN_OPEN_SHIRT,
    BARE_BEACH_CHEST,
    BIKINI_TOP,
    SUNDRESS_TOP,
    POLICE_UNIFORM
}

enum class BottomStyle {
    LONG_PANTS,
    BEACH_SHORTS,
    SWIM_BOARDSHORTS,
    BIKINI_SARONG,
    FLOWING_DRESS
}

enum class FootwearStyle {
    SNEAKERS,
    BEACH_SANDALS,
    LOAFERS_BOOTS
}

enum class HeadwearStyle {
    NONE,
    SUN_HAT,
    STRAW_FEDORA,
    POLICE_CAP
}

data class CharacterStyle(
    val skinR: Float, val skinG: Float, val skinB: Float,
    val hairR: Float, val hairG: Float, val hairB: Float,
    val shirtR: Float, val shirtG: Float, val shirtB: Float,
    val accentR: Float, val accentG: Float, val accentB: Float,
    val pantsR: Float, val pantsG: Float, val pantsB: Float,
    val shoeR: Float, val shoeG: Float, val shoeB: Float,
    val topStyle: TopStyle = TopStyle.SHORT_SLEEVE_SHIRT,
    val bottomStyle: BottomStyle = BottomStyle.LONG_PANTS,
    val footwear: FootwearStyle = FootwearStyle.SNEAKERS,
    val headwear: HeadwearStyle = HeadwearStyle.NONE,
    val hasSunglasses: Boolean = false,
    val isFemaleSilhouette: Boolean = false,
    val hasWatchOrBracelet: Boolean = false,
    val hasNecklace: Boolean = false
)

object HighPolyCharacterMeshes {

    val PROTAGONIST = CharacterStyle(
        skinR = 0.85f, skinG = 0.64f, skinB = 0.49f,
        hairR = 0.09f, hairG = 0.07f, hairB = 0.06f,
        shirtR = 0.07f, shirtG = 0.78f, shirtB = 0.76f,   // Turquoise Bahía knit polo/tee
        accentR = 0.96f, accentG = 0.94f, accentB = 0.90f, // Tailored ivory coastal linen jacket
        pantsR = 0.14f, pantsG = 0.21f, pantsB = 0.35f,   // Tailored slim indigo denim
        shoeR = 0.96f, shoeG = 0.96f, shoeB = 0.97f,
        topStyle = TopStyle.OPEN_JACKET,
        bottomStyle = BottomStyle.LONG_PANTS,
        footwear = FootwearStyle.SNEAKERS,
        headwear = HeadwearStyle.NONE,
        hasSunglasses = true,
        hasWatchOrBracelet = true,
        hasNecklace = true
    )

    val POLICE_OFFICER = CharacterStyle(
        skinR = 0.78f, skinG = 0.58f, skinB = 0.44f,
        hairR = 0.09f, hairG = 0.08f, hairB = 0.07f,
        shirtR = 0.11f, shirtG = 0.18f, shirtB = 0.35f,
        accentR = 0.08f, accentG = 0.13f, accentB = 0.26f,
        pantsR = 0.08f, pantsG = 0.12f, pantsB = 0.23f,
        shoeR = 0.08f, shoeG = 0.08f, shoeB = 0.10f,
        topStyle = TopStyle.POLICE_UNIFORM,
        bottomStyle = BottomStyle.LONG_PANTS,
        footwear = FootwearStyle.LOAFERS_BOOTS,
        headwear = HeadwearStyle.POLICE_CAP,
        hasSunglasses = true
    )

    // ==================== STREET PEDESTRIANS (UPGRADED HIGH-POLY) ====================

    val TOURIST_CORAL = CharacterStyle(
        skinR = 0.90f, skinG = 0.71f, skinB = 0.57f,
        hairR = 0.44f, hairG = 0.28f, hairB = 0.14f,
        shirtR = 0.96f, shirtG = 0.39f, shirtB = 0.28f,
        accentR = 0.98f, accentG = 0.84f, accentB = 0.44f,
        pantsR = 0.84f, pantsG = 0.77f, pantsB = 0.63f,
        shoeR = 0.22f, shoeG = 0.24f, shoeB = 0.28f,
        topStyle = TopStyle.SHORT_SLEEVE_SHIRT,
        bottomStyle = BottomStyle.BEACH_SHORTS,
        footwear = FootwearStyle.SNEAKERS,
        hasSunglasses = true,
        hasWatchOrBracelet = true
    )

    val LOCAL_DRESS = CharacterStyle(
        skinR = 0.80f, skinG = 0.59f, skinB = 0.44f,
        hairR = 0.11f, hairG = 0.07f, hairB = 0.05f,
        shirtR = 0.94f, shirtG = 0.21f, shirtB = 0.50f,
        accentR = 0.98f, accentG = 0.76f, accentB = 0.26f,
        pantsR = 0.90f, pantsG = 0.17f, pantsB = 0.46f,
        shoeR = 0.90f, shoeG = 0.74f, shoeB = 0.34f,
        topStyle = TopStyle.SUNDRESS_TOP,
        bottomStyle = BottomStyle.FLOWING_DRESS,
        footwear = FootwearStyle.BEACH_SANDALS,
        hasSunglasses = true,
        isFemaleSilhouette = true,
        hasNecklace = true
    )

    val LOCAL_GUAYABERA = CharacterStyle(
        skinR = 0.76f, skinG = 0.56f, skinB = 0.41f,
        hairR = 0.13f, hairG = 0.11f, hairB = 0.10f,
        shirtR = 0.96f, shirtG = 0.94f, shirtB = 0.86f,
        accentR = 0.88f, accentG = 0.84f, accentB = 0.74f,
        pantsR = 0.24f, pantsG = 0.27f, pantsB = 0.32f,
        shoeR = 0.36f, shoeG = 0.22f, shoeB = 0.12f,
        topStyle = TopStyle.SHORT_SLEEVE_SHIRT,
        bottomStyle = BottomStyle.LONG_PANTS,
        footwear = FootwearStyle.LOAFERS_BOOTS,
        hasWatchOrBracelet = true
    )

    // ==================== AUTHENTIC PUERTO VALLARTA BEACHGOERS ====================

    val BEACH_SWIM_SHORTS = CharacterStyle(
        skinR = 0.83f, skinG = 0.60f, skinB = 0.43f,    // Sun-kissed Bahía beach tan
        hairR = 0.22f, hairG = 0.15f, hairB = 0.09f,
        shirtR = 0.83f, shirtG = 0.60f, shirtB = 0.43f,  // Bare athletic beach torso
        accentR = 0.98f, accentG = 0.82f, accentB = 0.20f, // Neon yellow racing stripe on boardshorts
        pantsR = 0.06f, pantsG = 0.62f, pantsB = 0.80f,  // Turquoise surfer boardshorts
        shoeR = 0.08f, shoeG = 0.70f, shoeB = 0.78f,     // Turquoise beach flip-flops
        topStyle = TopStyle.BARE_BEACH_CHEST,
        bottomStyle = BottomStyle.SWIM_BOARDSHORTS,
        footwear = FootwearStyle.BEACH_SANDALS,
        headwear = HeadwearStyle.NONE,
        hasSunglasses = true,
        hasWatchOrBracelet = true,
        hasNecklace = true
    )

    val BEACH_BIKINI_SARONG = CharacterStyle(
        skinR = 0.84f, skinG = 0.63f, skinB = 0.47f,
        hairR = 0.18f, hairG = 0.11f, hairB = 0.07f,
        shirtR = 0.98f, shirtG = 0.30f, shirtB = 0.44f,  // Coral-pink bikini top
        accentR = 0.10f, accentG = 0.82f, accentB = 0.80f, // Turquoise hat ribbon & pareo trim
        pantsR = 0.08f, pantsG = 0.72f, pantsB = 0.74f,  // Sheer tropical beach pareo / swim wrap
        shoeR = 0.96f, shoeG = 0.76f, shoeB = 0.32f,     // Gold beach sandals
        topStyle = TopStyle.BIKINI_TOP,
        bottomStyle = BottomStyle.BIKINI_SARONG,
        footwear = FootwearStyle.BEACH_SANDALS,
        headwear = HeadwearStyle.SUN_HAT,
        hasSunglasses = true,
        isFemaleSilhouette = true,
        hasNecklace = true
    )

    val BEACH_HAWAIIAN_SHIRT = CharacterStyle(
        skinR = 0.88f, skinG = 0.68f, skinB = 0.53f,
        hairR = 0.32f, hairG = 0.22f, hairB = 0.13f,
        shirtR = 0.08f, shirtG = 0.70f, shirtB = 0.64f,  // Vibrant palm-teal open Hawaiian shirt
        accentR = 0.98f, accentG = 0.52f, accentB = 0.22f, // Sunset coral floral shirt accents
        pantsR = 0.91f, pantsG = 0.84f, pantsB = 0.70f,  // Crisp khaki beach shorts
        shoeR = 0.46f, shoeG = 0.29f, shoeB = 0.16f,     // Leather beach sandals
        topStyle = TopStyle.HAWAIIAN_OPEN_SHIRT,
        bottomStyle = BottomStyle.BEACH_SHORTS,
        footwear = FootwearStyle.BEACH_SANDALS,
        headwear = HeadwearStyle.STRAW_FEDORA,
        hasSunglasses = true,
        hasWatchOrBracelet = true
    )

    val BEACH_SUNDRESS = CharacterStyle(
        skinR = 0.89f, skinG = 0.70f, skinB = 0.55f,
        hairR = 0.58f, hairG = 0.42f, hairB = 0.20f,     // Sun-bleached blonde beach waves
        shirtR = 0.98f, shirtG = 0.84f, shirtB = 0.30f,  // Sunny mango-yellow beach sundress
        accentR = 0.96f, accentG = 0.44f, accentB = 0.36f, // Coral waist sash & sunhat ribbon
        pantsR = 0.98f, pantsG = 0.80f, pantsB = 0.24f,
        shoeR = 0.92f, shoeG = 0.74f, shoeB = 0.46f,
        topStyle = TopStyle.SUNDRESS_TOP,
        bottomStyle = BottomStyle.FLOWING_DRESS,
        footwear = FootwearStyle.BEACH_SANDALS,
        headwear = HeadwearStyle.SUN_HAT,
        hasSunglasses = true,
        isFemaleSilhouette = true,
        hasNecklace = true
    )

    fun buildHumanoid(style: CharacterStyle): HumanoidMeshSet {
        return HumanoidMeshSet(
            headAndNeck = buildHead(style),
            pelvisAndHips = buildPelvis(style),
            upperTorso = buildUpperTorso(style),
            leftUpperArm = buildUpperArm(style, side = -1f),
            leftForearmAndHand = buildForearmAndHand(style, side = -1f),
            rightUpperArm = buildUpperArm(style, side = 1f),
            rightForearmAndHand = buildForearmAndHand(style, side = 1f),
            leftThigh = buildThigh(style, side = -1f),
            leftCalfAndFoot = buildCalfAndFoot(style, side = -1f),
            rightThigh = buildThigh(style, side = 1f),
            rightCalfAndFoot = buildCalfAndFoot(style, side = 1f)
        )
    }

    /**
     * Local origin at neck base (y = 0 -> crown at +0.32m).
     * High-slice smooth cranium, jawline, cheekbones, nose, ears, hair, sun hats, and aviator/wayfarer shades.
     */
    private fun buildHead(s: CharacterStyle): MeshData {
        val m = MeshBuilder(760)
        // Smooth anatomical neck with trapezius blend
        m.addTaperedLimb(0f, -0.03f, 0f, 0.068f, 0.064f, 0f, 0.085f, 0.01f, 0.054f, 0.055f, s.skinR, s.skinG, s.skinB, slices = 14, rings = 3, capBottom = true, capTop = false)
        // High-smoothness cranium & face oval
        m.addEllipsoid(0f, 0.195f, 0.012f, 0.104f, 0.126f, 0.116f, s.skinR, s.skinG, s.skinB, stacks = 12, slices = 18)
        // Sculpted cheekbones & tapered chin/jawline
        m.addEllipsoid(0f, 0.115f, 0.044f, 0.068f, 0.060f, 0.075f, s.skinR * 0.98f, s.skinG * 0.97f, s.skinB * 0.97f, stacks = 8, slices = 14)
        m.addEllipsoid(-0.056f, 0.165f, 0.076f, 0.032f, 0.028f, 0.030f, s.skinR * 1.02f, s.skinG * 0.99f, s.skinB * 0.98f, stacks = 6, slices = 10)
        m.addEllipsoid(0.056f, 0.165f, 0.076f, 0.032f, 0.028f, 0.030f, s.skinR * 1.02f, s.skinG * 0.99f, s.skinB * 0.98f, stacks = 6, slices = 10)
        // Sculpted nose bridge & tip
        m.addEllipsoid(0f, 0.178f, 0.124f, 0.016f, 0.028f, 0.022f, s.skinR * 0.96f, s.skinG * 0.94f, s.skinB * 0.93f, stacks = 6, slices = 10)
        // Sculpted ears
        m.addEllipsoid(-0.105f, 0.188f, 0.0f, 0.019f, 0.035f, 0.024f, s.skinR * 0.94f, s.skinG * 0.92f, s.skinB * 0.92f, stacks = 6, slices = 10)
        m.addEllipsoid(0.105f, 0.188f, 0.0f, 0.019f, 0.035f, 0.024f, s.skinR * 0.94f, s.skinG * 0.92f, s.skinB * 0.92f, stacks = 6, slices = 10)

        // Hair base (always rendered under sun hats/fedoras for natural look)
        if (s.headwear != HeadwearStyle.POLICE_CAP) {
            m.addEllipsoid(0f, 0.252f, -0.012f, 0.112f, 0.092f, 0.122f, s.hairR, s.hairG, s.hairB, stacks = 10, slices = 18)
            m.addEllipsoid(0f, 0.285f, 0.034f, 0.086f, 0.044f, 0.074f, s.hairR * 1.14f, s.hairG * 1.14f, s.hairB * 1.14f, stacks = 7, slices = 14)
            m.addEllipsoid(0f, 0.192f, -0.074f, 0.098f, 0.092f, 0.070f, s.hairR, s.hairG, s.hairB, stacks = 8, slices = 14)
            if (s.isFemaleSilhouette) {
                // Flowing layered beach waves framing shoulders & back
                m.addTaperedLimb(-0.080f, -0.05f, -0.055f, 0.040f, 0.044f, -0.094f, 0.195f, -0.035f, 0.054f, 0.054f, s.hairR, s.hairG, s.hairB, slices = 12, rings = 4)
                m.addTaperedLimb(0.080f, -0.05f, -0.055f, 0.040f, 0.044f, 0.094f, 0.195f, -0.035f, 0.054f, 0.054f, s.hairR, s.hairG, s.hairB, slices = 12, rings = 4)
                m.addTaperedLimb(0f, -0.08f, -0.088f, 0.072f, 0.040f, 0f, 0.18f, -0.080f, 0.088f, 0.052f, s.hairR * 0.95f, s.hairG * 0.95f, s.hairB * 0.95f, slices = 12, rings = 4)
            }
        }

        // Authentic Headwear: Wide-brim woven beach sun hat, coastal straw fedora, or Policía cap
        when (s.headwear) {
            HeadwearStyle.POLICE_CAP -> {
                m.addCylinderY(0f, 0.242f, 0.312f, 0f, 0.112f, 0.122f, 0.09f, 0.14f, 0.28f, MaterialId.MATTE, slices = 18)
                m.addEllipsoid(0f, 0.312f, 0f, 0.124f, 0.026f, 0.124f, 0.09f, 0.14f, 0.28f, stacks = 7, slices = 16)
                m.addBox(0f, 0.246f, 0.115f, 0.19f, 0.014f, 0.10f, 0.04f, 0.04f, 0.05f, MaterialId.GLOSSY_PAINT)
                m.addEllipsoid(0f, 0.286f, 0.120f, 0.024f, 0.028f, 0.012f, 0.92f, 0.84f, 0.42f, MaterialId.CHROME, stacks = 6, slices = 10)
            }
            HeadwearStyle.SUN_HAT -> {
                // Wide-brim woven straw beach sun hat with colorful tropical ribbon!
                m.addCylinderY(0f, 0.256f, 0.270f, -0.006f, 0.245f, 0.240f, 0.89f, 0.78f, 0.54f, MaterialId.MATTE, slices = 22)
                m.addCylinderY(0f, 0.268f, 0.296f, -0.006f, 0.124f, 0.120f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE, slices = 20)
                m.addEllipsoid(0f, 0.318f, -0.006f, 0.116f, 0.056f, 0.116f, 0.91f, 0.80f, 0.56f, stacks = 8, slices = 18)
            }
            HeadwearStyle.STRAW_FEDORA -> {
                // Stylish Puerto Vallarta woven straw Panama fedora with dark ribbon!
                m.addCylinderY(0f, 0.258f, 0.270f, 0.004f, 0.168f, 0.164f, 0.90f, 0.82f, 0.62f, MaterialId.MATTE, slices = 20)
                m.addCylinderY(0f, 0.268f, 0.294f, 0.0f, 0.116f, 0.112f, 0.14f, 0.16f, 0.22f, MaterialId.MATTE, slices = 18)
                m.addCylinderY(0f, 0.294f, 0.354f, 0.0f, 0.110f, 0.095f, 0.92f, 0.84f, 0.66f, MaterialId.MATTE, slices = 18)
            }
            HeadwearStyle.NONE -> {}
        }

        // Brows & natural sculpted mouth
        m.addBox(-0.042f, 0.236f, 0.104f, 0.040f, 0.009f, 0.012f, s.hairR, s.hairG, s.hairB, MaterialId.MATTE)
        m.addBox(0.042f, 0.236f, 0.104f, 0.040f, 0.009f, 0.012f, s.hairR, s.hairG, s.hairB, MaterialId.MATTE)
        m.addEllipsoid(0f, 0.136f, 0.105f, 0.027f, 0.010f, 0.012f, s.skinR * 0.78f, s.skinG * 0.56f, s.skinB * 0.55f, MaterialId.MATTE, stacks = 5, slices = 10)

        if (s.hasSunglasses) {
            // Sleek reflective coastal sunglasses with metallic frame & temple arms
            m.addEllipsoid(-0.045f, 0.207f, 0.110f, 0.039f, 0.027f, 0.014f, 0.04f, 0.08f, 0.14f, MaterialId.GLASS, stacks = 7, slices = 14)
            m.addEllipsoid(0.045f, 0.207f, 0.110f, 0.039f, 0.027f, 0.014f, 0.04f, 0.08f, 0.14f, MaterialId.GLASS, stacks = 7, slices = 14)
            m.addBox(0f, 0.217f, 0.116f, 0.044f, 0.007f, 0.010f, 0.90f, 0.78f, 0.34f, MaterialId.CHROME)
            m.addBox(0f, 0.226f, 0.115f, 0.078f, 0.005f, 0.009f, 0.90f, 0.78f, 0.34f, MaterialId.CHROME)
            m.addBox(-0.096f, 0.210f, 0.056f, 0.008f, 0.007f, 0.105f, 0.88f, 0.76f, 0.32f, MaterialId.CHROME)
            m.addBox(0.096f, 0.210f, 0.056f, 0.008f, 0.007f, 0.105f, 0.88f, 0.76f, 0.32f, MaterialId.CHROME)
        } else {
            m.addEllipsoid(-0.040f, 0.208f, 0.105f, 0.014f, 0.012f, 0.010f, 0.10f, 0.09f, 0.09f, MaterialId.GLOSSY_PAINT, stacks = 5, slices = 10)
            m.addEllipsoid(0.040f, 0.208f, 0.105f, 0.014f, 0.012f, 0.010f, 0.10f, 0.09f, 0.09f, MaterialId.GLOSSY_PAINT, stacks = 5, slices = 10)
        }

        return m.build()
    }

    /**
     * Local origin at hip center (y = 0). Supports tailored pants/belt, surfer boardshorts with drawstring,
     * beach bikini wrap/pareo, or flowing sundress skirt!
     */
    private fun buildPelvis(s: CharacterStyle): MeshData {
        val m = MeshBuilder(420)
        val hipW = if (s.isFemaleSilhouette) 0.186f else 0.172f
        val waistW = if (s.isFemaleSilhouette) 0.138f else 0.162f

        // Smooth pelvis sphere
        m.addEllipsoid(0f, 0.015f, 0f, hipW, 0.118f, 0.118f, s.pantsR, s.pantsG, s.pantsB, stacks = 10, slices = 18)

        when (s.bottomStyle) {
            BottomStyle.LONG_PANTS, BottomStyle.BEACH_SHORTS -> {
                // Tailored waistband + leather belt + metallic buckle
                m.addCylinderY(0f, 0.072f, 0.114f, 0f, waistW * 1.04f, waistW * 1.03f, 0.13f, 0.09f, 0.07f, MaterialId.MATTE, slices = 18)
                m.addBox(0f, 0.093f, 0.120f, 0.056f, 0.036f, 0.022f, 0.90f, 0.80f, 0.38f, MaterialId.CHROME)
            }
            BottomStyle.SWIM_BOARDSHORTS -> {
                // Surfer boardshorts waistband with contrasting neon stripe & white tie drawstring!
                m.addCylinderY(0f, 0.068f, 0.112f, 0f, waistW * 1.03f, waistW * 1.02f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE, slices = 18)
                m.addBox(0f, 0.088f, 0.120f, 0.044f, 0.016f, 0.016f, 0.96f, 0.96f, 0.96f, MaterialId.MATTE)
            }
            BottomStyle.BIKINI_SARONG -> {
                // Flowing tropical beach pareo/wrap skirt draped gracefully over swimwear hips!
                m.addCylinderY(0f, -0.24f, 0.065f, 0f, 0.225f, 0.172f, s.pantsR, s.pantsG, s.pantsB, MaterialId.MATTE, slices = 20)
                m.addCylinderY(0f, 0.045f, 0.090f, 0f, 0.175f, 0.168f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE, slices = 20)
                // Exposed tanned lower midriff bridging pelvis to upperTorso
                m.addCylinderY(0f, 0.085f, 0.135f, 0f, 0.142f, 0.134f, s.skinR, s.skinG, s.skinB, MaterialId.MATTE, slices = 16)
            }
            BottomStyle.FLOWING_DRESS -> {
                // Breezy pleated coastal sundress skirt + colorful waist sash!
                m.addCylinderY(0f, -0.30f, 0.075f, 0f, 0.258f, 0.165f, s.pantsR, s.pantsG, s.pantsB, MaterialId.MATTE, slices = 22)
                m.addCylinderY(0f, 0.068f, 0.112f, 0f, waistW * 1.05f, waistW * 1.04f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE, slices = 18)
            }
        }
        return m.build()
    }

    /**
     * Local origin at waist pivot (y = 0 -> neck base at +0.50m, shoulder pivots at ±shoulderW, y = +0.42m).
     * Pivots independently from pelvis for realistic boxing torso snap & fluid running twist!
     */
    private fun buildUpperTorso(s: CharacterStyle): MeshData {
        val m = MeshBuilder(960)
        val shoulderW = if (s.isFemaleSilhouette) 0.184f else 0.228f
        val waistW = if (s.isFemaleSilhouette) 0.134f else 0.160f

        when (s.topStyle) {
            TopStyle.BARE_BEACH_CHEST -> {
                // Sculpted tanned athletic surfer torso with pectorals, lats & shell/silver beach necklace!
                m.addTaperedLimb(0f, -0.02f, 0f, waistW, 0.112f, 0f, 0.42f, 0.01f, shoulderW * 0.98f, 0.136f, s.skinR, s.skinG, s.skinB, slices = 18, rings = 6, capBottom = true, capTop = true)
                m.addEllipsoid(-0.075f, 0.34f, 0.095f, 0.076f, 0.062f, 0.056f, s.skinR * 1.02f, s.skinG * 1.01f, s.skinB * 1.01f, stacks = 8, slices = 14)
                m.addEllipsoid(0.075f, 0.34f, 0.095f, 0.076f, 0.062f, 0.056f, s.skinR * 1.02f, s.skinG * 1.01f, s.skinB * 1.01f, stacks = 8, slices = 14)
                m.addEllipsoid(0f, 0.41f, 0.005f, shoulderW * 1.00f, 0.088f, 0.128f, s.skinR, s.skinG, s.skinB, stacks = 8, slices = 16)
            }
            TopStyle.BIKINI_TOP -> {
                // Sculpted tanned feminine midriff & back + colorful sculpted bikini halter top!
                m.addTaperedLimb(0f, -0.02f, 0f, waistW, 0.106f, 0f, 0.41f, 0.008f, shoulderW * 0.96f, 0.122f, s.skinR, s.skinG, s.skinB, slices = 18, rings = 6, capBottom = true, capTop = true)
                m.addEllipsoid(0f, 0.40f, 0.005f, shoulderW * 0.96f, 0.076f, 0.114f, s.skinR, s.skinG, s.skinB, stacks = 8, slices = 16)
                // Bikini cups & crisp back strap
                m.addEllipsoid(-0.064f, 0.30f, 0.086f, 0.062f, 0.058f, 0.054f, s.shirtR, s.shirtG, s.shirtB, stacks = 8, slices = 14)
                m.addEllipsoid(0.064f, 0.30f, 0.086f, 0.062f, 0.058f, 0.054f, s.shirtR, s.shirtG, s.shirtB, stacks = 8, slices = 14)
                m.addCylinderY(0f, 0.275f, 0.312f, 0.0f, 0.168f, 0.172f, s.shirtR, s.shirtG, s.shirtB, MaterialId.MATTE, slices = 18)
            }
            TopStyle.HAWAIIAN_OPEN_SHIRT -> {
                // Sun-tanned inner chest peeking through breezy unbuttoned Hawaiian tropical shirt!
                m.addTaperedLimb(0f, -0.02f, 0f, waistW * 0.95f, 0.108f, 0f, 0.42f, 0.01f, shoulderW * 0.94f, 0.128f, s.skinR, s.skinG, s.skinB, slices = 16, rings = 5)
                // Vibrant open left/right shirt panels + tropical floral accent stripes + open camp collar!
                m.addTaperedLimb(-0.105f, -0.04f, 0.014f, 0.095f, 0.124f, -0.118f, 0.43f, 0.014f, 0.114f, 0.142f, s.shirtR, s.shirtG, s.shirtB, slices = 14, rings = 4)
                m.addTaperedLimb(0.105f, -0.04f, 0.014f, 0.095f, 0.124f, 0.118f, 0.43f, 0.014f, 0.114f, 0.142f, s.shirtR, s.shirtG, s.shirtB, slices = 14, rings = 4)
                m.addTaperedLimb(0f, -0.04f, -0.042f, waistW * 0.98f, 0.094f, 0f, 0.43f, -0.032f, shoulderW * 0.99f, 0.112f, s.shirtR, s.shirtG, s.shirtB, slices = 14, rings = 4)
                // Tropical Hawaiian pattern pockets/stripes
                m.addBox(-0.105f, 0.28f, 0.136f, 0.068f, 0.065f, 0.018f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE)
                m.addBox(0.105f, 0.16f, 0.130f, 0.072f, 0.055f, 0.018f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE)
                m.addCylinderY(0f, 0.44f, 0.495f, 0.005f, 0.092f, 0.084f, s.shirtR * 0.94f, s.shirtG * 0.94f, s.shirtB * 0.94f, MaterialId.MATTE, slices = 16)
            }
            TopStyle.SUNDRESS_TOP -> {
                // Sleeveless breezy coastal sundress bodice with tanned shoulders & collarbone
                m.addTaperedLimb(0f, -0.02f, 0f, waistW, 0.110f, 0f, 0.34f, 0.012f, shoulderW * 0.92f, 0.126f, s.shirtR, s.shirtG, s.shirtB, slices = 18, rings = 5)
                m.addTaperedLimb(0f, 0.33f, 0.008f, shoulderW * 0.88f, 0.116f, 0f, 0.43f, 0.006f, shoulderW * 0.92f, 0.110f, s.skinR, s.skinG, s.skinB, slices = 16, rings = 3)
                m.addEllipsoid(0f, 0.41f, 0.004f, shoulderW * 0.95f, 0.072f, 0.112f, s.skinR, s.skinG, s.skinB, stacks = 8, slices = 16)
            }
            else -> {
                // Tailored shirt / Vice linen jacket / Policía uniform
                m.addTaperedLimb(0f, -0.02f, 0f, waistW, 0.116f, 0f, 0.42f, 0.01f, shoulderW, 0.138f, s.shirtR, s.shirtG, s.shirtB, slices = 18, rings = 6, capBottom = true, capTop = true)
                m.addEllipsoid(0f, 0.415f, 0.01f, shoulderW * 1.02f, 0.092f, 0.132f, s.shirtR, s.shirtG, s.shirtB, stacks = 8, slices = 18)
                m.addCylinderY(0f, 0.465f, 0.515f, 0.005f, 0.088f, 0.080f, s.shirtR * 0.94f, s.shirtG * 0.94f, s.shirtB * 0.94f, MaterialId.MATTE, slices = 16)

                if (s.topStyle == TopStyle.OPEN_JACKET || s.topStyle == TopStyle.POLICE_UNIFORM) {
                    m.addTaperedLimb(-0.112f, -0.04f, 0.012f, 0.096f, 0.126f, -0.126f, 0.44f, 0.012f, 0.116f, 0.146f, s.accentR, s.accentG, s.accentB, slices = 14, rings = 4)
                    m.addTaperedLimb(0.112f, -0.04f, 0.012f, 0.096f, 0.126f, 0.126f, 0.44f, 0.012f, 0.116f, 0.146f, s.accentR, s.accentG, s.accentB, slices = 14, rings = 4)
                    m.addBox(-0.076f, 0.34f, 0.142f, 0.036f, 0.24f, 0.020f, s.accentR * 0.96f, s.accentG * 0.96f, s.accentB * 0.96f, MaterialId.MATTE)
                    m.addBox(0.076f, 0.34f, 0.142f, 0.036f, 0.24f, 0.020f, s.accentR * 0.96f, s.accentG * 0.96f, s.accentB * 0.96f, MaterialId.MATTE)
                    m.addTaperedLimb(0f, -0.04f, -0.044f, waistW * 0.98f, 0.096f, 0f, 0.44f, -0.034f, shoulderW * 0.99f, 0.116f, s.accentR, s.accentG, s.accentB, slices = 14, rings = 4)
                }
                if (s.topStyle == TopStyle.POLICE_UNIFORM) {
                    m.addEllipsoid(-0.10f, 0.37f, 0.152f, 0.030f, 0.034f, 0.014f, 0.92f, 0.86f, 0.45f, MaterialId.CHROME, stacks = 6, slices = 12)
                    m.addBox(0.12f, 0.44f, 0.092f, 0.04f, 0.055f, 0.03f, 0.10f, 0.10f, 0.12f, MaterialId.MATTE)
                }
            }
        }

        if (s.hasNecklace) {
            // Subtle gold/shell coastal pendant necklace at collar
            m.addCylinderY(0f, 0.445f, 0.460f, 0.018f, 0.088f, 0.088f, 0.92f, 0.80f, 0.36f, MaterialId.CHROME, slices = 16)
        }

        return m.build()
    }

    /**
     * Local origin at shoulder joint (y = 0, extends smoothly to elbow joint at y = -0.29m).
     */
    private fun buildUpperArm(s: CharacterStyle, side: Float): MeshData {
        val m = MeshBuilder(380)
        val armScale = if (s.isFemaleSilhouette) 0.84f else 1.0f
        val isSleeveless = s.topStyle == TopStyle.BARE_BEACH_CHEST ||
            s.topStyle == TopStyle.BIKINI_TOP ||
            s.topStyle == TopStyle.SUNDRESS_TOP
        val isShortSleeve = s.topStyle == TopStyle.SHORT_SLEEVE_SHIRT || s.topStyle == TopStyle.HAWAIIAN_OPEN_SHIRT

        val (slR, slG, slB) = when {
            isSleeveless -> Triple(s.skinR, s.skinG, s.skinB)
            s.topStyle == TopStyle.OPEN_JACKET || s.topStyle == TopStyle.POLICE_UNIFORM -> Triple(s.accentR, s.accentG, s.accentB)
            else -> Triple(s.shirtR, s.shirtG, s.shirtB)
        }

        // Smooth rounded deltoid cap
        m.addEllipsoid(0f, 0f, 0f, 0.080f * armScale, 0.082f * armScale, 0.080f * armScale, slR, slG, slB, stacks = 9, slices = 14)

        if (isShortSleeve) {
            // Crisp short beach/guayabera sleeve down to mid-bicep + tanned bare lower bicep!
            m.addTaperedLimb(0f, 0f, 0f, 0.072f * armScale, 0.072f * armScale, side * 0.008f, -0.16f, 0.005f, 0.064f * armScale, 0.064f * armScale, slR, slG, slB, slices = 14, rings = 3)
            m.addTaperedLimb(side * 0.008f, -0.15f, 0.005f, 0.058f * armScale, 0.058f * armScale, side * 0.012f, -0.29f, 0.010f, 0.050f * armScale, 0.050f * armScale, s.skinR, s.skinG, s.skinB, slices = 14, rings = 3)
        } else {
            // Full sleeve (rolled Vice jacket or Police uniform) OR bare tanned athletic upper arm
            m.addTaperedLimb(0f, 0f, 0f, 0.068f * armScale, 0.068f * armScale, side * 0.012f, -0.29f, 0.010f, 0.054f * armScale, 0.054f * armScale, slR, slG, slB, slices = 14, rings = 5)
            if (s.topStyle == TopStyle.OPEN_JACKET) {
                // Stylish rolled linen jacket cuff right above the elbow
                m.addCylinderY(side * 0.012f, -0.305f, -0.245f, 0.010f, 0.059f * armScale, 0.061f * armScale, slR * 0.94f, slG * 0.94f, slB * 0.94f, MaterialId.MATTE, slices = 14)
            }
        }
        // Seamless elbow joint sphere (hides seam at any flexion angle!)
        m.addEllipsoid(side * 0.012f, -0.29f, 0.010f, 0.050f * armScale, 0.050f * armScale, 0.050f * armScale, s.skinR, s.skinG, s.skinB, stacks = 7, slices = 12)

        return m.build()
    }

    /**
     * Local origin at elbow joint (y = 0, extends smoothly to wrist -0.26m and sculpted fist/hand -0.34m).
     * Pivots dynamically around the elbow for natural running pump, boxing guard, and explosive punch extension!
     */
    private fun buildForearmAndHand(s: CharacterStyle, side: Float): MeshData {
        val m = MeshBuilder(380)
        val armScale = if (s.isFemaleSilhouette) 0.84f else 1.0f
        val (fR, fG, fB) = if (s.topStyle == TopStyle.POLICE_UNIFORM) {
            Triple(s.accentR, s.accentG, s.accentB)
        } else {
            Triple(s.skinR, s.skinG, s.skinB)
        }

        // Sculpted forearm muscle tapering to wrist
        m.addTaperedLimb(0f, 0f, 0f, 0.050f * armScale, 0.052f * armScale, side * 0.006f, -0.255f, 0.014f, 0.036f * armScale, 0.038f * armScale, fR, fG, fB, slices = 14, rings = 4)

        // Dive watch or tropical wrist bracelet on left wrist
        if (side < 0f && s.hasWatchOrBracelet) {
            m.addCylinderY(side * 0.006f, -0.248f, -0.220f, 0.014f, 0.042f * armScale, 0.042f * armScale, 0.88f, 0.82f, 0.40f, MaterialId.CHROME, slices = 14)
        }

        // Sculpted palm, closed boxing knuckles & thumb
        m.addEllipsoid(side * 0.006f, -0.305f, 0.020f, 0.036f * armScale, 0.056f * armScale, 0.044f * armScale, s.skinR, s.skinG, s.skinB, stacks = 8, slices = 12)
        m.addEllipsoid(side * -0.010f, -0.292f, 0.052f, 0.016f * armScale, 0.024f * armScale, 0.022f * armScale, s.skinR, s.skinG, s.skinB, stacks = 6, slices = 10)

        return m.build()
    }

    /**
     * Local origin at hip socket (y = 0, extends smoothly to knee pivot at y = -0.43m).
     */
    private fun buildThigh(s: CharacterStyle, side: Float): MeshData {
        val m = MeshBuilder(360)
        val legScale = if (s.isFemaleSilhouette) 0.88f else 1.0f

        when (s.bottomStyle) {
            BottomStyle.LONG_PANTS -> {
                m.addTaperedLimb(0f, 0f, 0f, 0.098f * legScale, 0.100f * legScale, 0f, -0.43f, 0.008f, 0.068f * legScale, 0.070f * legScale, s.pantsR, s.pantsG, s.pantsB, slices = 14, rings = 5)
                m.addEllipsoid(0f, -0.43f, 0.012f, 0.066f * legScale, 0.064f * legScale, 0.068f * legScale, s.pantsR, s.pantsG, s.pantsB, stacks = 7, slices = 12)
            }
            BottomStyle.BEACH_SHORTS, BottomStyle.SWIM_BOARDSHORTS -> {
                // Shorts leg ending cleanly above the knee + bare sun-tanned lower thigh & knee!
                val shortEnd = if (s.bottomStyle == BottomStyle.SWIM_BOARDSHORTS) -0.34f else -0.29f
                m.addTaperedLimb(0f, 0f, 0f, 0.102f * legScale, 0.104f * legScale, 0f, shortEnd, 0.006f, 0.082f * legScale, 0.084f * legScale, s.pantsR, s.pantsG, s.pantsB, slices = 14, rings = 4)
                if (s.bottomStyle == BottomStyle.SWIM_BOARDSHORTS) {
                    // Crisp surfer side racing stripe on boardshorts!
                    m.addBox(side * 0.086f * legScale, shortEnd * 0.52f, 0.004f, 0.016f, -shortEnd * 0.82f, 0.036f, s.accentR, s.accentG, s.accentB, MaterialId.MATTE)
                }
                m.addTaperedLimb(0f, shortEnd + 0.02f, 0.006f, 0.070f * legScale, 0.072f * legScale, 0f, -0.43f, 0.008f, 0.062f * legScale, 0.064f * legScale, s.skinR, s.skinG, s.skinB, slices = 14, rings = 3)
                m.addEllipsoid(0f, -0.43f, 0.012f, 0.060f * legScale, 0.058f * legScale, 0.062f * legScale, s.skinR, s.skinG, s.skinB, stacks = 7, slices = 12)
            }
            BottomStyle.BIKINI_SARONG, BottomStyle.FLOWING_DRESS -> {
                // Bare tanned sculpted thighs emerging smoothly from beach sarong or sundress
                m.addTaperedLimb(0f, 0f, 0f, 0.088f * legScale, 0.090f * legScale, 0f, -0.43f, 0.008f, 0.060f * legScale, 0.062f * legScale, s.skinR, s.skinG, s.skinB, slices = 14, rings = 5)
                m.addEllipsoid(0f, -0.43f, 0.012f, 0.058f * legScale, 0.056f * legScale, 0.060f * legScale, s.skinR, s.skinG, s.skinB, stacks = 7, slices = 12)
            }
        }
        return m.build()
    }

    /**
     * Local origin at knee joint (y = 0, foot sole touches y = -0.47m -> total leg 0.90m).
     * Pivots dynamically at the knee for realistic stride lift, athletic jump tuck & sneaker/sandal foot strike!
     */
    private fun buildCalfAndFoot(s: CharacterStyle, side: Float): MeshData {
        val m = MeshBuilder(420)
        val legScale = if (s.isFemaleSilhouette) 0.88f else 1.0f
        val (cR, cG, cB) = if (s.bottomStyle == BottomStyle.LONG_PANTS) {
            Triple(s.pantsR, s.pantsG, s.pantsB)
        } else {
            Triple(s.skinR, s.skinG, s.skinB)
        }

        // Sculpted upper calf bulge + tapered ankle
        m.addTaperedLimb(0f, 0f, 0f, 0.062f * legScale, 0.066f * legScale, 0f, -0.16f, -0.008f, 0.066f * legScale, 0.072f * legScale, cR, cG, cB, slices = 14, rings = 3)
        m.addTaperedLimb(0f, -0.15f, -0.008f, 0.064f * legScale, 0.068f * legScale, 0f, -0.39f, -0.012f, 0.044f * legScale, 0.046f * legScale, cR, cG, cB, slices = 14, rings = 4)

        when (s.footwear) {
            FootwearStyle.BEACH_SANDALS -> {
                // Authentic Puerto Vallarta beach sandals / flip-flops: contoured bare foot + sandal footbed + V-strap!
                m.addEllipsoid(0f, -0.422f, 0.048f, 0.048f * legScale, 0.030f, 0.112f, s.skinR, s.skinG, s.skinB, stacks = 8, slices = 14)
                // Contoured sandal sole
                m.addBox(0f, -0.458f, 0.050f, 0.102f * legScale, 0.018f, 0.236f, s.shoeR, s.shoeG, s.shoeB, MaterialId.MATTE)
                // Sandals instep V-straps
                m.addBox(-0.024f * legScale, -0.410f, 0.062f, 0.018f, 0.014f, 0.088f, s.shoeR * 0.9f, s.shoeG * 0.9f, s.shoeB * 0.9f, MaterialId.MATTE)
                m.addBox(0.024f * legScale, -0.410f, 0.062f, 0.018f, 0.014f, 0.088f, s.shoeR * 0.9f, s.shoeG * 0.9f, s.shoeB * 0.9f, MaterialId.MATTE)
            }
            FootwearStyle.SNEAKERS -> {
                // Multi-detail coastal athletic sneaker with sculpted upper, white midsole & colored heel accent
                m.addEllipsoid(0f, -0.424f, 0.052f, 0.055f * legScale, 0.044f, 0.128f, s.shoeR, s.shoeG, s.shoeB, MaterialId.MATTE, stacks = 9, slices = 14)
                m.addBox(0f, -0.460f, 0.050f, 0.110f * legScale, 0.024f, 0.244f, 0.97f, 0.97f, 0.98f, MaterialId.MATTE)
                m.addBox(0f, -0.412f, -0.048f, 0.094f * legScale, 0.052f, 0.036f, s.shirtR, s.shirtG, s.shirtB, MaterialId.MATTE)
            }
            FootwearStyle.LOAFERS_BOOTS -> {
                // Polished leather loafers / Policía tactical boots
                m.addEllipsoid(0f, -0.422f, 0.050f, 0.054f * legScale, 0.046f, 0.128f, s.shoeR, s.shoeG, s.shoeB, MaterialId.GLOSSY_PAINT, stacks = 9, slices = 14)
                m.addBox(0f, -0.459f, 0.048f, 0.108f * legScale, 0.024f, 0.240f, 0.10f, 0.10f, 0.12f, MaterialId.MATTE)
            }
        }

        return m.build()
    }
}
