# WebSocket Ticker Demo (Android / Kotlin)

Portfolio sample focused specifically on real-time WebSocket handling: a live price ticker
that stays connected through drops, backs off correctly, and shares one socket across every
UI observer instead of opening a new connection per screen.

## Stack

- Kotlin + Jetpack Compose, Clean Architecture (`domain` → `data` → `presentation`)
- **Ktor Client WebSockets** for the transport
- **Koin** for DI, **MVI** for the screen (State / Action)
- Coroutines `Flow`, `callbackFlow`, `shareIn`

## What it demonstrates

- `data/remote/QuoteSocketClient.kt` — owns the raw socket: subscribe handshake, frame
  decoding, and a reconnect loop that never lets an `Exception` kill the flow silently.
- `data/remote/ReconnectStrategy.kt` — exponential backoff with jitter as a **pure function**
  (`nextDelayMillis(attempt): Long`), so the retry timing is unit-tested without touching
  coroutines, timers, or a real socket. See `ReconnectStrategyTest.kt`.
- `data/repository/QuoteRepositoryImpl.kt` — multiplexes the single socket connection across
  every collector with `shareIn(WhileSubscribed(5_000))`: the connection survives a screen
  rotation or quick navigation instead of reconnecting, and closes itself when nobody's
  listening anymore.
- `domain/model/Quote.kt` — a `ConnectionStatus` sealed type (`Connecting` / `Connected` /
  `Reconnecting(attempt, delayMillis)` / `Failed`) surfaced all the way to the UI, so the user
  sees *why* data stopped updating instead of a frozen screen.
- `presentation/ticker/TickerViewModel.kt` + test — MVI ViewModel collecting two flows
  (quotes, connection status) concurrently, with a fake repository built on
  `MutableSharedFlow` to drive both in tests.

## Running

Open in Android Studio, let Gradle sync, run `app`. There's no live backend behind the
placeholder host, so the screen will sit in "Reconnecting" — the point is the connection
lifecycle and backoff logic, not a working feed.
