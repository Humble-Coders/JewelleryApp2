package com.humblecoders.jewelleryapp.screen.orderHistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humblecoders.jewelleryapp.model.Order
import com.humblecoders.jewelleryapp.model.OrderItem
import com.humblecoders.jewelleryapp.screen.homeScreen.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel,
    navController: NavController
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val isLoadingBalance by viewModel.isLoadingBalance.collectAsState()
    
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    
    val goldColor = Color(0xFF896C6C)
    val lightBackground = Color(0xFFF8F6F4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (selectedOrder != null) "Order Details" else "Order History",
                        color = goldColor
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedOrder != null) {
                                selectedOrder = null
                            } else {
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = goldColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = { 
            if (selectedOrder == null) {
                BottomNavigationBar(navController) 
            }
        },
        containerColor = lightBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = goldColor)
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = error ?: "An error occurred",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshOrders() },
                                colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                selectedOrder != null -> {
                    OrderDetailView(
                        order = selectedOrder!!,
                        goldColor = goldColor
                    )
                }
                orders.isEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            BalanceCard(
                                balance = balance,
                                isLoading = isLoadingBalance,
                                goldColor = goldColor
                            )
                        }
                        item {
                            EmptyOrdersView(goldColor = goldColor)
                        }
                    }
                }
                else -> {
                    OrderList(
                        orders = orders,
                        balance = balance,
                        isLoadingBalance = isLoadingBalance,
                        onOrderClick = { order ->
                            selectedOrder = order
                        },
                        goldColor = goldColor
                    )
                }
            }
        }
    }
}

@Composable
fun OrderList(
    orders: List<Order>,
    balance: Double?,
    isLoadingBalance: Boolean,
    onOrderClick: (Order) -> Unit,
    goldColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            BalanceCard(
                balance = balance,
                isLoading = isLoadingBalance,
                goldColor = goldColor
            )
        }
        items(orders) { order ->
            OrderCard(
                order = order,
                onClick = { onOrderClick(order) },
                goldColor = goldColor
            )
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    goldColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.id.takeLast(8)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = order.getFormattedDate(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = order.getFormattedTotal(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = goldColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = order.status, goldColor = goldColor)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} item${if (order.items.size != 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = goldColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    goldColor: Color
) {
    val statusColor = when (status.uppercase()) {
        "CONFIRMED" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFF9800)
        "CANCELLED" -> Color(0xFFF44336)
        else -> Color.Gray
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusColor.copy(alpha = 0.1f)
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OrderDetailView(
    order: Order,
    goldColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Order Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Order Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DetailRow(label = "Order ID", value = order.id)
                    DetailRow(label = "Date", value = order.getFormattedDate())
                    DetailRow(label = "Status", value = order.status) {
                        StatusBadge(status = order.status, goldColor = goldColor)
                    }
                    DetailRow(label = "Payment Method", value = order.paymentMethod)
                }
            }
        }
        
        item {
            // Items Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Items (${order.items.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    order.items.forEachIndexed { index, item ->
                        OrderItemRow(item = item, goldColor = goldColor)
                        if (index < order.items.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        
        item {
            // Price Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Price Breakdown",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PriceRow(label = "Subtotal", amount = order.subtotal)
                    if (order.discountAmount > 0) {
                        PriceRow(label = "Discount", amount = -order.discountAmount, isDiscount = true)
                    }
                    if (order.gstAmount > 0) {
                        PriceRow(
                            label = if (order.isGstIncluded) "GST (Included)" else "GST",
                            amount = order.gstAmount
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = goldColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = order.getFormattedTotal(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        if (valueContent != null) {
            valueContent()
        } else {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
fun OrderItemRow(
    item: OrderItem,
    goldColor: Color
) {
    Column {
        Text(
            text = item.productName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Qty: ${item.quantity}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (item.weight > 0) {
                    Text(
                        text = "Weight: ${item.weight}g",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Text(
                text = "₹${String.format("%,.2f", item.price)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = goldColor
            )
        }
    }
}

@Composable
fun PriceRow(
    label: String,
    amount: Double,
    isDiscount: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = if (isDiscount) "-₹${String.format("%,.2f", amount)}" else "₹${String.format("%,.2f", amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDiscount) Color(0xFF4CAF50) else Color.Black
        )
    }
}

@Composable
fun BalanceCard(
    balance: Double?,
    isLoading: Boolean,
    goldColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "Account Balance",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = goldColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (balance != null) {
                            val formattedBalance = if (balance >= 0) {
                                "₹${String.format("%,.2f", balance)}"
                            } else {
                                "-₹${String.format("%,.2f", -balance)}"
                            }
                            formattedBalance
                        } else {
                            "₹0.00"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (balance != null && balance < 0) {
                            Color(0xFFF44336) // Red for negative balance
                        } else {
                            goldColor // Gold for positive balance
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyOrdersView(goldColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Orders Yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your order history will appear here",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
