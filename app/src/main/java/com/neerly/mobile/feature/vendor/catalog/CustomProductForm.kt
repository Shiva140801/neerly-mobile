package com.neerly.mobile.feature.vendor.catalog

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The guided "make your own product" form, kept free of Android so it's unit-tested:
 * what it is → kind → measured in → size → packed as → price. Mirrors the server's
 * CustomProducts rules (size ranges, ice can't be a returnable jar, tankers are litres).
 */
enum class CustomCategory { WATER, ICE, OTHER }
enum class CustomUnit { L, ML, KG, PCS }
enum class Packaging { RETURNABLE, SEALED, BAG, TANKER, LOOSE }

data class CustomProductForm(
    val category: CustomCategory? = null,
    val kind: String? = null,
    val unit: CustomUnit? = null,
    val size: String = "",
    val packaging: Packaging? = null,
    val packCount: Int = 1,
    /** What the seller typed; blank = use [suggestedName]. */
    val name: String = "",
    val price: String = "",
    val dailyCapacity: Int = 30,
    val swap: Boolean = true,
    val deposit: String = "",
    val photoKey: String? = null
) {
    val sizeValue: BigDecimal? get() = size.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val priceValue: BigDecimal? get() = price.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val depositValue: BigDecimal? get() = deposit.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
    val returnable: Boolean get() = packaging == Packaging.RETURNABLE

    val sizeRange: ClosedRange<BigDecimal>? get() = unit?.let { u -> packaging?.let { p -> range(u, p) } }
    val sizeOk: Boolean get() = sizeValue?.let { v -> sizeRange?.let { v in it } } == true

    val suggestedName: String? get() {
        val c = category ?: return null; val u = unit ?: return null; val v = sizeValue ?: return null; val p = packaging ?: return null
        return defaultName(c, kind, u, v, p, packCount.takeIf { p == Packaging.SEALED && it > 1 })
    }

    /** A returnable jar needs a way back: the empty is swapped, or kept on a deposit. */
    val valid: Boolean get() = category != null && unit != null && packaging != null && sizeOk && priceValue != null &&
        dailyCapacity >= 1 && (!returnable || swap || depositValue != null) &&
        (category != CustomCategory.OTHER || !kind.isNullOrBlank())

    fun body(): CustomProductBody? = if (!valid) null else CustomProductBody(
        category = category!!.name, kind = kind?.trim()?.ifBlank { null }, unit = unit!!.name, size = sizeValue!!,
        packaging = packaging!!.name, packCount = packCount.takeIf { packaging == Packaging.SEALED && it > 1 },
        name = name.trim().ifBlank { null }, price = priceValue!!, dailyCapacity = dailyCapacity,
        depositAmount = if (returnable) depositValue else null, allowEmptySwap = returnable && swap, photoUrl = photoKey
    )

    companion object {
        val WATER_KINDS = listOf("DRINKING", "MINERAL", "CHILLED", "ALKALINE")
        val ICE_KINDS = listOf("CUBES", "CRUSHED", "BLOCK")

        fun units(c: CustomCategory): List<CustomUnit> = when (c) {
            CustomCategory.WATER -> listOf(CustomUnit.L, CustomUnit.ML)
            CustomCategory.ICE -> listOf(CustomUnit.KG, CustomUnit.PCS)
            CustomCategory.OTHER -> listOf(CustomUnit.PCS, CustomUnit.L, CustomUnit.KG)
        }

        fun defaultUnit(c: CustomCategory) = units(c).first()

        fun packagings(c: CustomCategory, u: CustomUnit?): List<Packaging> = when (c) {
            CustomCategory.WATER -> if (u == CustomUnit.ML) listOf(Packaging.SEALED)
                else listOf(Packaging.RETURNABLE, Packaging.SEALED, Packaging.TANKER, Packaging.LOOSE)
            CustomCategory.ICE -> listOf(Packaging.BAG, Packaging.LOOSE)
            CustomCategory.OTHER -> listOf(Packaging.SEALED, Packaging.LOOSE, Packaging.RETURNABLE)
        }

        fun range(u: CustomUnit, p: Packaging): ClosedRange<BigDecimal> = when {
            p == Packaging.TANKER -> BigDecimal("500")..BigDecimal("20000")
            u == CustomUnit.KG -> BigDecimal("0.1")..BigDecimal("50")
            u == CustomUnit.L -> BigDecimal("0.1")..BigDecimal("25")
            u == CustomUnit.ML -> BigDecimal("100")..BigDecimal("5000")
            else -> BigDecimal("1")..BigDecimal("500")
        }

        fun unitLabel(u: CustomUnit, v: BigDecimal) = when (u) {
            CustomUnit.L -> "L"; CustomUnit.ML -> "ml"; CustomUnit.KG -> "kg"
            CustomUnit.PCS -> if (v.compareTo(BigDecimal.ONE) == 0) "pc" else "pcs"
        }

        fun plain(v: BigDecimal): String = v.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

        /** Same wording the server suggests: "Ice cubes · 1 kg bag", "Mineral water · 500 ml × 24". */
        fun defaultName(c: CustomCategory, kind: String?, u: CustomUnit, v: BigDecimal, p: Packaging, pack: Int?): String {
            val what = when (c) {
                CustomCategory.WATER -> when (kind?.uppercase()) {
                    "MINERAL" -> "Mineral water"; "CHILLED" -> "Chilled water"; "ALKALINE" -> "Alkaline water"
                    else -> "Drinking water"
                }
                CustomCategory.ICE -> when (kind?.uppercase()) { "CRUSHED" -> "Crushed ice"; "BLOCK" -> "Ice block"; else -> "Ice cubes" }
                CustomCategory.OTHER -> kind?.trim()?.lowercase()?.replaceFirstChar { it.uppercase() }?.ifBlank { null } ?: "Item"
            }
            val size = "${plain(v)} ${unitLabel(u, v)}" + if (pack != null && pack > 1) " × $pack" else ""
            val suffix = when (p) {
                Packaging.RETURNABLE -> " jar"; Packaging.BAG -> " bag"; Packaging.TANKER -> " tanker"
                Packaging.SEALED -> if (pack != null && pack > 1) "" else " bottle"; Packaging.LOOSE -> " refill"
            }
            return "$what · $size$suffix"
        }

        fun from(p: ShopProduct): CustomProductForm? {
            val c = p.custom ?: return null
            return CustomProductForm(
                category = runCatching { CustomCategory.valueOf(c.category) }.getOrNull(), kind = c.kind,
                unit = runCatching { CustomUnit.valueOf(c.unit) }.getOrNull(), size = plain(c.size),
                packaging = runCatching { Packaging.valueOf(c.packaging) }.getOrNull(), packCount = c.packCount ?: 1,
                name = c.name.orEmpty(), price = plain(p.price), dailyCapacity = p.dailyCapacity,
                swap = p.allowTransferAndReturn, deposit = p.depositAmount?.let(::plain).orEmpty(), photoKey = p.photoUrl
            )
        }
    }
}

/** Picking a category resets what no longer fits; the next steps get sensible defaults. */
fun CustomProductForm.withCategory(c: CustomCategory): CustomProductForm {
    val u = CustomProductForm.defaultUnit(c)
    val p = CustomProductForm.packagings(c, u).first()
    return copy(category = c, kind = when (c) { CustomCategory.WATER -> "DRINKING"; CustomCategory.ICE -> "CUBES"; else -> null },
        unit = u, packaging = p, packCount = 1, size = if (c == CustomCategory.ICE) "1" else "")
}

fun CustomProductForm.withUnit(u: CustomUnit): CustomProductForm {
    val options = category?.let { CustomProductForm.packagings(it, u) } ?: emptyList()
    return copy(unit = u, packaging = packaging?.takeIf { it in options } ?: options.firstOrNull())
}
