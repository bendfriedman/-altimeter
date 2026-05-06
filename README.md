# Höhenmesser — Android Altimeter App

A simple Android altimeter app built with Kotlin and Jetpack Compose. It reads the device's barometric pressure sensor and converts the raw pressure reading into an altitude estimate using the International Barometric Formula.

## Features

- **Live altitude** calculated from the barometric pressure sensor
- **Live pressure** display in hPa
- **Calibration** — enter a known altitude to recalibrate the reference pressure (P₀), improving accuracy for your current location
- Sensor listener is properly unregistered in `onDestroy` to avoid battery drain

## How it works

Altitude is derived from the **International Barometric Formula** (valid up to 11,000 m):

```
h = (288.15 / 0.0065) × (1 − (p / P₀)^(1/5.255))
```

where `p` is the current pressure and `P₀` is the reference pressure at sea level (default: 1013.25 hPa).

When you calibrate by entering a known altitude, the app solves the inverse formula to compute a corrected P₀:

```
P₀ = p / (1 − 0.0065 × h / 288.15)^5.255
```

## Tech stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | ViewModel + `mutableStateOf` (state hoisting) |
| Sensor | `SensorManager` — `Sensor.TYPE_PRESSURE` |
| Min SDK | Android API 30 |

## Project structure

```
app/src/main/java/com/example/hoehenmesser/
└── MainActivity.kt      # Activity, SensorEventListener, ViewModel, UI, calibration logic
```

## Getting started

1. Clone the repo and open the project in Android Studio.
2. Run on a **physical device** — the emulator does not expose a real pressure sensor.
3. For best accuracy, calibrate the app by entering the altitude of your current location (e.g. from a map or GPS app).
