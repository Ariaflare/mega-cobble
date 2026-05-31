# Mega Cobble

A **proof-of-concept** Fabric add-on that brings **Mega Evolution** to
[Cobblemon](https://cobblemon.com/).

> Status: early scaffold. Right now the mod just loads cleanly alongside Cobblemon
> and logs on init. Mega Evolution mechanics are built out from here.

## Target stack

| Component        | Version          |
| ---------------- | ---------------- |
| Minecraft        | 1.21.1           |
| Mod loader       | Fabric           |
| Fabric Loader    | >= 0.17.2        |
| Fabric API       | >= 0.116.6+1.21.1 |
| Cobblemon        | 1.7.3+1.21.1     |
| Java             | 21               |

## Project layout

```
src/main/java/com/aaroncraft/megacobble/        common entrypoint (MegaCobble.java)
src/main/java/com/aaroncraft/megacobble/client/  client entrypoint (MegaCobbleClient.java)
src/main/resources/fabric.mod.json               mod metadata
build.gradle / settings.gradle / gradle.properties   Fabric Loom build config
```

## Building

You need JDK 21 on your PATH. From the project root:

```bash
./gradlew build          # Linux / macOS
.\gradlew.bat build      # Windows
```

The built mod jar lands in `build/libs/`.

## Running in dev

```bash
./gradlew runClient      # launch a dev client with the mod + Cobblemon loaded
./gradlew runServer      # launch a dev server
```

## Dependencies & the Cobblemon jar

Cobblemon is pulled automatically from its
[Maven repository](https://maven.impactdev.net/) at build time — you do **not**
need the local jar to build.

The provided `Cobblemon-fabric-1.7.3+1.21.1.jar` (~135 MB) is **git-ignored** and
kept only as a local reference. It is not committed because of its size.

## License

All rights reserved (proof of concept / private).
