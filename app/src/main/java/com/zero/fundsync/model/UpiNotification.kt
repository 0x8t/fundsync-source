package com.zero.fundsync.model

data class UpiNotification(
    val amount: String,
    val sender: String,
    val timestamp: Long,
    val donationId: String? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) 