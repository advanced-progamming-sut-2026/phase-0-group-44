# PvZ Asset Browser
A desktop application for viewing Plants vs. Zombies 2 PAM animations, character parts, and texture atlases, built with LibGDX and [libPVZ](https://github.com/pizpizi/libPVZ).

## Features
- **Animation Playback:** Browse and play PAM animation clips directly from your extracted asset directory.
- **Part Inspection:** View individual character parts, armor states, and status effects alongside their visibility maps.
- **Atlas & Region Browser:** Search full texture atlases and individual sub-image regions.
- **Atlas Exporter:** Cut and export texture atlases.
- **Interactive Canvas:**
  - **2D Viewport Panning:** Middle-click and drag to move freely around the workspace.
  - **Camera Zooming:** Scroll wheel zoom.
  - **On-Screen Measurement Tool:** Left-click and drag to draw a box that measures pixel width and height dimensions.

## Requirements
- **JDK:** 8+
- **Gradle:** 7.0+

## Configuration
Set your local PVZ2 asset directory (which should contain `IMAGES/`, `ATLASES/`, and a `Resources.json`, which is the decoded `RESOURCES.RTON`) path in `gradle.properties`:
```properties
systemProp.pvz.assets=/path/to/PVZ2 Assets
```

## Building and Running
### Run via Gradle
```bash
./gradlew run
```

### Build via Gradle
```bash
./gradlew build
```

### Run via Executable JAR
```bash
java -Dpvz.assets="/path/to/PVZ2 Assets" -jar browser.jar
```

<img width="1920" height="1168" alt="image" src="https://github.com/user-attachments/assets/c11fa2cb-bb18-4a0d-874f-39ebe5274e50" />
<img width="1920" height="1168" alt="image" src="https://github.com/user-attachments/assets/9728e49a-54d5-4cfd-945e-d1ce5c9b0bda" />

## Contributing
Bug reports and feature requests are welcome — please open an [issue](https://github.com/pizpizi/pvz-asset-browser/issues).

## License
This project is licensed under the [MIT License](LICENSE).

This is an unofficial, fan-made project and is not affiliated with, endorsed by, or associated with PopCap Games or Electronic Arts. No game assets are distributed with this application — you must provide your own extracted assets from a legally owned copy of the game.
