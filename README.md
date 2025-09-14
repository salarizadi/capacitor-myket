# @salarizadi/capacitor-myket

[![npm version](https://img.shields.io/npm/v/@salarizadi/capacitor-myket-billing?color=brightgreen&label=npm)](https://www.npmjs.com/package/@salarizadi/capacitor-myket)
[![Downloads](https://img.shields.io/npm/dt/@salarizadi/capacitor-myket-billing)](https://www.npmjs.com/package/@salarizadi/capacitor-myket)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Capacitor 6](https://img.shields.io/badge/Capacitor-6+-blue?logo=capacitor)](https://capacitorjs.com)

> A zero-config Capacitor plugin for **Myket** (Iranian Android Market) in-app purchases, powered by the official Myket IAB SDK.

---

## 🚀 Features

| | |
|-|-|
| ✅ **Initialize once** – auto-reconnects on demand | ✅ **Promise + event** hybrid API |
| ✅ **Consumable & non-consumable** products | ✅ **RSA signature verification** |
| ✅ **Query prices, titles, descriptions** | ✅ **TypeScript definitions** included |

---

## 📦 Installation

```bash
npm install @salarizadi/capacitor-myket
npx cap sync android
```

---

## 🔐 Prerequisites

1. Upload your APK to [**Myket Developer Console**](https://developer.myket.ir/) (alpha/beta/production).
2. Copy your **RSA public key** from *Developer Console → Services & APIs*.

---

## 🧑‍💻 Usage

```javascript
import { Myket } from '@salarizadi/capacitor-myket';

// OR
// const { Myket } = window.Capacitor.Plugins;

// 1️⃣ One-time setup
await Myket.initialize({ rsaPublicKey: 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...' });

// 2️⃣ Listen to purchase life-cycle
Myket.addListener('purchaseStateChanged', ({ state, purchase }) => {
  console.log(`State: ${state}`, purchase);
});

// 3️⃣ Fetch product catalog
const { products } = await Myket.getProducts({ skus: ['coin100', 'premium'] });
console.log(products); // [{ sku, title, price, description, type }]

// 4️⃣ Purchase
const { purchase } = await Myket.purchaseProduct({
  productId: 'coin100',
  type: 'inapp',        // 'inapp' | 'subs'
  payload: 'user123'    // optional developer payload
});

// 5️⃣ Consume (for consumables)
await Myket.consumeProduct({ token: purchase.purchaseToken });
```

---

## 📖 API

| Method | Params | Returns | Description |
|--------|--------|---------|-------------|
| `initialize` | `{ rsaPublicKey: string }` | `Promise<{ connected: boolean }>` | Must be called first. |
| `getConnectionState` | — | `Promise<{ state: 'CONNECTED' \| 'NOT_INITIALIZED' }>` | Diagnostic helper. |
| `getProducts` | `{ skus: string[] }` | `Promise<{ products: SkuDetails[] }>` | Load titles, prices, etc. |
| `purchaseProduct` | `PurchaseRequest` | `Promise<{ purchase: Purchase }>` | Launches Myket purchase flow. |
| `consumeProduct` | `{ token: string }` | `Promise<{ consumed: true }>` | Marks item as consumed. |
| `disconnect` | — | `Promise<{ disconnected: true }>` | Closes helper & frees memory. |

---

## 📡 Events

| Event | Payload |
|-------|---------|
| `purchaseStateChanged` | `{ state: PurchaseState, purchase?: Purchase }` |

Purchase states:  
`PURCHASE_BEGIN`, `PURCHASED`, `CANCELLED`, `FAILED`, `CONSUMED`, `FAILED_TO_BEGIN`

---

## 🧰 Troubleshooting

| Symptom | Fix |
|---------|-----|
| `Billing not initialized` | Call `initialize()` before any billing method. |
| `-1003: Signature verification failed` | Double-check RSA public key. |
| `User cancelled` | Normal when back button pressed; handle gracefully. |
| `Item not found` | Product ID must exist in console and be active. |

---

## 📄 License

MIT © [Salar Izadi](https://github.com/salarizadi)

---

## 🔗 Links

| | |
|-|-|
| Repository | [github.com/salarizadi/capacitor-myket](https://github.com/salarizadi/capacitor-myket) |
| Myket Console | [developer.myket.ir](https://developer.myket.ir) |
| Capacitor | [capacitorjs.com](https://capacitorjs.com) |
