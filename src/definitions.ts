/* ---------------------------------------------------------- */
/*    TypeScript definitions for the Capacitor Myket plugin   */
/* ---------------------------------------------------------- */

export interface MyketPlugin {
    /* ---------------------------------------------------------- */
    /*  Lifecycle                                                 */
    /* ---------------------------------------------------------- */

    /**
     * Initialise the Myket billing client.
     * Must be called before any other billing API.
     */
    initialize(options: { rsaKey: string }): Promise<{ message: string }>;

    /* ---------------------------------------------------------- */
    /*  Purchase flow                                             */
    /* ---------------------------------------------------------- */

    /**
     * Launch the purchase flow for a single product.
     * The returned promise resolves when the purchase finishes successfully.
     * Purchase state changes are also emitted via the `purchaseStateChanged` listener.
     */
    purchaseProduct(options: {
        productId: string;
        type?: 'inapp' | 'subs'; // default: 'inapp'
        payload?: string; // optional developer payload
    }): Promise<MyketPurchase>;

    /* ---------------------------------------------------------- */
    /*  Consume                                                   */

    /* ---------------------------------------------------------- */

    /**
     * Consume (acknowledge) a managed purchase so it can be bought again.
     * Requires the purchase token returned in the original purchase.
     */
    consumeProduct(options: { token: string }): Promise<{ state: 'CONSUMED', consumed: true }>;

    /* ---------------------------------------------------------- */
    /*  Query products & purchases                                */
    /* ---------------------------------------------------------- */

    /**
     * Query local product details (title, price, description, ...) for the given SKUs.
     * Only SKUs previously configured in Myket console are returned.
     */
    getProducts(options: { skus: string[] }): Promise<{ products: MyketSkuDetails[] }>;

    /**
     * Return all purchases currently owned by the user (non-consumed).
     */
    getPurchaseInfo(): Promise<{ purchases: MyketPurchase[] }>;

    /* ---------------------------------------------------------- */
    /*  Connection state                                          */
    /* ---------------------------------------------------------- */

    /**
     * Quick way to check whether the billing client is ready.
     */
    getConnectionState(): Promise<{ state: 'CONNECTED' | 'NOT_INITIALIZED' }>;

    /* ---------------------------------------------------------- */
    /*  Disconnect                                                */
    /* ---------------------------------------------------------- */

    /**
     * Release resources and disconnect from Myket billing service.
     * After calling this you must `initialize()` again before any billing API.
     */
    disconnect(): Promise<{ disconnected: true }>;

    /* ---------------------------------------------------------- */
    /*  Events                                                    */
    /* ---------------------------------------------------------- */

    /**
     * Listen to purchase state changes:
     *  - PURCHASE_BEGIN   : purchase flow started
     *  - FAILED_TO_BEGIN  : technical error before flow opened
     *  - PURCHASED        : user successfully purchased
     *  - CANCELLED        : user cancelled or payment failed
     */
    addListener(
        eventName: 'purchaseStateChanged',
        listenerFunc: (data: PurchaseStateChange) => void
    ): Promise<PluginListenerHandle>;

    removeAllListeners(): Promise<void>;
}

/* ---------------------------------------------------------- */
/*  Types                                                     */
/* ---------------------------------------------------------- */

export interface MyketPurchase {
    orderId: string;
    packageName: string;
    productId: string; // SKU
    purchaseTime: number; // epoch ms
    purchaseState: number; // 0 = purchased, 1 = cancelled, 2 = refunded
    purchaseToken: string;
    developerPayload?: string;
    signature: string;
    itemType: 'inapp' | 'subs';
}

export interface MyketSkuDetails {
    sku: string;
    type: 'inapp' | 'subs';
    price: string; // formatted price in user currency
    title: string;
    description: string;

    // any extra fields returned by Myket (json) are also present
    [key: string]: any;
}

export interface PurchaseStateChange {
    state: 'PURCHASE_BEGIN' | 'FAILED_TO_BEGIN' | 'PURCHASED' | 'CANCELLED' | 'FAILED';
    productId?: string;
    type?: string;
    payload?: string;

    // when state === 'PURCHASED' the full purchase object is spread here
    [key: string]: any;
}

/* ---------------------------------------------------------- */
/*  Capacitor registration helper                             */
/* ---------------------------------------------------------- */

import type {PluginListenerHandle} from '@capacitor/core';

declare module '@capacitor/core' {
    interface PluginRegistry {
        Myket: MyketPlugin;
    }
}