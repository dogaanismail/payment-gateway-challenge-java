# 🧪 Testing Report

A breakdown of every automated test in this repo plus the live end-to-end
scenarios run against the gateway and the Mountebank bank simulator.

- **Total automated tests:** 64
- **Failures / errors:** 0
- **Last full run:** 2026-04-23 (`./gradlew test` → BUILD SUCCESSFUL)
- **Last live verification:** 2026-04-23 (eight curl scenarios, all green — see §3)

Reproduce locally:

```bash
./gradlew test                       # full unit + integration suite
docker compose up -d && ./gradlew bootRun   # then run the curl scenarios in §3
```

---

## 1. Test inventory by suite

### `model.PostPaymentRequestValidationTest` — 24 tests
Bean-Validation matrix for the inbound DTO. Asserts every field’s constraint and
the cross-field expiry-in-the-future check fire correctly.

| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldHaveNoViolationsForValidRequest` | Happy path baseline |
| 2-5 | `shouldRejectInvalidCardNumbers` (`@ValueSource` ×4) | empty / too short / too long / non-numeric PAN |
| 6-9 | `shouldRejectInvalidExpiryMonth` (×4) | 0, 13, -1, 99 |
| 10 | `shouldRejectExpiryInThePast` | Cross-field check |
| 11-15 | `shouldRejectUnsupportedCurrencies` (×5) | `JPY`, `gbp` (case), `EU`, `EUROS`, empty |
| 16-18 | `shouldRejectNonPositiveAmount` (×3) | 0, -1, -100 |
| 19-23 | `shouldRejectInvalidCvv` (×5) | empty / too short / too long / letters / mixed |
| 24 | `shouldNotExposeCardNumberOrCvvInToString` | PII guarantee on `@ToString` |

### `mapper.PaymentMapperTest` — 4 tests
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldCopyAllFieldsAndFormatExpiryWhenMappingToBankAuthorizationRequest` | DTO → bank request, expiry formatted `MM/YYYY` |
| 2 | `shouldPadSingleDigitMonthWhenFormattingExpiryDate` | `4` → `04` |
| 3 | `shouldMapAllFieldsAndDeriveLastFourWhenBuildingPaymentResponse` | Response builder copies + derives `last4` |
| 4 | `shouldNeverExposeFullCardNumberInPaymentResponse` | PII guarantee on response model |

### `client.BankAuthorizationRequestTest` — 1 test
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldNotExposeCardNumberOrCvvInToString` | PII guarantee on the bank-bound DTO |

### `idempotency.RequestFingerprinterTest` — 3 tests
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldProduceIdenticalFingerprintForIdenticalRequests` | Deterministic hash |
| 2 | `shouldProduceDifferentFingerprintWhenAmountChanges` | Field sensitivity |
| 3 | `shouldProduceDifferentFingerprintWhenCardNumberChanges` | Field sensitivity |

### `idempotency.InMemoryIdempotencyStoreTest` — 4 tests
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldExecuteSupplierAndReturnFreshResultOnFirstCall` | First call invokes supplier exactly once |
| 2 | `shouldReplayCachedResponseAndNotInvokeSupplierOnSecondCallWithSameFingerprint` | Replay path, supplier not called again |
| 3 | `shouldThrowWhenSameKeyIsReusedWithDifferentFingerprint` | Same key + different body → `IdempotencyKeyReuseException` |
| 4 | `shouldNotCacheWhenSupplierThrows` | Failed payments are *not* cached — retry-with-same-key still works |

### `idempotency.InMemoryIdempotencyStoreConcurrencyTest` — 1 test
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldOnlyInvokeSupplierOnceWhenManyThreadsUseSameKeyConcurrently` | Atomic check-then-store under contention; the supplier counter is `1` even with N parallel callers using the same key |

### `client.ResilientAcquiringBankClientTest` — 4 tests
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldRetryOnBankServiceUnavailableUpToMaxAttempts` | 3 attempts on 503 (verified via `Mockito.times(3)`) |
| 2 | `shouldNotRetryOnGenericAcquiringBankException` | Non-503 errors fail fast (`times(1)`) |
| 3 | `shouldReturnSuccessfullyWithoutRetryOnSuccess` | Happy path: one bank call, no retries |
| 4 | `shouldFastFailWithBankUnavailableWhenCircuitBreakerOpens` | Trips the breaker, then asserts subsequent call short-circuits with `BankServiceUnavailableException` (delegate not invoked) |

### `web.RequestIdFilterTest` — 6 tests
| # | Method | What it pins down |
|---|---|---|
| 1 | `shouldEchoIncomingRequestIdHeader` | Honor caller-supplied `X-Request-Id` |
| 2 | `shouldGenerateRequestIdWhenHeaderIsAbsent` | Generate UUID when missing |
| 3 | `shouldGenerateRequestIdWhenHeaderIsBlank` | Treat blank as missing |
| 4 | `shouldEchoIncomingCorrelationIdHeader` | Honor caller-supplied `X-Correlation-Id` |
| 5 | `shouldFallBackCorrelationIdToRequestIdWhenAbsent` | If no correlation id → use request id |
| 6 | `shouldGenerateBothIdsWhenNeitherHeaderIsProvided` | Both generated and equal |

### `controller.PaymentGatewayControllerTest` — 17 tests (`@SpringBootTest` + MockMvc)
| # | Method | Layer asserted |
|---|---|---|
| 1 | `shouldReturnPaymentWhenIdExists` | GET happy path |
| 2 | `shouldReturnNotFoundWhenPaymentIdDoesNotExist` | GET → 404 |
| 3 | `shouldReturnAuthorizedResponseWhenBankAuthorizesPayment` | POST → 201 Authorized |
| 4 | `shouldReturnDeclinedResponseWhenBankDeclinesPayment` | POST → 201 Declined |
| 5 | `shouldReturnRejectedResponseWhenRequestIsInvalid` | Bean validation → 400 with field list |
| 6 | `shouldReturnBadGatewayWhenAcquiringBankIsUnavailable` | Generic bank failure → 502 |
| 7 | `shouldReturnBadRequestWhenPaymentIdIsNotAUuid` | Path-var type mismatch → 400 |
| 8 | `shouldReturnRejectedWhenJsonBodyIsMalformed` | Unparseable JSON → 400 |
| 9 | `shouldReturnUnsupportedMediaTypeWhenContentTypeIsNotJson` | → 415 |
| 10 | `shouldReturnMethodNotAllowedWhenHttpMethodIsNotSupported` | → 405 |
| 11 | `shouldReplayResponseWhenSameIdempotencyKeyIsReused` | Same key → bank called **once** (`times(1)`), `Idempotent-Replayed: true` |
| 12 | `shouldReturnUnprocessableEntityWhenIdempotencyKeyIsReusedWithDifferentBody` | Same key + different body → 422 |
| 13 | `shouldEchoIncomingRequestIdHeader` | End-to-end through filter chain |
| 14 | `shouldGenerateRequestIdWhenHeaderIsAbsent` | End-to-end through filter chain |
| 15 | `shouldReturnServiceUnavailableWhenAcquiringBankReturns503` | Bank 503 → clean 503 + `Retry-After: 30` |
| 16 | `shouldNeverReturnFullCardNumberOrCvvAndShouldExposeOnlyLastFour` | Response JSON contains no PAN, no CVV — only `card_number_last_four` |
| 17 | `shouldNotPersistOrReturnCvvWhenFetchingPaymentById` | POST → GET round-trip — fetched payload contains no PAN, no CVV |

---

## 2. Coverage map (challenge phases ↔ tests)

| Phase | Requirement | Where it’s proven |
|---|---|---|
| 1 — REST API | POST/GET happy paths | controller #1, #3, #4 |
| 1 — REST API | Validation matrix | validation suite (24), controller #5/#7/#8/#9/#10 |
| 2 — Bank integration | Map merchant DTO ↔ bank DTO | mapper suite (4), `BankAuthorizationRequestTest` |
| 3 — Idempotency | Atomic `computeIfAbsent` | `InMemoryIdempotencyStoreTest`, **concurrency** test |
| 3 — Idempotency | Bank called once on duplicate key | controller #11 (`Mockito.times(1)`) |
| 3 — Idempotency | Same key + different body → 422 | controller #12, store test #3 |
| 3 — Idempotency | Failed payments not cached | store test #4 |
| 4 — Resilience | Retry only on 503, ≤3 attempts, exp. backoff | `ResilientAcquiringBankClientTest` #1, #2 |
| 4 — Resilience | Circuit breaker fast-fails when open | `ResilientAcquiringBankClientTest` #4 |
| 4 — Resilience | Clean 503 to merchant — no stack trace | controller #15 |
| 5 — Observability | `X-Request-Id` echo / generate / blank | filter #1, #2, #3 + controller #13, #14 |
| 5 — Observability | `X-Correlation-Id` echo / fallback / both generated | filter #4, #5, #6 |
| 5 — PII | PAN / CVV never logged or returned | `@ToString` PII tests on all 3 DTOs + controller #16, #17 |
| 6 — Testing | Three bank scenarios | controller #3 (auth), #4 (declined), #15 (503) |

---

## 3. Live end-to-end verification (2026-04-23)

Run against `./gradlew bootRun` + `docker compose up -d` (Mountebank simulator
bound to `:8080`, gateway on `:8090`).

### A) Authorize with idempotency key
```http
POST /api/v1/payments
Idempotency-Key: demo-001
{ "card_number":"2222405343248877", "expiry_month":4, "expiry_year":2030,
  "currency":"GBP", "amount":100, "cvv":"123" }
```
**Result:** `201 Created`, `Idempotent-Replayed: false`, `Location:
/api/v1/payments/c3cffd69-de0f-429d-8784-91b05c9c77e8`, body
`{"id":"c3cffd69…","status":"Authorized","card_number_last_four":"8877"}`. ✅

### B) Same call replayed
Same body, same `Idempotency-Key`. Bank is **not** invoked again.
**Result:** `201 Created`, `Idempotent-Replayed: true`, **same `id`** as A. ✅

### C) GET the payment back
```http
GET /api/v1/payments/c3cffd69-de0f-429d-8784-91b05c9c77e8
```
**Result:** `200 OK`. Body contains `card_number_last_four=8877` and **no**
`card_number` / `cvv` keys. ✅

### D) Decline (card ends in `6`)
```http
POST /api/v1/payments
{ "card_number":"2222405343248876", … }
```
**Result:** `201 Created`, `status: Declined`. ✅

### E) Bank 503 (card ends in `0`)
**Result:** `503 Service Unavailable`,
`Retry-After: 30`,
body `{"status":"Error","code":"ACQUIRING_BANK_SERVICE_UNAVAILABLE","message":"Acquiring bank is temporarily unavailable, please retry shortly", … }` —
no stack trace leaked. ✅

### F) Trip the circuit breaker — 12 consecutive `0`-ending POSTs
| Call | Status | Latency |
|---|---|---|
| 1 | 503 | 0.667 s |
| 2 | 503 | 0.650 s |
| 3 | 503 | 0.645 s |
| 4 | 503 | 0.645 s |
| 5 | 503 | 0.637 s |
| 6 | 503 | 0.636 s |
| 7 | 503 | 0.642 s |
| **8** | **503** | **0.004 s** ← breaker opened |
| 9 | 503 | 0.003 s |
| 10 | 503 | 0.003 s |
| 11 | 503 | 0.003 s |
| 12 | 503 | 0.003 s |

The breaker collapses bank-failure latency from **~640 ms → ~3 ms**, protecting
both us and the upstream bank. Gateway logs:
`Acquiring bank circuit breaker CLOSED -> OPEN`, then
`Acquiring bank circuit breaker is OPEN, fast-failing request` for the rest. ✅

### G) Validation rejection
```json
{ "card_number":"12", "expiry_month":13, "expiry_year":1999,
  "currency":"JPY", "amount":0, "cvv":"12" }
```
**Result:** `400 Bad Request`, `code: VALIDATION_FAILED`, errors list contains
every offending field (`amount`, `cvv`, `cardNumber`, `currency`,
`expiryMonth`). Bank was **not** called. ✅

### H) Correlation-id propagation
```http
GET /api/v1/payments/{id}
X-Request-Id: hop-1
X-Correlation-Id: journey-42
```
**Result:** response carries `X-Request-Id: hop-1` and
`X-Correlation-Id: journey-42` unchanged. ✅

---

## 4. Suggested follow-ups (not implemented yet)

- **Real Mountebank-backed integration test** (currently we mock the bank in unit
  tests). One `@SpringBootTest` that points at `localhost:8080` and exercises
  cards `…1`, `…2`, `…0` end-to-end — would catch contract drift on
  `BankAuthorizationRequest`/`Response`.
- **Pact contract tests** with the acquiring bank.
- **Load + soak** (`k6` / Gatling) to characterise p99 latency, validate the
  breaker thresholds under real bursty traffic, and pressure-test the
  idempotency store.
- **Mutation testing** (PIT) — would expose any tests that pass with weakened
  assertions.

