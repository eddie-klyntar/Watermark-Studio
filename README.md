# Watermark Studio

**Your images. Your identity. Your watermark.**

Watermark Studio is an open-source Android image watermarking and compositing application designed for creators who want complete control over how their images are branded.

Instead of relying on basic text-overlay tools and restrictive watermark presets, Watermark Studio provides a dedicated visual workspace for creating, styling, positioning and applying custom text and logo watermarks.

The project focuses on creative control, customisation and a straightforward local editing workflow.

---

## ✨ Features

### Text & Logo Watermarking
- Add custom text overlays.
- Import and position logos.
- Customise watermark typography.
- Adjust text appearance and opacity.
- Create personalised image branding.

### Advanced Fill & Effects
- Gradient text fills.
- Custom gradient colours.
- Gradient angle adjustment.
- Text fill opacity.
- Background highlight effects.
- Additional visual styling controls.

### Compositing & Positioning
- Fine-grained watermark positioning.
- Visual element selection.
- Compositing controls.
- Adjustable placement and alignment.
- On-canvas editing with selection handles.

### Tiled Watermark Engine
- Create repeated watermark patterns.
- Configure tiled watermark appearances.
- Apply repeated patterns across images.
- Customise the visual presentation of tiled elements.

### Image Import
- Import images from the Android gallery.
- Import TIFF, BMP and other supported image files.
- Work with locally selected images.

### Batch Processing
- Folder-based batch processing.
- Processing queue.
- Multiple-image workflows.

### Projects & Export
- Save and manage reusable `.wmstudio` projects.
- Export edited images.
- Preserve editable project configurations for future work.

---

## 🎨 Designed for Creative Freedom

Watermark Studio is intended for people who want more than a username placed in a corner.

Whether you're a photographer, digital artist, social-media creator, designer or someone who simply wants to protect and identify their images, the application provides a flexible environment for creating personalised watermarks.

The interface provides dedicated editing categories, including:

- Content
- Fill & FX
- Compositing
- Position
- Tiling

Each category provides controls for a different aspect of the watermarking process.

---

## 📱 Screenshots

Screenshots and additional application previews will be added here as the project develops.

---

## 📦 Download

Prebuilt Android APKs are available through the repository's GitHub Releases section.

**[Browse Watermark Studio Releases](https://github.com/eddie-klyntar/Watermark-Studio/releases)**

### Current Preview

**Version:** v0.1.0-debug

This is an early public preview intended for testing and experimentation.

Please be aware that:

- This is a debug build, not a production release.
- Some features may still require refinement.
- Future debug builds may not support direct upgrades because their signing keys can differ.
- Back up important projects and exported images.

---

## 🛠️ Building from Source

Watermark Studio is developed as a native Android application using Kotlin.

The repository contains the application source code and Android project configuration.

### Requirements

- Windows, Linux or macOS.
- JDK compatible with the project's Gradle configuration.
- Android SDK.
- Compatible Gradle installation.
- Required Android SDK platforms and build tools.

### Build a Debug APK

From the project root, run:

**Windows PowerShell:**

```powershell
gradle :app:assembleDebug
```

**Linux / macOS:**

```bash
gradle :app:assembleDebug
```

The generated APK will normally be located at:

```text
app/build/outputs/apk/debug/
```

### Build a Release APK

```bash
gradle :app:assembleRelease
```

Release signing must be configured with your own signing keystore and the project's release configuration.

Never commit private signing keys or passwords to the repository.

Refer to the Gradle configuration files for project-specific build requirements.

---

## 🔧 Development

Contributions, modifications and independent builds are welcome.

You can:

- Inspect the source code.
- Modify the application.
- Customise the interface.
- Extend existing functionality.
- Add new editing capabilities.
- Build your own APK.
- Experiment with the project architecture.

The goal is to keep the project accessible to developers and creators who want to understand, modify or extend the application.

---

## 🗺️ Project Roadmap

Potential areas of future development include:

- Additional text styling options.
- More compositing controls.
- Expanded gradient and visual effects.
- Additional watermark placement controls.
- Improved tiled pattern customisation.
- Expanded image format support.
- Batch-processing improvements.
- Additional project-management capabilities.
- UI and performance refinements.

This roadmap is exploratory and does not guarantee that every proposed feature will be implemented.

---

## 🐛 Issues & Feedback

Found a bug? Have a feature suggestion? Want to improve something?

Please use the GitHub Issues section:

**[Report an Issue](https://github.com/eddie-klyntar/Watermark-Studio/issues)**

Useful reports should include:

- Android device model.
- Android version.
- Application version.
- Steps to reproduce the issue.
- Expected behaviour.
- Actual behaviour.
- Screenshots or logs where relevant.

Constructive feedback and contributions are welcome.

---

## 📄 License

The project license will be specified in the repository's `LICENSE` file.

Please check the applicable license before redistributing modified versions or incorporating the code into other projects.

---

## About

Watermark Studio is an independent open-source software project.

It is built around a simple idea:

**Your watermark should look the way you want it to look, not the way a preset tells you it should.**
