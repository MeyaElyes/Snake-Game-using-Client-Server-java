# Snake Game (Java) - Server/Client

This repository contains a minimal Java server and client for a multiplayer snake game.

Features:
- Server accepts multiple clients over TCP and runs a shared game map and apples.
- Clients render the map using Swing. Snakes are squares; apples are red circles.

How to compile (Windows PowerShell):

1. Open PowerShell in the project root (where `src` is located).
2. Compile sources:

```powershell
javac -d out -sourcepath src src/common/*.java src/server/*.java src/client/*.java
```

## Snake Game (Java) — Quick Start

Minimal multiplayer snake server + Swing client.

Quick PowerShell steps (copy-paste from the project root: `C:\..\snakegamejava`)

1) Compile (recursively finds all .java files and writes classes into `out\`):

```powershell
if (-not (Test-Path out)) { New-Item -ItemType Directory out | Out-Null }
$files = Get-ChildItem -Path . -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files
```

2) Run the server (leave this terminal open):

```powershell
java -cp out server.GameServer
```

3) Run one or more clients (each in its own PowerShell window):

```powershell
java -cp out client.GameClient YourName
```

Notes
- You can run the server and clients on the same machine (use multiple terminals).
- If `javac` or `java` are not found, make sure a JDK is installed and added to your PATH.
- This is a prototype: networking is simple Java Object streams, and collision rules are basic.

If you want, I can add a single-command script (PowerShell `.ps1`) to automate compile + run steps.
