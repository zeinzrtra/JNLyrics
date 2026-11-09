<div align="center">

# 🎵 JNLyrics

![Fabric](https://img.shields.io/badge/Loader-Fabric-blue?style=for-the-badge&logo=fabric)
![Minecraft](https://img.shields.io/badge/Minecraft-26.2-brightgreen?style=for-the-badge&logo=minecraft)
![Environment](https://img.shields.io/badge/Environment-Client--Only-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-purple?style=for-the-badge)

**Bring your soundtrack to life in Minecraft.**  
Real-time 3D in-world floating lyrics and a sleek dynamic music HUD for **Spotify**, **YouTube**, and Windows media players.

</div>

---

## ✨ Features

### 🌌 3D In-World Floating Lyrics
- **True In-World Immersion**: Lyrics appear floating naturally in front of your character, complete with smooth billboard rotation and depth blending.
- **Dynamic Pop-Up Animations**: Words and lines glide into view with fluid, responsive animations inspired by *Dango* and *LyricsPlus*.
- **Vibrant Glow Palettes**: Choose from *Sparkling Gold*, *Neon Cyan*, *Neon Purple*, *Emerald Green*, or *Crystal White*.
- **Next-Line Preview**: Optional preview of upcoming lines so you never miss a beat during karaoke while mining!

### 🏝️ Sleek Dynamic Island HUD
- A minimalist frosted-glass pill at the top of your screen showing the active song title, artist, and a glowing real-time progress bar.
- Automatically stays out of the way of your crosshair and vanilla HUD elements.

### ⚡ True Zero-Latency Telemetry (Mutlak 0ms)
- Direct integration with **Windows WinRT System Media Transport Controls (SMTC)** updating every **100ms** (10x/sec).
- Monotonic clock interpolation ensures lyrics hit the exact vocal beat with zero drift, stutter, or artificial delays.

### 🎧 Universal Media Support
- **Spotify Desktop**: Instant detection with official studio track metadata.
- **YouTube & YouTube Music**: Smart title parsing automatically extracts the artist and song title from video formats like `Artist - Title (Official Video)` in Chrome, Edge, Firefox, and Brave.
- Also supports Apple Music, Deezer, Tidal, VLC, and any browser-based web player.

### 🌐 Automatic High-Precision Lyrics (LRCLIB)
- Automatically fetches time-synced studio lyrics via [LRCLIB](https://lrclib.net/).
- Intelligent multi-layered candidate scoring filters out community blank lines and matches the exact track duration (±4.5s).
- Offline disk caching ensures played songs load instantly on repeat plays without network traffic.

---

## 🎮 Controls & Settings

Mod controls are streamlined to prevent key conflicts with your favorite modpacks:

| Key | Action | Description |
| :---: | :---: | :--- |
| **`M`** | **Open Settings Menu** | Opens the sleek in-game configuration card. |
| **`L`** | **Toggle Lyrics** | Instantly show or hide lyrics with a quick toast notification. |

> *All keybindings can be customized in Options → Controls → Key Binds under the **JNLyrics** category.*

---

## ⚙️ Modern In-Game GUI (`M`)

Press **`M`** anytime in-game to open the frosted glass settings modal:
- **3D World Mode**: Toggle floating in-world lyrics.
- **Top HUD Pill**: Toggle the compact media progress island.
- **Pop-Up Animation Style**: Switch between *Dynamic Floating* and *Fixed Center*.
- **Glow Color Selector**: Pick your favorite glowing visual accent.
- **Interactive Offset Slider**: Smoothly calibrate timing from `-1000ms` (Earlier) to `+1000ms` (Later) with **0ms** right in the center.
- **Cancel & Save Footer**: Discard unwanted changes with **Batal** or save permanently with **Simpan**.

---

## 📋 Requirements & Compatibility

- **Minecraft**: `26.x` (`26.1` – `26.4`)  
  *(For Minecraft `1.21.2` – `1.21.4`, switch to the [`1.21.x`](https://github.com/zeinzrtra/JNLyrics/tree/1.21.x) branch)*
- **Mod Loader**: **Fabric Loader** (≥ 0.16.0)
- **API**: **Fabric API**
- **Java**: Java 25
- **OS**: Windows 10 / Windows 11 *(utilizes native Windows Media Telemetry)*
- **Side**: **Client-Only** (100% safe to use on any vanilla or modded multiplayer server)

---

## 📦 Modpack Policy

**Yes, you can!**  
You are free to include **JNLyrics** in any public or private modpack on Modrinth or CurseForge under the terms of the **MIT License**.

---

<div align="center">

Crafted with ❤️ by **JNCompany** (`zeinzrtra`)

</div>
