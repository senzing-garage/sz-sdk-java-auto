# Configuration refresh modes (DISABLED / REACTIVE / PROACTIVE)

Automatic configuration refresh keeps the
{@code activeConfigId} in sync with the current {@code defaultConfigId}. The
mode is selected by the `configRefreshPeriod` (`Duration`) passed to the
builder:

| Mode | `configRefreshPeriod` | Behavior |
|------|-----------------------|----------|
| **DISABLED** | `null` (`DISABLED_CONFIG_REFRESH`) | No automatic refresh; `@SzConfigRetryable` methods are not retried after config changes. |
| **REACTIVE** | `Duration.ZERO` (`REACTIVE_CONFIG_REFRESH`) | On-demand: when a method annotated `@SzConfigRetryable` fails, the config is refreshed; if it actually changed, the method is retried automatically. |
| **PROACTIVE** | `Duration.ofSeconds(N)` where `N > 0` | A background `Reinitializer` thread refreshes every `N` seconds **and** still performs the reactive refresh on failures. |

## How it works

- REACTIVE/PROACTIVE compare the active vs. default configuration IDs and
  reinitialize when they differ.
- Reinitialization is bounded by `MAX_REINITIALIZE_COUNT` (5) to avoid an
  infinite loop under racing config changes; on exhaustion a `*** WARNING`
  is printed and the caller is allowed to retry anyway.
- The thread-local `ENSURING_CONFIG` flag prevents infinite recursion when
  the SDK calls made *during* a refresh (e.g. `getActiveConfigId()`,
  `getDefaultConfigId()`, `reinitialize()`) route back through
  `execute(...)`.

## Important constraint

You **cannot** specify both an explicit `configId` *and* enable configuration
refresh — the builder throws `IllegalStateException`. A fixed `configId` pins
the environment to one configuration, which is fundamentally incompatible with
keeping it in sync with the default.

## Convenience constants

- `DISABLED_CONFIG_REFRESH` = `null` `Duration`
- `REACTIVE_CONFIG_REFRESH` = `Duration.ofSeconds(0)`
- `DISABLED_CONCURRENCY` = `null` `Integer`
- `RECOMMENDED_CONCURRENCY` = `0` (use `Runtime.availableProcessors()`)
