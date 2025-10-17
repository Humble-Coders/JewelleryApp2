// REPLACE the entire RatesDialog.kt file content with this:

package com.example.jewelleryapp.screen.homeScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jewelleryapp.model.GoldSilverRates
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RatesDialog(
    rates: GoldSilverRates?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // Debug logging
    LaunchedEffect(rates, isLoading) {
        android.util.Log.d("RatesDialog", "State - isLoading: $isLoading, rates: $rates")
        android.util.Log.d("RatesDialog", "Gold rate: ${rates?.goldRatePerGram}, Silver rate: ${rates?.silverRatePerGram}")
    }

    Dialog(
        onDismissRequest = {
            isVisible = false
            // Delay to allow animation to complete
            kotlinx.coroutines.GlobalScope.launch {
                delay(200)
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glassmorphism background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .blur(0.5.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    Color.White.copy(alpha = 0.7f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    // Glassmorphism content
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // Header with glassmorphism effect
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Animated icon background
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        color = Color(0xFF896C6C).copy(alpha = 0.1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AttachMoney,
                                                contentDescription = "Rates",
                                                tint = Color(0xFF896C6C),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Today's Rates",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF896C6C)
                                        )
                                        Text(
                                            text = "Live precious metal prices",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Modern close button
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            isVisible = false
                                            kotlinx.coroutines.GlobalScope.launch {
                                                delay(200)
                                                onDismiss()
                                            }
                                        },
                                    shape = CircleShape,
                                    color = Color.Gray.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isLoading) {
                                ModernLoadingState()
                            } else if (rates != null && (rates.goldRatePerGram > 0 || rates.silverRatePerGram > 0)) {
                                // Animated rate cards
                                ModernRateCard(
                                    title = "Gold",
                                    rate = rates.goldRatePerGram,
                                    currency = rates.currency,
                                    changePercentage = rates.rateChangePercentage["gold"] ?: "0%",
                                    cardColor = Color(0xFF896C6C),
                                    delay = 100L
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ModernRateCard(
                                    title = "Silver",
                                    rate = rates.silverRatePerGram,
                                    currency = rates.currency,
                                    changePercentage = rates.rateChangePercentage["silver"] ?: "0%",
                                    cardColor = Color(0xFFC0C0C0),
                                    delay = 200L
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Last updated with modern design
                                LastUpdatedSection(rates.lastUpdated)
                            } else {
                                ErrorState(onRetry = onRetry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernRateCard(
    title: String,
    rate: Double,
    currency: String,
    changePercentage: String,
    cardColor: Color,
    delay: Long = 0L
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(500))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardColor.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box {
                // Gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    cardColor.copy(alpha = 0.1f),
                                    cardColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Metal indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(cardColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$title (per gram)",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Animated rate display
                        AnimatedRateDisplay(rate = rate)
                    }

                    // Change indicator with modern design
                    ChangeIndicator(changePercentage = changePercentage)
                }
            }
        }
    }
}

@Composable
private fun AnimatedRateDisplay(rate: Double) {
    var animatedRate by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(rate) {
        val animation = TargetBasedAnimation<Float, AnimationVector1D>(
            animationSpec = tween(1000, easing = FastOutSlowInEasing),
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = rate.toFloat()
        )

        val startTime = withFrameNanos { it }
        do {
            val playTime = withFrameNanos { it } - startTime
            animatedRate = animation.getValueFromNanos(playTime)
        } while (!animation.isFinishedFromNanos(playTime))
    }

    Text(
        text = "₹${animatedRate.toInt()}",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}

@Composable
private fun ChangeIndicator(changePercentage: String) {
    val isPositive = changePercentage.startsWith("+")
    val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = changeColor.copy(alpha = 0.1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp
                    else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = if (isPositive) "Up" else "Down",
                    tint = changeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = changePercentage,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
            }
            Text(
                text = "vs yesterday",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LastUpdatedSection(lastUpdated: Long) {
    val lastUpdatedText = if (lastUpdated > 0) {
        val date = Date(lastUpdated)
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        formatter.format(date)
    } else {
        "Unknown"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Gray.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Last Updated",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Last updated",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = lastUpdatedText,
                    fontSize = 13.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModernLoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Animated loading rings
        val infiniteTransition = rememberInfiniteTransition(label = "loading")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing)
            ),
            label = "rotation"
        )

        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(60.dp)
                    .rotate(rotationAngle),
                color = Color(0xFF896C6C),
                strokeWidth = 3.dp
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .rotate(-rotationAngle),
                color = Color(0xFF896C6C),
                strokeWidth = 2.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fetching latest rates...",
            fontSize = 16.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Unable to load rates",
            color = Color.Red,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "Please check your connection and try again",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Retry button
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF896C6C)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Retry",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry", fontSize = 14.sp)
        }
    }
}

// Keep the existing RatesDrawerItem function unchanged
@Composable
fun RatesDrawerItem(
    goldRate: Double?,
    silverRate: Double?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val amberColor = Color(0xFF896C6C)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = amberColor.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                amberColor.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = "Rates",
                            tint = amberColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Today's Rates",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View Details",
                    tint = amberColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = amberColor,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Gold Rate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFFFD700), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gold",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (goldRate != null) "₹${goldRate.toInt()}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "per gram",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(amberColor.copy(alpha = 0.3f))
                    )
                    
                    // Silver Rate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFC0C0C0), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Silver",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (silverRate != null) "₹${silverRate.toInt()}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "per gram",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}