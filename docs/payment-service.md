# Payment service: how online payments work in AgriScan

This page explains, in plain words, how a farmer pays online in AgriScan: what we used, how we used it and where the
code is. You do not need to know Stripe or Razorpay before reading it.

---

## 1. In one minute

- A farmer can pay online for two things: an **Agro Store order** and a **rental booking** (a tractor, a tempo…).
- The payment page offers **three ways to pay**: **UPI**, **Card**, and **Scan & Pay** (section 5).
- UPI and card money is handled by a **payment provider**, a company that moves money safely: **Stripe** or
  **Razorpay**. For development there is a third, fake one: the **Simulated** provider. No real money moves with it.
- With **Scan & Pay** the farmer pays the **seller directly**: the shop's or vehicle owner's own UPI QR (PhonePe,
  Google Pay…). The money goes straight to the seller's bank, and the seller confirms in their portal when it arrives.
- **The app never decides the amount from the browser.** The server always works out the amount from the order or
  booking.
- After the farmer pays, the server **double-checks with the provider** (or, for Scan & Pay, waits for the seller)
  before it marks anything as paid.
- Every change of a payment (opened, paid, failed…) is also written to **MongoDB**, as a history log.

### The three ways to pay

| Way | What the farmer does | Who confirms it |
|---|---|---|
| **UPI** | Pays with PhonePe, Google Pay, Paytm… through the provider | The provider |
| **Card** | Types a debit or credit card. The form checks the number, expiry and CVV while they type, with a message under each field | The provider |
| **Scan & Pay** | Scans the seller's QR, pays in their UPI app, then types the 12-digit UPI reference | The seller (shopkeeper or vehicle owner) |

---

## 2. The three providers

| Provider | What it is | When it is used | Real money? |
|---|---|---|---|
| **Simulated** | Our own fake checkout: a card or UPI form, and a button to make the payment fail | When no Stripe or Razorpay keys are set (this is the normal case on a laptop) | No |
| **Stripe** | International payment company. The farmer types card details into Stripe's own box on our page | When the Stripe keys are set | Yes (or test money with test keys) |
| **Razorpay** | Indian payment company. Opens its own window with UPI, cards, net banking and wallets | When the Razorpay keys are set, and Stripe's are not | Yes (or test money with test keys) |

### Which provider is used?

The setting `agriscan.payments.provider` decides it. Its default is `auto`:

1. Stripe keys set? Use **Stripe**.
2. Otherwise, Razorpay keys set? Use **Razorpay**.
3. Otherwise use **Simulated**.

You can also force one with the environment variable `AGRISCAN_PAYMENTS_PROVIDER=stripe|razorpay|simulated`.
If you force Stripe or Razorpay but forget its keys, online payment is switched off (the API answers *503: online
payment is not available*). It does **not** quietly fall back to fake payments.

When the backend starts, it prints which provider it uses, for example:
`Online payments go to SIMULATED`.

Scan & Pay does not use a provider at all, so it works the same whichever provider is chosen.

---

## 3. Libraries we used

### Backend (Java, Spring Boot)

| Library | Version | What we use it for |
|---|---|---|
| `com.stripe:stripe-java` | 33.4.2 | Talk to Stripe: create a *PaymentIntent*, read it back, check webhook signatures |
| `com.razorpay:razorpay-java` | 1.4.10 | Talk to Razorpay: create a Razorpay *order* |
| `spring-boot-starter-data-mongodb` (MongoDB Java driver 5.8.1) | Spring Boot 4.1.1 | Write the payment history log to MongoDB |
| `spring-boot-starter-data-jpa` + H2 | Spring Boot 4.1.1 | Store payments, orders and bookings in the main (SQL) database |
| `com.google.zxing:core` (ZXing) | 3.5.3 | Draw the Scan & Pay QR code (a PNG image) from the seller's UPI ID, with the amount filled in |
| Java's built-in `javax.crypto.Mac` | JDK 17 | Check Razorpay signatures (HMAC-SHA256) |

### Frontend (React)

| Library | How we load it | What we use it for |
|---|---|---|
| **Stripe.js** (`https://js.stripe.com/v3/`) | Loaded from Stripe's website only when a Stripe checkout opens | Shows Stripe's secure card box (the *Payment Element*) |
| **Razorpay Checkout** (`https://checkout.razorpay.com/v1/checkout.js`) | Loaded from Razorpay's website only when a Razorpay checkout opens | Opens Razorpay's payment window |

We did **not** add any npm package for payments. Both providers require their script to come from their own
website (that is part of their security), so we load it on demand with a tiny helper (`load-script.ts`).

---

## 4. The journey of one payment, step by step

Example: Sunita buys fertilizer worth ₹730 and pays online.

```
 Farmer (browser)              AgriScan backend                Provider (Stripe / Razorpay / Simulated)
 ────────────────              ────────────────                ───────────────────────────────────────
 1. Cart → "Pay online"  ───►  creates the order (AWAITING_PAYMENT)
 2.                      ───►  POST /api/payments
                                 works out the amount (₹730)
                                 opens a payment  ─────────────►  "a payment of ₹730 is coming"
                                                  ◄─────────────  gives back its id (pi_… / order_… / sim_…)
 3. Payment page shows the provider's checkout
 4. Farmer pays          ──────────────────────────────────────►  takes the money
 5.                      ───►  POST /api/payments/{id}/confirm
                                 asks the provider "is it really paid?" ──►
                                 marks payment SUCCEEDED, order PAID
                                 sends a notification, writes to MongoDB
 6. Result page: "Payment successful"
                               (later) webhook from provider ◄───────────  "payment captured" (a second, independent signal)
```

In words:

1. **Checkout.** In the cart the farmer picks *Pay online* (or *Cash on delivery*, which needs no payment at all).
   The order is saved as **AWAITING_PAYMENT**.
2. **Open the payment.** The browser calls `POST /api/payments` with *what* to pay: `{purpose: STORE_ORDER,
   referenceId: 12}`. It never sends an amount. The server asks the store (or rentals) code how much is due, then
   opens the payment at the provider.
3. **Pay.** The payment page (`/pay/:paymentId`) has three tabs, **UPI**, **Card** and **Scan & Pay**, and shows the
   right checkout:
   - Simulated: our own UPI form or card form (both check what is typed), with a *Simulate a failed payment* link.
   - Stripe: Stripe's card box, inside our page.
   - Razorpay: Razorpay's window, on top of our page, opened on UPI or card as the farmer chose.
   - Scan & Pay: the seller's QR, whatever the provider (next section).
4. **Confirm.** When the provider says "done", the browser calls `POST /api/payments/{id}/confirm`. The server
   **does not trust the browser**: it asks Stripe again, or checks Razorpay's signature, before it accepts the payment.
5. **Settle.** If it is paid, the server:
   - marks the payment **SUCCEEDED**,
   - marks the order **PAID** (or the booking **PAID**, and tells the vehicle owner),
   - sends the farmer a notification ("Payment received"),
   - writes the change to MongoDB.
6. **Result.** The result page (`/pay/:paymentId/result`) shows *successful*, *failed* (with the reason and *Try
   again*), or *processing*. While the bank is still processing, it checks again every 2 seconds for about 30 seconds.

**If it fails**, nothing is lost: the order stays *AWAITING_PAYMENT*. The farmer can press *Try again*, or pay later
from **My Orders** with *Pay ₹X now*. A new payment is opened each time.

**Webhooks** (step 6, bottom line): the provider also calls our server directly. If the farmer closes the browser
right after paying, the webhook still marks the payment as paid. Whichever signal comes first (confirm or webhook) wins;
the second one changes nothing. The payment is counted **exactly once**.

---

## 5. Scan & Pay: paying the seller directly

Shopkeepers and vehicle owners can take money **straight into their own bank account**. They add their UPI ID and/or
a photo of their PhonePe, Google Pay or bank QR in their portal. The farmer scans it and pays; AgriScan never touches
the money. That is why the **seller** says whether it arrived, not a provider.

Example: Sunita buys fertilizer worth ₹280 from *Solapur Mandi Agro Depot* and chooses **Scan & Pay**.

```
 Farmer (Sunita)                  AgriScan backend                          Shopkeeper (Sanjay)
 ───────────────                  ────────────────                          ───────────────────
 1. Payment page → Scan & Pay     the checkout already carries the shop's QR and UPI ID
 2. Scans the QR in PhonePe and pays ₹280 ─── money goes straight to the shop's bank ───►
 3. Types the 12-digit UPI ref ─► POST /api/payments/scan-and-pay
                                    saves the payment as PROCESSING, with the UPI ref
                                    order → VERIFYING_PAYMENT ("Payment to verify")
                                    notifies the shopkeeper ───────────────────► "Sunita says they paid you ₹280
                                                                                   (UPI ref 498765432101)"
                                                                                  checks the bank, taps Received
                                  ◄── POST /api/seller/payments/{id}/received ───
                                    payment SUCCEEDED, order PAID
 4. Notification: "Payment successful"
```

**If the money did not arrive**, the seller taps **Not received** and may add a short reason ("Not in my account").
The payment becomes FAILED, the order goes back to *Awaiting payment*, and the farmer is told why, so they can show
the seller their UPI reference or pay again.

**Which QR does the farmer see?**

- The seller's **uploaded QR**, when they added one (the photo from their PhonePe or bank app). The farmer types the
  amount in their UPI app; the page tells them how much.
- Otherwise, a QR that **AgriScan draws** from the seller's UPI ID with the **amount already filled in** (ZXing).
- Always: the UPI ID with a **Copy** button, and a **Pay ₹X in a UPI app** button. On a phone it opens PhonePe, Google
  Pay… with everything filled in (a `upi://pay?…` link).

**The rules**

- **One UPI reference pays for one payment.** The same 12 digits cannot be used twice, for any order or booking.
- **No double payments.** While a Scan & Pay payment waits for the seller, the farmer cannot start another payment for
  the same order; they can pay again once the seller says *Not received*.
- **Only the seller who was paid decides.** Another shop or owner gets *404 not found* for that payment.
- **One shop per cart.** Each shop has its own QR, so a cart holds one shop's products. Adding a product from another
  shop asks the farmer first: *Empty cart and add*, or *Keep my cart*.

**Where sellers set it up**

| Seller | Where | Where they confirm payments |
|---|---|---|
| Shopkeeper | Shop portal → **Shop** tab → *Scan & Pay* | Shop dashboard → *Payments to confirm* |
| Vehicle owner | Partner dashboard → *Scan & Pay* card → **Edit** | Partner dashboard, and the booking's page |

---

## 6. Where the code lives

### Backend: `backend/src/main/java/com/myagree/app/`

| File | What it does |
|---|---|
| `payment/PaymentController.java` | The farmer's API: open a payment, read it, confirm it, send a Scan & Pay UPI reference |
| `payment/SellerPaymentController.java` | The seller's API: Scan & Pay payments sent to them, *Received* / *Not received* |
| `payment/UpiQrCodes.java` | Makes the `upi://pay` link and draws its QR code (ZXing) |
| `payment/PaymentWebhookController.java` | Receives Stripe's and Razorpay's webhooks |
| `payment/AdminPaymentController.java` | The admin's list of all payments |
| `payment/PaymentService.java` | **The heart.** Opens, reuses, confirms and settles payments, exactly once |
| `payment/Payment.java` | One payment as stored in the SQL database (table `payment`) |
| `payment/PaymentProviders.java` | Picks the provider (the `auto` rule above) |
| `payment/SimulatedPaymentProvider.java` | The fake provider |
| `payment/StripePaymentProvider.java` | Everything Stripe: create the PaymentIntent, read it back, read webhooks |
| `payment/RazorpayPaymentProvider.java` | Everything Razorpay: create the order, check signatures, read webhooks |
| `payment/HmacSignatures.java` | Signature check helper (HMAC-SHA256, compared in constant time) |
| `payment/PaymentNotifications.java` | "Payment received" / "Payment failed" notifications |
| `payment/PaymentMetrics.java` | *Revenue this month* on the admin dashboard |
| `payment/PaymentActivity*.java` | The MongoDB history log (see section 9) |
| `store/StorePayments.java` | Tells payments how much an order costs and who the shop is, and follows the payment: *Payment to verify*, PAID, or back to unpaid |
| `rental/RentalPayments.java` | The same for rental bookings, and tells the owner |
| `store/ShopController.java`, `ShopService.java` | The shop's UPI ID and QR (plus the rest of the shop portal) |
| `rental/OwnerProfileController.java`, `VehicleOwnerService.java` | The vehicle owner's UPI ID and QR |
| `common/spi/PayableResolver.java` | The contract between payments and the store/rental code ("what is due, and to whom?") |
| `common/spi/ScanAndPayPayee.java` | The seller a Scan & Pay payment goes to: name, UPI ID, QR |
| `common/UpiId.java` | What a valid UPI ID looks like (`name@bank`) |
| `common/MongoConfig.java` | The MongoDB connection settings |

Settings: `backend/src/main/resources/application.properties` (the `agriscan.payments.*` and `agriscan.mongodb.*`
lines). Error messages in English, Marathi and Hindi: `resources/i18n/payment*.properties`.

### Frontend: `frontend/src/`

| File | What it does |
|---|---|
| `pages/store/CartSheet.tsx`, `PaymentMethodPicker.tsx` | The cart with *Pay online* / *Cash on delivery* |
| `pages/store/useCart.ts` | Places the order; for online orders goes straight to the payment page |
| `pages/payments/usePayNow.ts` | One shared "Pay now" action (used by the cart, My Orders, the result page and bookings) |
| `pages/payments/PaymentPage.tsx`, `PaymentMethodTabs.tsx` | The payment page with its *UPI / Card / Scan & Pay* tabs; picks the right checkout below |
| `pages/payments/SimulatedCardForm.tsx`, `SimulatedUpiForm.tsx` | The fake card and UPI forms, with a message under each wrong field |
| `lib/payment-checks.ts`, `hooks/useFieldErrors.ts` | The checks behind those messages: card number (Luhn), expiry, CVV, UPI ID, UPI reference |
| `pages/payments/ScanAndPayCheckout.tsx` | Scan & Pay: the seller's QR, *Copy* UPI ID, *Pay in a UPI app*, and the UPI reference box |
| `components/payments/PaymentsToConfirm.tsx` | The seller's *Payments to confirm* list with *Received* / *Not received* |
| `components/payments/ScanAndPaySettings.tsx`, `ScanAndPayCard.tsx` | Where sellers add their UPI ID and QR |
| `pages/payments/StripeCheckout.tsx` | Mounts Stripe's card box and confirms with Stripe |
| `pages/payments/RazorpayCheckout.tsx` | Opens Razorpay's window and sends its signed answer to our server |
| `pages/payments/PaymentResultPage.tsx`, `usePaymentResult.ts` | The result page, with the "check again" logic |
| `pages/payments/load-script.ts`, `payment-sdks.ts` | Loads Stripe.js / Razorpay from their websites, with their types |
| `pages/orders/OrdersPage.tsx`, `OrderCard.tsx` | My Orders, with *Pay ₹X now* for unpaid orders |
| `pages/bookings/BookingActions.tsx` | *Pay online* on a confirmed booking |
| `lib/api.ts` (`api.payments`) | The calls to the backend |
| `i18n/messages/{en,mr,hi}/payments.ts` | All payment texts in English, Marathi and Hindi |

---

## 7. The API

| Call | Who | What it does |
|---|---|---|
| `POST /api/payments` | Farmer | Open (or reopen) the payment of an order or booking. Body: `{"purpose": "STORE_ORDER", "referenceId": 12}` |
| `GET /api/payments/{id}` | Farmer | Read a payment: status, amount, description, failure reason |
| `POST /api/payments/{id}/confirm` | Farmer | "I have paid, please check." The server checks with the provider |
| `POST /api/payments/scan-and-pay` | Farmer | "I paid the seller directly." Body: `{"purpose": "STORE_ORDER", "referenceId": 12, "upiReference": "412345678901"}` |
| `GET /api/seller/payments?status=PROCESSING` | Shopkeeper, vehicle owner | The Scan & Pay payments sent to them, newest first |
| `POST /api/seller/payments/{id}/received` | Shopkeeper, vehicle owner | "The money arrived": the order or booking becomes paid |
| `POST /api/seller/payments/{id}/not-received` | Shopkeeper, vehicle owner | "It did not arrive." Optional body: `{"reason": "Not in my account"}` |
| `POST`/`DELETE /api/shop/me/upi-qr`, `/api/owner/me/upi-qr` | Shopkeeper / vehicle owner | Upload (form field `image`) or remove their QR. The UPI ID is saved with the rest of their profile |
| `POST /api/payments/webhooks/stripe` | Stripe | Stripe tells us about a payment (signed with `Stripe-Signature`) |
| `POST /api/payments/webhooks/razorpay` | Razorpay | Razorpay tells us about a payment (signed with `X-Razorpay-Signature`) |
| `GET /api/admin/payments?status=&page=&size=` | Admin | Every payment, newest first |

Payment statuses: `REQUIRES_PAYMENT` (waiting for the farmer) → `PROCESSING` (bank is working on it, or for Scan &
Pay: waiting for the seller) → `SUCCEEDED` or `FAILED`. `CANCELLED` means a newer payment replaced it.

---

## 8. Safety rules we follow

1. **The server sets the amount.** The browser only says *what* to pay for. Nobody can change the price in the browser.
2. **Never trust "it's paid" from the browser.** For Stripe the server reads the payment back from Stripe. For
   Razorpay it checks the signature `HMAC_SHA256(orderId|paymentId, keySecret)` that only Razorpay can make.
3. **Webhooks must be signed.** The webhook URLs are public, but an unsigned or wrongly signed call is rejected.
4. **Exactly once.** A payment is settled once, even if the confirm call and the webhook both arrive, or a webhook
   arrives twice. Stripe calls also carry an *idempotency key* (`agriscan-payment-{id}`), so a retried request
   never creates a second charge.
5. **Farmers only see their own payments.** Someone else's payment id answers *404 not found*. Sellers likewise only
   see and decide the Scan & Pay payments sent to them.
6. **Secrets stay on the server.** Secret keys come from environment variables and never reach the browser. The
   browser only gets the *public* key (Stripe publishable key, Razorpay key id).
7. **Scan & Pay is confirmed by whoever got the money.** The farmer's "I have paid" only starts the check; the
   payment is paid when the seller taps *Received*. A UPI reference can be used for one payment only.

---

## 9. The payment history log in MongoDB

Besides the main database, every change of every payment is copied into **MongoDB**, as a history you can read later
(for support, audits or reports). The main SQL database stays the "truth"; MongoDB is the logbook.

| What | Value |
|---|---|
| Server | `mongodb://127.0.0.1:27017` (the *MongoDB* Windows service on this computer) |
| Database | `agriscan` |
| Collection | `payment_events` |
| Schema | `backend/src/main/resources/mongodb/payment_events.schema.json` |
| Connection settings | `spring.mongodb.uri` and `agriscan.mongodb.*` in `application.properties`; code in `common/MongoConfig.java` |

**The schema.** The JSON file lists every field, its type and its allowed values. When the backend starts, it
creates the collection with this schema as a **strict validator**, so MongoDB itself refuses any document that does
not fit. It also adds two indexes: `payment_timeline` (one payment's history in order) and `farmer_activity` (a
farmer's latest activity).

**One document per change.** A payment that fails once and then succeeds leaves four documents:

```json
{ "paymentId": 1, "type": "OPENED",    "source": "CHECKOUT",     "amountRupees": 1210, "provider": "SIMULATED", "providerReference": "sim_1", ... }
{ "paymentId": 1, "type": "FAILED",    "source": "CONFIRMATION", "failureReason": "Declined in the test checkout", ... }
{ "paymentId": 2, "type": "OPENED",    "source": "CHECKOUT", ... }
{ "paymentId": 2, "type": "SUCCEEDED", "source": "CONFIRMATION", ... }
```

`source` says who reported it: `CHECKOUT` (the farmer started paying), `CONFIRMATION` (our confirm call),
`WEBHOOK` (the provider) or `SELLER` (the shopkeeper or vehicle owner answered a Scan & Pay payment, whose
`provider` is `SCAN_AND_PAY` and whose `providerReference` is the farmer's UPI reference).

**MongoDB can never break a payment.** The log is written *after* the payment is saved, on a separate background
thread. If MongoDB is stopped, payments still work normally; the backend only logs a warning, and it sets the
collection up as soon as MongoDB is back.

**Look at the data** with **MongoDB Compass** (already installed on this computer): connect to
`mongodb://127.0.0.1:27017`, open database `agriscan`, then collection `payment_events`.

> Note: on a laptop the main SQL database (H2) is emptied every time the backend restarts, so payment ids start again
> at 1. MongoDB keeps everything, so the same `paymentId` can appear for different payments from different runs.
> Sort by `occurredAt` to read it in order.

To run the backend **without** MongoDB, set `AGRISCAN_MONGODB_ENABLED=false`. To use another MongoDB (for example
MongoDB Atlas), set `AGRISCAN_MONGODB_URI` to its connection string.

---

## 10. Try it yourself (no real money)

1. Start the backend and the frontend (commands in section 12).
2. Sign in as the demo farmer: phone `9876543210`, password `Farmer@123`.
3. Agro Store → add something → **View Cart** → **Pay online** → **Pay ₹… & place order**.
4. On the payment page (Simulated):
   - **UPI**: type any UPI ID like `name@okaxis` and press **Pay**: the payment succeeds;
   - **Card**: try `4111 1111 1111 1111`, a future expiry like `12/30` and any 3-digit CVV. Wrong values (for
     example expiry `11777`, which becomes `11/77`) show a message under the field;
   - or press **Simulate a failed payment** to see the failure page and **Try again**.
5. Open **My Orders** (profile menu) to see the order as *Paid*, *Placed* (cash) or *Awaiting payment*.

**Scan & Pay**, with two browser windows (one of them private, so both can stay signed in):

1. As the farmer, pay an order with **Scan & Pay**. Type any 12 digits as the UPI reference, for example
   `412345678901`, and press **I have paid**. The order shows *Payment to verify*. (The demo QRs use `@agriscandemo`
   UPI IDs, which no bank knows, so nothing can really be paid with them.)
2. As the shopkeeper (phone `9800054321`, password `Shop@123`), the dashboard shows it under *Payments to confirm*.
   Tap **Received**: the farmer's order becomes *Paid*. Or tap **Not received** and pick a reason: the farmer is told
   why and can pay again.
3. The same works for a confirmed rental booking with the owner `9822233445` (password `Owner@123`).

---

## 11. Turning on real payments (test mode first!)

Set the keys as environment variables before starting the backend. Use **test keys** first; they move no real money.

| Provider | Environment variables | Where to get them |
|---|---|---|
| Stripe | `STRIPE_SECRET_KEY` (`sk_test_…`), `STRIPE_PUBLISHABLE_KEY` (`pk_test_…`), `STRIPE_WEBHOOK_SECRET` (`whsec_…`) | Stripe Dashboard → Developers → API keys / Webhooks |
| Razorpay | `RAZORPAY_KEY_ID` (`rzp_test_…`), `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Razorpay Dashboard → Account & Settings → API Keys / Webhooks |

Test details you can pay with in **test mode**:

- Stripe: card `4242 4242 4242 4242`, any future expiry, any CVC.
- Razorpay: UPI `success@razorpay` (or `failure@razorpay` to see a failure), or card `4111 1111 1111 1111`.

Webhooks need the provider to reach your computer:

- Stripe: install the Stripe CLI and run
  `stripe listen --forward-to localhost:8080/api/payments/webhooks/stripe`; it prints the `whsec_…` secret to use.
- Razorpay: expose port 8080 with a tunnel (for example ngrok) and add
  `https://<your-tunnel>/api/payments/webhooks/razorpay` in the Razorpay dashboard with events `payment.captured`,
  `order.paid` and `payment.failed`.

Without webhooks, payments still work: the confirm call settles them. Webhooks are the safety net.

---

## 12. How to run the project

| Part | Folder | Command (VS Code terminal, PowerShell) | Opens at |
|---|---|---|---|
| Backend | `backend` | `.\mvnw.cmd spring-boot:run` | http://localhost:8080 |
| Frontend | `frontend` | `npm run dev` (run `npm install` once first) | http://localhost:5173 |

In Git Bash, the backend command is `./mvnw spring-boot:run`. MongoDB must be running (it starts with Windows as the
*MongoDB* service); if it is not, the app still runs and only the history log is skipped.

---

## 13. Small dictionary

| Word | Meaning |
|---|---|
| **Provider** | The company that moves the money (Stripe, Razorpay) |
| **PaymentIntent** | Stripe's name for "a payment we expect" (`pi_…`) |
| **Razorpay order** | Razorpay's name for the same thing (`order_…`) |
| **Webhook** | The provider calling *our* server to say what happened to a payment |
| **Signature (HMAC)** | A code made with a secret key; it proves a message really comes from the provider and was not changed |
| **Idempotent** | Doing it twice has the same effect as doing it once (no double charge, no double "paid") |
| **Paise** | 1 rupee = 100 paise; Stripe and Razorpay count amounts in paise |
| **UPI ID** | A bank account's UPI address, like `name@okaxis` |
| **UPI QR** | A QR code holding a UPI ID (and sometimes an amount); any UPI app can scan it and pay |
| **UPI reference (UTR)** | The 12-digit number a UPI app shows after paying ("UPI Ref No."); the seller finds the payment in their bank by it |
| **Scan & Pay** | AgriScan's name for paying the seller's own UPI QR, confirmed by the seller |
| **Schema validator** | MongoDB's rule list for a collection; documents that break the rules are refused |
