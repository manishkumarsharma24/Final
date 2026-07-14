package com.dispatch.java.features.sealedclasses;

public sealed class PaymentMethod permits CreditCard, PayPal,Crypto {

}
