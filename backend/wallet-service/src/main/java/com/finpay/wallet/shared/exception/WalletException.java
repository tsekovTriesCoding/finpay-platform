package com.finpay.wallet.shared.exception;

public class WalletException extends RuntimeException {
    public WalletException(String message) { super(message); }
    public WalletException(String message, Throwable cause) { super(message, cause); }
}
