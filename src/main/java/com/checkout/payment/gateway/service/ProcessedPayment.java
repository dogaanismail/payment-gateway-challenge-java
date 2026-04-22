package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.PostPaymentResponse;

public record ProcessedPayment(PostPaymentResponse response, boolean replayed) {
}

