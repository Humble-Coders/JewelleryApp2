package com.example.jewelleryapp.screen.homeScreen

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.jewelleryapp.model.GoldSilverRates
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RatesDialog(
    rates: GoldSilverRates?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Rates",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB78628)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFB78628))
                    }
                } else if (rates != null) {
                    // Gold Rate Card
                    RateCard(
                        title = "Gold",
                        rate = rates.goldRatePerGram,
                        currency = rates.currency,
                        changePercentage = rates.rateChangePercentage["gold"] ?: "0%",
                        cardColor = Color(0xFFFFD700).copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Silver Rate Card
                    RateCard(
                        title = "Silver",
                        rate = rates.silverRatePerGram,
                        currency = rates.currency,
                        changePercentage = rates.rateChangePercentage["silver"] ?: "0%",
                        cardColor = Color(0xFFC0C0C0).copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Last Updated
                    val lastUpdatedText = if (rates.lastUpdated > 0) {
                        val date = Date(rates.lastUpdated)
                        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                        "Last updated: ${formatter.format(date)}"
                    } else {
                        "Last updated: Unknown"
                    }

                    Text(
                        text = lastUpdatedText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Unable to load rates",
                            color = Color.Red,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RateCard(
    title: String,
    rate: Double,
    currency: String,
    changePercentage: String,
    cardColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$title (per gram)",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "₹${rate.toInt()}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                val isPositive = changePercentage.startsWith("+")
                val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = if (isPositive) "Up" else "Down",
                        tint = changeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = changePercentage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = changeColor
                    )
                }
                Text(
                    text = "vs yesterday",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// Add this new composable to HomeScreen.kt

@Composable
fun RatesDrawerItem(
    goldRate: Double?,
    silverRate: Double?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB78628).copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachMoney,
                        contentDescription = "Rates",
                        tint = Color(0xFFB78628),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Today's Rates",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View Details",
                    tint = Color(0xFFB78628),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFB78628),
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Gold Rate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFFD700), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gold",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = if (goldRate != null) "₹${goldRate.toInt()}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    // Silver Rate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFC0C0C0), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Silver",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = if (silverRate != null) "₹${silverRate.toInt()}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}