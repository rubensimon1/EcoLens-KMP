package com.rubensimon.ecolens.ui.screens.features

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubensimon.ecolens.data.models.items.Coupon
import com.rubensimon.ecolens.data.repository.UserRepository
import com.rubensimon.ecolens.ui.components.*
import com.rubensimon.ecolens.utils.PointsManager
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

/**
 * Tienda de recompensas — migrada de RewardsActivity.
 *
 * Carga cupones desde Supabase o muestra ejemplos de fallback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(onBackClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var coupons by remember { mutableStateOf<List<Coupon>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    // Usar puntos reales de PointsManager
    var puntos by remember { mutableIntStateOf(PointsManager.getPoints()) }
    var snackbarMessage by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var redeemedCoupons by remember { mutableStateOf<List<Coupon>>(emptyList()) }

    LaunchedEffect(Unit) {
        val dbCoupons = UserRepository().getCouponsFromDb()
        coupons = dbCoupons.ifEmpty { getSampleCoupons() }

        val userId = UserRepository().getCurrentSessionUserId()
        if (userId != null) {
            PointsManager.loadFromSupabase(userId)
            puntos = PointsManager.getPoints()

            val dbRedemptions = UserRepository().getRedemptions(userId)
            redeemedCoupons = dbRedemptions.map { r ->
                val matchingCoupon = coupons.find { it.id == r.cupon_id }
                Coupon(
                    id = r.cupon_id,
                    title = matchingCoupon?.title ?: "Cupón Canjeado",
                    pointsCost = matchingCoupon?.pointsCost ?: 0,
                    createdAt = r.fechaCanje
                )
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("🎁 Recompensas", color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = EcoColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EcoColors.BackgroundDark,
                    titleContentColor = EcoColors.TextPrimary,
                    navigationIconContentColor = EcoColors.TextPrimary
                )
            )
        },
        containerColor = EcoColors.BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Puntos disponibles
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tus puntos disponibles", color = EcoColors.TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "⭐ $puntos pts",
                            color = EcoColors.GlassAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                    Box(
                        modifier = Modifier.size(44.dp).background(EcoColors.GlassGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CardGiftcard, null, tint = EcoColors.GlassAccent)
                    }
                }
            }

            if (snackbarMessage.isNotEmpty()) {
                Surface(
                    color = EcoColors.GlassGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = snackbarMessage,
                        color = EcoColors.Success,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = EcoColors.TextPrimary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        height = 3.dp,
                        color = EcoColors.GlassAccent
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Tienda", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium) },
                    selectedContentColor = EcoColors.GlassAccent,
                    unselectedContentColor = EcoColors.TextSecondary
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Mis Cupones", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium) },
                    selectedContentColor = EcoColors.GlassAccent,
                    unselectedContentColor = EcoColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EcoColors.GlassAccent)
                }
            } else {
                if (selectedTabIndex == 0) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
                    ) {
                        items(coupons) { coupon ->
                            var isRedeeming by remember { mutableStateOf(false) }
                            CouponCard(
                                coupon = coupon,
                                currentPoints = puntos,
                                onRedeem = {
                                     if (puntos >= coupon.pointsCost && !isRedeeming) {
                                         scope.launch {
                                             isRedeeming = true
                                             val userId = UserRepository().getCurrentSessionUserId()
                                             if (userId == null) {
                                                 snackbarMessage = "❌ Sesión no válida. Por favor, reidentifícate."
                                                 isRedeeming = false
                                                 return@launch
                                             }
                                             
                                             val now = Clock.System.now()
                                             val expiration = now.plus(30, DateTimeUnit.DAY, TimeZone.UTC)
                                             
                                             val redemption = com.rubensimon.ecolens.data.models.social.RedemptionModel(
                                                 user_id = userId,
                                                 cupon_id = coupon.id,
                                                 tienda_id = coupon.tiendaId,
                                                 codigo_qr = "VAL-" + now.toEpochMilliseconds().toString().takeLast(6),
                                                 fechaCanje = now.toString(),
                                                 fechaUso = now.toString(),
                                                 fechaExpiracion = expiration.toString()
                                             )
                                             
                                             val success = UserRepository().redeemCoupon(redemption)
                                             if (success) {
                                                 // Descontar puntos de verdad
                                                 val subSuccess = PointsManager.subtractPoints(coupon.pointsCost)
                                                 if (subSuccess) {
                                                     puntos = PointsManager.getPoints()
                                                     redeemedCoupons = listOf(coupon) + redeemedCoupons
                                                     snackbarMessage = "✅ ¡${coupon.title} canjeado!"
                                                 } else {
                                                     snackbarMessage = "❌ Error al descontar puntos locales"
                                                 }
                                             } else {
                                                 snackbarMessage = "❌ Error en el servidor al canjear. Inténtalo de nuevo."
                                             }
                                             isRedeeming = false
                                         }
                                     }
                                 }
                            )
                        }
                    }
                } else {
                    if (redeemedCoupons.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.History, null, tint = EcoColors.CardPrimary, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Aún no tienes cupones", color = EcoColors.TextSecondary, textAlign = TextAlign.Center)
                        }
                    } else {
                        val groupedRedemptions = remember(redeemedCoupons) {
                            redeemedCoupons.groupBy { 
                                it.createdAt?.substringBefore("T") ?: "Reciente" 
                            }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp)
                        ) {
                            groupedRedemptions.forEach { (date, coupons) ->
                                item {
                                    Text(
                                        text = date,
                                        color = EcoColors.GlassAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                
                                items(coupons) { coupon ->
                                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(coupon.title, color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text("Válido en tiendas colaboradoras", color = EcoColors.TextSecondary, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            PlatformQRCodeView(
                                                content = "ECOLENS:${coupon.id}:${com.rubensimon.ecolens.utils.TimeUtils.getCurrentTimestamp()}",
                                                modifier = Modifier.size(160.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Muestra este código para validar", color = EcoColors.GlassAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            GlassButton(
                                                onClick = {
                                                    snackbarMessage = "✅ Cupón validado con éxito"
                                                    redeemedCoupons = redeemedCoupons.filter { it.id != coupon.id }
                                                },
                                                containerColor = EcoColors.Success,
                                                modifier = Modifier.fillMaxWidth().height(44.dp)
                                            ) {
                                                Text("Validar Canje")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponCard(
    coupon: Coupon,
    currentPoints: Int,
    onRedeem: () -> Unit
) {
    val canRedeem = currentPoints >= coupon.pointsCost
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(EcoColors.CardPrimary, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CardGiftcard, null, tint = EcoColors.GlassAccent.copy(alpha = 0.5f))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(coupon.title, color = EcoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (coupon.description.isNotBlank()) {
                    Text(coupon.description, color = EcoColors.TextSecondary, fontSize = 11.sp, maxLines = 1)
                }
                Text("${coupon.pointsCost} pts", color = EcoColors.GlassAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            GlassButton(
                onClick = onRedeem,
                enabled = canRedeem,
                containerColor = if (canRedeem) EcoColors.GlassAccent else EcoColors.CardPrimary,
                contentColor = if (canRedeem) Color.White else EcoColors.TextSecondary,
                modifier = Modifier.height(36.dp).width(80.dp)
            ) {
                Text(
                    text = if (canRedeem) "Canjear" else "Faltan",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getSampleCoupons() = listOf(
    Coupon(id = "709f4f39-4658-49bb-be40-124a942f91df", title = "1 día de alquiler", description = "Bicicleta eléctrica incluida", pointsCost = 80, category = "transporte"),
    Coupon(id = "835c679a-bf8d-4c53-b464-76f816e94cb5", title = "10% en accesorios", description = "Descuento en accesorios de móvil y PC", pointsCost = 200, category = "electrónica"),
    Coupon(id = "b1e1b1e1-b1e1-b1e1-b1e1-b1e1b1e1b1e1", title = "10% descuento", description = "En cualquier libro", pointsCost = 60, category = "comercio"),
    Coupon(id = "c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1", title = "Pizza Mediana Gratis", description = "Cualquier ingrediente", pointsCost = 150, category = "restaurante"),
    Coupon(id = "d1d1d1d1-d1d1-d1d1-d1d1-d1d1d1d1d1d1", title = "Café + Croissant", description = "Desayuno completo gratis", pointsCost = 50, category = "cafetería"),
    Coupon(id = "e1e1e1e1-e1e1-e1e1-e1e1-e1e1e1e1e1e1", title = "5€ de descuento", description = "En compras superiores a 20€", pointsCost = 100, category = "supermercado"),
)
