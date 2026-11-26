package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.ui.components.CardDeckStyle
import com.example.ui.components.TableBackgroundStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

enum class SubscriptionPlan(
    val productId: String,
    val title: String,
    val periodLabel: String,
    val defaultPriceText: String,
    val badge: String,
    val description: String,
) {
    WEEKLY(
        productId = "shaddaha_plus_weekly",
        title = "الاشتراك الأسبوعي",
        periodLabel = "أسبوعياً",
        defaultPriceText = "9.99 ريال / أسبوعياً",
        badge = "تجربة سريعة",
        description = "إنشاء جلسات غير محدود يومياً + فتح جميع ألوان الورق ومزايا شدها بلس لمدة أسبوع.",
    ),
    MONTHLY(
        productId = "shaddaha_plus_monthly",
        title = "الاشتراك الشهري",
        periodLabel = "شهرياً",
        defaultPriceText = "29 ريال / شهرياً",
        badge = "الأكثر طلباً",
        description = "إنشاء جلسات غير محدود يومياً + فتح جميع ألوان الورق وشارة المشترك لمدة شهر كامل.",
    ),
    YEARLY(
        productId = "shaddaha_plus_yearly",
        title = "الاشتراك السنوي",
        periodLabel = "سنوياً",
        defaultPriceText = "149 ريال / سنوياً",
        badge = "أفضل توفير",
        description = "إنشاء جلسات غير محدود طوال العام + جميع مزايا شدها بلس وألوان الورق بأفضل سعر سنوي.",
    ),
}

object ShaddahaProducts {
    const val SUBSCRIPTION_WEEKLY_ID = "shaddaha_plus_weekly"
    const val SUBSCRIPTION_PLUS_ID = "shaddaha_plus_monthly"
    const val SUBSCRIPTION_YEARLY_ID = "shaddaha_plus_yearly"
    const val SUBSCRIPTION_DEFAULT_PRICE = "29 ريال / شهرياً"

    val SUBSCRIPTION_PRODUCT_IDS: Map<SubscriptionPlan, String> = mapOf(
        SubscriptionPlan.WEEKLY to SUBSCRIPTION_WEEKLY_ID,
        SubscriptionPlan.MONTHLY to SUBSCRIPTION_PLUS_ID,
        SubscriptionPlan.YEARLY to SUBSCRIPTION_YEARLY_ID,
    )

    val PRODUCT_TO_SUBSCRIPTION: Map<String, SubscriptionPlan> =
        SUBSCRIPTION_PRODUCT_IDS.entries.associate { (plan, id) -> id to plan }

    val DECK_PRODUCT_IDS: Map<CardDeckStyle, String> = mapOf(
        CardDeckStyle.DIAMOND to "deck_diamond_20",
        CardDeckStyle.GOLD to "deck_gold_10",
        CardDeckStyle.SILVER to "deck_silver_7",
        CardDeckStyle.BRONZE to "deck_bronze_5",
    )

    val PRODUCT_TO_DECK: Map<String, CardDeckStyle> =
        DECK_PRODUCT_IDS.entries.associate { (deck, id) -> id to deck }

    val BACKGROUND_PRODUCT_IDS: Map<TableBackgroundStyle, String> = mapOf(
        TableBackgroundStyle.SAUDI_GOLD to "bg_saudi_gold_20",
        TableBackgroundStyle.SAUDI_ROYAL_DARK_GOLD to "bg_saudi_royal_20",
        TableBackgroundStyle.SAUDI_GREEN to "bg_saudi_green_20",
    )

    val PRODUCT_TO_BACKGROUND: Map<String, TableBackgroundStyle> =
        BACKGROUND_PRODUCT_IDS.entries.associate { (bg, id) -> id to bg }
}

data class BillingUiSnapshot(
    val isPlayReady: Boolean = false,
    val isCatalogLoaded: Boolean = false,
    val subscriptionPrice: String = ShaddahaProducts.SUBSCRIPTION_DEFAULT_PRICE,
    val subscriptionPrices: Map<SubscriptionPlan, String> = SubscriptionPlan.entries.associateWith { it.defaultPriceText },
    val activeSubscriptionPlanFromPlay: SubscriptionPlan? = null,
    val deckPrices: Map<CardDeckStyle, String> = emptyMap(),
    val backgroundPrices: Map<TableBackgroundStyle, String> = emptyMap(),
    val ownedDecksFromPlay: Set<CardDeckStyle> = setOf(CardDeckStyle.CLASSIC),
    val ownedBackgroundsFromPlay: Set<TableBackgroundStyle> = TableBackgroundStyle.freeBackgrounds.toSet(),
    val isSubscribedFromPlay: Boolean = false,
    val statusMessage: String = "الدفع عبر Google Play Billing (يرتبط بحساب التاجر عند النشر)",
) {
    fun priceFor(plan: SubscriptionPlan): String = subscriptionPrices[plan] ?: plan.defaultPriceText
}

sealed interface BillingLaunchResult {
    data object LaunchedPlaySheet : BillingLaunchResult
    data class PreviewFallback(val reason: String) : BillingLaunchResult
}

class PlayBillingManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _snapshot = MutableStateFlow(BillingUiSnapshot())
    val snapshot: StateFlow<BillingUiSnapshot> = _snapshot.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val purchaseEvents: SharedFlow<String> = _purchaseEvents.asSharedFlow()

    private val cachedProducts = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    scope.launch { processPurchases(purchases, notifyNewUnlock = true) }
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                _purchaseEvents.tryEmit("تم إلغاء عملية الشراء من Google Play")
            }
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch { refreshExistingPurchases() }
                _purchaseEvents.tryEmit("هذا العنصر مملوك مسبقاً في حسابك على Google Play — جاري الاستعادة")
            }
            else -> {
                Log.w(TAG, "Billing error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                _purchaseEvents.tryEmit("تعذر إتمام الدفع عبر Google Play (${billingResult.responseCode})")
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun connectAndSync() {
        if (billingClient.isReady) {
            scope.launch {
                queryCatalog()
                refreshExistingPurchases()
            }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    _snapshot.update {
                        it.copy(
                            isPlayReady = true,
                            statusMessage = "متصل بخدمة Google Play Billing",
                        )
                    }
                    scope.launch {
                        queryCatalog()
                        refreshExistingPurchases()
                    }
                } else {
                    Log.w(TAG, "Billing setup failed (${billingResult.responseCode}): ${billingResult.debugMessage}")
                    _snapshot.update {
                        it.copy(
                            isPlayReady = false,
                            statusMessage = "معاينة محلية: لم تتوفر خدمات Google Play الكاملة على هذا الجهاز",
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _snapshot.update { it.copy(isPlayReady = false) }
            }
        })
    }

    fun launchSubscriptionPurchase(
        activity: Activity?,
        plan: SubscriptionPlan,
        userId: String?,
    ): BillingLaunchResult {
        val priceText = _snapshot.value.priceFor(plan)
        if (activity == null || !billingClient.isReady) {
            return BillingLaunchResult.PreviewFallback(
                "خدمة Google Play غير متصلة في هذه المعاينة — تم تفعيل ${plan.title} للمعاينة ($priceText)",
            )
        }
        val productDetails = cachedProducts[plan.productId]
            ?: return BillingLaunchResult.PreviewFallback(
                "معرف الاشتراك (${plan.productId}) غير منشور في Play Console بعد — تم تفعيل ${plan.title} للمعاينة ($priceText)",
            )
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingLaunchResult.PreviewFallback(
                "لا توجد خطة اشتراك مفعلة في Play Console لهذا المعرف بعد",
            )

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
        hashAccountId(userId)?.let { flowBuilder.setObfuscatedAccountId(it) }

        val result = billingClient.launchBillingFlow(activity, flowBuilder.build())
        return if (result.responseCode == BillingResponseCode.OK) {
            BillingLaunchResult.LaunchedPlaySheet
        } else {
            BillingLaunchResult.PreviewFallback("تعذر فتح نافذة Google Play (${result.responseCode})")
        }
    }

    fun launchDeckPurchase(
        activity: Activity?,
        deck: CardDeckStyle,
        userId: String?,
    ): BillingLaunchResult {
        val productId = ShaddahaProducts.DECK_PRODUCT_IDS[deck]
            ?: return BillingLaunchResult.PreviewFallback("الورق الكلاسيكي مجاني")

        if (activity == null || !billingClient.isReady) {
            return BillingLaunchResult.PreviewFallback(
                "خدمة Google Play غير متصلة في هذه المعاينة — تم تفعيل ${deck.title} للمعاينة",
            )
        }
        val productDetails = cachedProducts[productId]
            ?: return BillingLaunchResult.PreviewFallback(
                "المنتج ($productId) غير منشور في Play Console بعد — تم تفعيل ${deck.title} للمعاينة",
            )

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        val offerToken = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
            ?: productDetails.oneTimePurchaseOfferDetails?.offerToken
        if (!offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
        hashAccountId(userId)?.let { flowBuilder.setObfuscatedAccountId(it) }

        val result = billingClient.launchBillingFlow(activity, flowBuilder.build())
        return if (result.responseCode == BillingResponseCode.OK) {
            BillingLaunchResult.LaunchedPlaySheet
        } else {
            BillingLaunchResult.PreviewFallback("تعذر فتح نافذة Google Play (${result.responseCode})")
        }
    }

    fun launchBackgroundPurchase(
        activity: Activity?,
        background: TableBackgroundStyle,
        userId: String?,
    ): BillingLaunchResult {
        val productId = ShaddahaProducts.BACKGROUND_PRODUCT_IDS[background]
            ?: return BillingLaunchResult.PreviewFallback("هذه الخلفية مجانية")

        if (activity == null || !billingClient.isReady) {
            return BillingLaunchResult.PreviewFallback(
                "خدمة Google Play غير متصلة في هذه المعاينة — تم تفعيل ${background.title} للمعاينة (${background.priceLabel})",
            )
        }
        val productDetails = cachedProducts[productId]
            ?: return BillingLaunchResult.PreviewFallback(
                "المنتج ($productId) غير منشور في Play Console بعد — تم تفعيل ${background.title} للمعاينة (${background.priceLabel})",
            )

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        val offerToken = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
            ?: productDetails.oneTimePurchaseOfferDetails?.offerToken
        if (!offerToken.isNullOrBlank()) {
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
        hashAccountId(userId)?.let { flowBuilder.setObfuscatedAccountId(it) }

        val result = billingClient.launchBillingFlow(activity, flowBuilder.build())
        return if (result.responseCode == BillingResponseCode.OK) {
            BillingLaunchResult.LaunchedPlaySheet
        } else {
            BillingLaunchResult.PreviewFallback("تعذر فتح نافذة Google Play (${result.responseCode})")
        }
    }

    private suspend fun queryCatalog() {
        val subProducts = SubscriptionPlan.entries.map { plan ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(plan.productId)
                .setProductType(ProductType.SUBS)
                .build()
        }
        val subParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subProducts)
            .build()

        val allInAppSkus = ShaddahaProducts.DECK_PRODUCT_IDS.values +
            ShaddahaProducts.BACKGROUND_PRODUCT_IDS.values
        val inAppProducts = allInAppSkus.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(ProductType.INAPP)
                .build()
        }
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProducts)
            .build()

        val subResult = withContext(Dispatchers.IO) { billingClient.queryProductDetails(subParams) }
        val inAppResult = withContext(Dispatchers.IO) { billingClient.queryProductDetails(inAppParams) }

        val subPrices = SubscriptionPlan.entries.associateWith { it.defaultPriceText }.toMutableMap()
        if (subResult.billingResult.responseCode == BillingResponseCode.OK) {
            subResult.productDetailsList?.forEach { details ->
                cachedProducts[details.productId] = details
                val plan = ShaddahaProducts.PRODUCT_TO_SUBSCRIPTION[details.productId]
                if (plan != null) {
                    val formatted = details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.firstOrNull()
                        ?.formattedPrice
                    if (!formatted.isNullOrBlank()) {
                        subPrices[plan] = "$formatted / ${plan.periodLabel}"
                    }
                }
            }
        }

        val deckPrices = mutableMapOf<CardDeckStyle, String>()
        val backgroundPrices = mutableMapOf<TableBackgroundStyle, String>()
        if (inAppResult.billingResult.responseCode == BillingResponseCode.OK) {
            inAppResult.productDetailsList?.forEach { details ->
                cachedProducts[details.productId] = details
                val formatted = details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                    ?: details.oneTimePurchaseOfferDetails?.formattedPrice
                val deck = ShaddahaProducts.PRODUCT_TO_DECK[details.productId]
                if (deck != null && !formatted.isNullOrBlank()) {
                    deckPrices[deck] = formatted
                }
                val bg = ShaddahaProducts.PRODUCT_TO_BACKGROUND[details.productId]
                if (bg != null && !formatted.isNullOrBlank()) {
                    backgroundPrices[bg] = formatted
                }
            }
        }

        val hasStoreCatalog = cachedProducts.isNotEmpty()
        _snapshot.update { current ->
            current.copy(
                isCatalogLoaded = hasStoreCatalog,
                subscriptionPrice = subPrices[SubscriptionPlan.MONTHLY] ?: ShaddahaProducts.SUBSCRIPTION_DEFAULT_PRICE,
                subscriptionPrices = subPrices,
                deckPrices = deckPrices,
                backgroundPrices = backgroundPrices,
                statusMessage = if (hasStoreCatalog) {
                    "مربوط بمنتجات Google Play Console (التحصيل عبر محفظة التاجر)"
                } else {
                    "وضع التجربة: أنشئ معرفات المنتجات في Google Play Console لتفعيل التحصيل الفعلي"
                },
            )
        }
    }

    suspend fun refreshExistingPurchases() {
        if (!billingClient.isReady) return
        val subsResult = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.SUBS)
                    .build(),
            )
        }
        val inAppResult = withContext(Dispatchers.IO) {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .build(),
            )
        }

        val allPurchases = buildList {
            if (subsResult.billingResult.responseCode == BillingResponseCode.OK) {
                addAll(subsResult.purchasesList)
            }
            if (inAppResult.billingResult.responseCode == BillingResponseCode.OK) {
                addAll(inAppResult.purchasesList)
            }
        }
        if (allPurchases.isNotEmpty()) {
            processPurchases(allPurchases, notifyNewUnlock = false)
        }
    }

    private suspend fun processPurchases(
        purchases: List<Purchase>,
        notifyNewUnlock: Boolean,
    ) {
        val unlockedDecks = mutableSetOf(CardDeckStyle.CLASSIC)
        val unlockedBackgrounds = TableBackgroundStyle.freeBackgrounds.toMutableSet()
        var hasSub = false
        var activeSubPlan: SubscriptionPlan? = null

        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        val ackResult = withContext(Dispatchers.IO) {
                            billingClient.acknowledgePurchase(ackParams)
                        }
                        if (ackResult.responseCode != BillingResponseCode.OK) {
                            Log.w(TAG, "Acknowledge failed: ${ackResult.responseCode} ${ackResult.debugMessage}")
                        }
                    }
                    for (productId in purchase.products) {
                        val subPlan = ShaddahaProducts.PRODUCT_TO_SUBSCRIPTION[productId]
                        if (subPlan != null) {
                            hasSub = true
                            activeSubPlan = subPlan
                            unlockedDecks.addAll(CardDeckStyle.entries)
                        } else {
                            ShaddahaProducts.PRODUCT_TO_DECK[productId]?.let { deck ->
                                unlockedDecks.add(deck)
                            }
                            ShaddahaProducts.PRODUCT_TO_BACKGROUND[productId]?.let { bg ->
                                unlockedBackgrounds.add(bg)
                            }
                        }
                    }
                    if (notifyNewUnlock) {
                        _purchaseEvents.tryEmit("تم تأكيد الشراء عبر Google Play وتفعيل المزايا في حسابك")
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    if (notifyNewUnlock) {
                        _purchaseEvents.tryEmit("عملية الدفع قيد المعالجة عبر Google Play — سيتم التفعيل فور التأكيد")
                    }
                }
                else -> Unit
            }
        }

        _snapshot.update { current ->
            current.copy(
                ownedDecksFromPlay = current.ownedDecksFromPlay + unlockedDecks,
                ownedBackgroundsFromPlay = current.ownedBackgroundsFromPlay + unlockedBackgrounds,
                isSubscribedFromPlay = current.isSubscribedFromPlay || hasSub,
                activeSubscriptionPlanFromPlay = activeSubPlan ?: current.activeSubscriptionPlanFromPlay,
            )
        }
    }

    fun release() {
        scope.cancel()
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun hashAccountId(userId: String?): String? {
        if (userId.isNullOrBlank()) return null
        val digest = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(64)
    }

    private companion object {
        const val TAG = "PlayBillingManager"
    }
}
