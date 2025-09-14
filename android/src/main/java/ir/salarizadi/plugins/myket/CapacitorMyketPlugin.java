package ir.salarizadi.plugins.myket;

import android.util.Log;

import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ir.myket.billingclient.BuildConfig;
import ir.myket.billingclient.IabHelper;
import ir.myket.billingclient.util.Purchase;
import ir.myket.billingclient.util.SkuDetails;

/**
 * Capacitor plugin wrapping Myket IAB for unified in-app billing.
 * All public methods are exposed to JavaScript via @PluginMethod.
 */
@CapacitorPlugin(name = "Myket")
public class CapacitorMyketPlugin extends Plugin {

    private static final String TAG = "MyketPlugin";
    private IabHelper helper;
    private boolean setupDone = false;

    private static final String PURCHASE_BEGIN = "PURCHASE_BEGIN";
    private static final String FAILED_TO_BEGIN = "FAILED_TO_BEGIN";
    private static final String PURCHASED = "PURCHASED";
    private static final String CANCELLED = "CANCELLED";
    private static final String FAILED = "FAILED";
    private static final String CONSUMED = "CONSUMED";

    /* ---------------------------------------------------------- */
    /*                         Lifecycle                          */
    /* ---------------------------------------------------------- */

    @PluginMethod
    public void initialize (PluginCall call) {
        if (setupDone) {
            call.resolve(new JSObject().put("message", "Already initialized"));
            return;
        }

        String rsaKey = call.getString("rsaPublicKey", "");
        if (rsaKey.isEmpty()) {
            call.reject("RSA public key is required");
            return;
        }

        helper = new IabHelper(getContext(), rsaKey);
        helper.enableDebugLogging(BuildConfig.DEBUG);

        helper.startSetup(result -> {
            if (!result.isSuccess()) {
                Log.e(TAG, "Setup error: " + result);
                call.reject("Setup failed: " + result.getMessage(), result.getResponse() + "");
                return;
            }
            setupDone = true;
            Log.d(TAG, "Setup successful");
            call.resolve(new JSObject().put("connected", true));
        });
    }

    /* ---------------------------------------------------------- */
    /*                       Purchase flow                        */
    /* ---------------------------------------------------------- */

    @PluginMethod
    public void purchaseProduct (PluginCall call) {
        if (!setupDone) {
            call.reject("Billing not initialized");
            return;
        }

        String productId = call.getString("productId");
        String type = call.getString("type", "inapp");
        String payload = call.getString("payload", "");

        if (productId == null || productId.isEmpty()) {
            call.reject("productId is required");
            return;
        }

        call.setKeepAlive(true);

        JSObject response = new JSObject();
        response.put("productId", productId);
        response.put("type", type);
        response.put("payload", payload);

        try {
            notifyPurchaseState(PURCHASE_BEGIN, response);

            helper.launchPurchaseFlow(
                    getActivity(),
                    productId,
                    type,
                    (result, purchaseInfo) -> {
                        if (result.isSuccess()) {
                            JSObject purchase = new JSObject();
                            purchase.put("orderId", purchaseInfo.getOrderId());
                            purchase.put("packageName", purchaseInfo.getPackageName());
                            purchase.put("productId", purchaseInfo.getSku());
                            purchase.put("purchaseTime", purchaseInfo.getPurchaseTime());
                            purchase.put("purchaseState", purchaseInfo.getPurchaseState());
                            purchase.put("purchaseToken", purchaseInfo.getToken());
                            purchase.put("payload", purchaseInfo.getDeveloperPayload());
                            purchase.put("signature", purchaseInfo.getSignature());
                            purchase.put("itemType", purchaseInfo.getItemType());

                            JSObject successData = new JSObject();
                            successData.put("purchase", purchase);
                            successData.put("state", PURCHASED);

                            notifyPurchaseState(PURCHASED, successData);
                            call.resolve(successData);
                            return;
                        }
                        notifyPurchaseState(CANCELLED, response);

                        String message = result.getMessage();
                        boolean isCancelled = message != null && message.contains("User cancelled");
                        call.reject("Purchase failed: " + message, isCancelled ? CANCELLED : FAILED);
                    },
                    payload
            );
        } catch (Exception e) {
            notifyPurchaseState(FAILED_TO_BEGIN, response);
            call.reject("Launch purchase flow error: " + e.getMessage(), e);
        }
    }

    /* ---------------------------------------------------------- */
    /*                         Consume                            */
    /* ---------------------------------------------------------- */

    @PluginMethod
    public void consumeProduct (PluginCall call) {
        if (!setupDone) {
            call.reject("Billing not initialized", FAILED);
            return;
        }

        String token = call.getString("token");
        if (token == null || token.isEmpty()) {
            call.reject("token is required", FAILED);
            return;
        }

        call.setKeepAlive(true);

        helper.queryInventoryAsync(true, new ArrayList<>(), (result, inv) -> {
            if (result.isFailure()) {
                call.reject("Query before consume failed: " + result.getMessage(), FAILED);
                return;
            }

            Purchase target = null;
            for (Purchase p : inv.getAllPurchases()) {
                if (token.equals(p.getToken())) {
                    target = p;
                    break;
                }
            }
            if (target == null) {
                call.reject("Purchase with given token not found", FAILED);
                return;
            }

            helper.consumeAsync(target, (purchase, r) -> {
                if (r.isSuccess()) {
                    JSObject data = new JSObject();
                    data.put("state", CONSUMED);
                    data.put("consumed", true);
                    call.resolve(data);
                } else {
                    call.reject("Consume failed: " + r.getMessage(), FAILED);
                }
            });
        });
    }

    /* ---------------------------------------------------------- */
    /*                Query products & purchases                  */
    /* ---------------------------------------------------------- */

    @PluginMethod
    public void getProducts (PluginCall call) {
        if (!setupDone) {
            call.reject("Billing not initialized");
            return;
        }

        JSArray skusArr = call.getArray("skus");
        List<String> skus = new ArrayList<>();
        if (skusArr != null) {
            try {
                for (int i = 0; i < skusArr.length(); i++) skus.add(skusArr.getString(i));
            } catch (JSONException ignore) {
            }
        }

        helper.queryInventoryAsync(true, skus, (result, inv) -> {
            if (result.isFailure()) {
                call.reject("Query failed: " + result.getMessage(), result.getResponse() + "");
                return;
            }

            JSArray products = new JSArray();
            for (String sku : skus) {
                SkuDetails d = inv.getSkuDetails(sku);
                if (d != null) products.put(skuToJson(d));
            }

            call.resolve(new JSObject().put("products", products));
        });
    }

    /* ---------------------------------------------------------- */
    /*                   Connection state                         */
    /* ---------------------------------------------------------- */

    @PluginMethod
    public void getConnectionState (PluginCall call) {
        JSObject res = new JSObject();
        res.put("state", setupDone ? "CONNECTED" : "NOT_INITIALIZED");
        call.resolve(res);
    }

    /* ---------------------------------------------------------- */
    /*                        Disconnect                          */
    /* ---------------------------------------------------------- */

    @PluginMethod
    public void disconnect (PluginCall call) {
        if (helper != null) {
            helper.dispose();
            helper = null;
            setupDone = false;
        }
        call.resolve(new JSObject().put("disconnected", true));
    }

    /* ---------------------------------------------------------- */
    /*                     Internal helpers                       */
    /* ---------------------------------------------------------- */

    private void notifyPurchaseState (String state, JSObject data) {
        JSObject result = new JSObject();
        result.put("state", state);
        if (data != null) {
            for (Iterator<String> it = data.keys(); it.hasNext(); ) {
                String key = it.next();
                result.put(key, data.opt(key));
            }
        }
        notifyListeners("purchaseStateChanged", result);
    }

    private JSObject purchaseToJson (Purchase p) {
        JSObject o = new JSObject();
        o.put("orderId", p.getOrderId());
        o.put("packageName", p.getPackageName());
        o.put("productId", p.getSku());
        o.put("purchaseTime", p.getPurchaseTime());
        o.put("purchaseState", p.getPurchaseState());
        o.put("purchaseToken", p.getToken());
        o.put("developerPayload", p.getDeveloperPayload());
        o.put("signature", p.getSignature());
        o.put("itemType", p.getItemType());
        return o;
    }

    private JSObject skuToJson (SkuDetails d) {
        JSObject o = new JSObject();
        o.put("sku", d.getSku());
        o.put("type", d.getType());
        o.put("price", d.getPrice());
        o.put("title", d.getTitle());
        o.put("description", d.getDescription());
        return o;
    }

    @Override
    protected void handleOnDestroy () {
        if (helper != null) {
            helper.dispose();
            helper = null;
            setupDone = false;
        }
        super.handleOnDestroy();
    }

}