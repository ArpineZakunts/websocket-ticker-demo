# WebSocket Ticker Demo (Android / Kotlin)

Portfolio sample focused specifically on real-time WebSocket handling: a live price ticker
with an explicit connection lifecycle, ping keepalive, and reconnect-with-backoff that
survives drops without the UI ever seeing an unhandled exception.

## Stack

- Kotlin + Jetpack Compose, Clean Architecture (`domain` → `data` → `presentation`)
- **Ktor Client WebSockets** for the transport
- **Koin** for DI, **MVI** for the screen (State / Action)
- Coroutines `Flow`/`StateFlow`/`SharedFlow`, structured concurrency (`supervisorScope`),
  `Mutex`-guarded lifecycle

## What it demonstrates

- `data/remote/QuoteSocketClient.kt` — the core of the demo. Holds the session in an
  `AtomicReference`, exposes an explicit `initSocket(symbols)` / `close()` lifecycle guarded
  by a `Mutex` (so a caller can't race a reconnect against a teardown), and runs the connect
  loop and ping keepalive as two children of one `supervisorScope` — a ping failure doesn't
  silently kill the read loop or vice versa. Reconnects on any `Exception` other than
  `CancellationException`.
- `data/remote/ReconnectStrategy.kt` — exponential backoff with jitter as a **pure function**
  (`nextDelayMillis(attempt): Long`), unit-tested without touching coroutines, timers, or a
  real socket. See `ReconnectStrategyTest.kt`.
- `data/repository/QuoteRepositoryImpl.kt` — thin adapter from the domain's `connect`/
  `disconnect`/`quotes`/`connectionStatus` contract onto the socket client; the ViewModel
  decides when the connection opens and closes, not the Flow's collection lifecycle.
- `domain/model/Quote.kt` — a `ConnectionStatus` sealed type (`Connecting` / `Connected` /
  `Reconnecting(attempt, delayMillis)` / `Failed`) surfaced all the way to the UI, so the user
  sees *why* data stopped updating instead of a frozen screen.
- `presentation/ticker/TickerViewModel.kt` + test — MVI ViewModel that connects on
  `StartWatching`, collects quotes and connection status concurrently, and disconnects on
  `StopWatching`/`onCleared`. Fake repository built on `MutableSharedFlow` drives both flows
  in tests independently of the real socket.

## Running

Open in Android Studio, let Gradle sync, run `app`. There's no live backend behind the
placeholder host, so the screen will sit in "Reconnecting" — the point is the connection
lifecycle and backoff logic, not a working feed.
