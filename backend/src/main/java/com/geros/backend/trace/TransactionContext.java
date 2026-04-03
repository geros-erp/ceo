package com.geros.backend.trace;

public final class TransactionContext {

    public static final String HEADER_NAME = "X-Transaction-Id";
    public static final String REQUEST_ATTRIBUTE = "transactionId";

    private static final ThreadLocal<String> CURRENT_TRANSACTION = new ThreadLocal<>();

    private TransactionContext() {
    }

    public static void setCurrentTransactionId(String transactionId) {
        CURRENT_TRANSACTION.set(transactionId);
    }

    public static String getCurrentTransactionId() {
        return CURRENT_TRANSACTION.get();
    }

    public static void clear() {
        CURRENT_TRANSACTION.remove();
    }
}
