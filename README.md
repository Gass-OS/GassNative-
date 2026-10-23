<div align="center">GassNative

A Debian ARM64 Desktop Inside Android

Bringing the power of Linux to Android — without root access.

""Android" (https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)" (https://www.android.com/)
""Java" (https://img.shields.io/badge/Java-Application-ED8B00?logo=openjdk&logoColor=white)" (https://www.java.com/)
""Bash" (https://img.shields.io/badge/Bash-Build_System-4EAA25?logo=gnubash&logoColor=white)" (https://www.gnu.org/software/bash/)
""Debian" (https://img.shields.io/badge/Debian-13_Trixie-A81D33?logo=debian&logoColor=white)" (https://www.debian.org/)
""Linux" (https://img.shields.io/badge/Linux-ARM64-FCC624?logo=linux&logoColor=black)" (https://www.kernel.org/)
""PRoot" (https://img.shields.io/badge/PRoot-Rootless_Linux-0078D4)" (https://proot-me.github.io/)

"Download APK" (https://github.com/Gass-OS/GassNative-/releases) · "Source Code" (https://github.com/Gass-OS/GassNative-) · "Report a Bug" (https://github.com/Gass-OS/GassNative-/issues)

</div>---

About the Project

GassNative is an experimental Android application created by Aymen Gharsalli under the Gass-OS project.

Its goal is to bring a complete Debian ARM64 Linux environment and desktop experience to Android devices. It combines a native Java Android application, PRoot, Linux userspace components, and an integrated graphical display.

GassNative is designed to run without requiring root access to the Android device.

The project explores the integration of Linux desktop applications, ARM64 software, native Android interfaces, and Windows compatibility technologies on modern Android hardware.

Features

- Native Android application — Java-based interface and Android activities.
- Debian ARM64 — Debian 13 Trixie Linux userspace.
- Rootless execution — PRoot-based environment without requiring Android root.
- XFCE4 desktop — A lightweight Linux desktop environment.
- Integrated display — Android SurfaceView and Linux display integration.
- Profile management — Separate Linux environment profiles.
- System settings — DNS, environment variables, bind mounts and configuration.
- Terminal tools — Access to the Debian command-line environment.
- Crash reporting — Diagnostics for troubleshooting application issues.
- Windows compatibility research — Bundled Wine/Hangover components.
- Graphics development — Experimental Linux graphics acceleration on Android.

Some features are experimental and may behave differently depending on the device and configuration.

Technology Stack

Android Application Development

<div align="center">""Java" (https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)" (https://www.java.com/)
""Android SDK" (https://img.shields.io/badge/Android_SDK-3DDC84?style=for-the-badge&logo=android&logoColor=white)" (https://developer.android.com/)
""Bash" (https://img.shields.io/badge/Bash-4EAA25?style=for-the-badge&logo=gnubash&logoColor=white)" (https://www.gnu.org/software/bash/)

</div>The project uses Java for Android application logic and Bash for the manual build process and automation.

The current source tree contains 33 Java source files and one Bash build script. The APK is built using Android SDK tools rather than Gradle.

Linux Environment and Desktop

<div align="center">""Debian" (https://img.shields.io/badge/Debian-A81D33?style=for-the-badge&logo=debian&logoColor=white)" (https://www.debian.org/)
""Linux" (https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black)" (https://www.kernel.org/)
""XFCE" (https://img.shields.io/badge/XFCE-Desktop-2284F2?style=for-the-badge)" (https://xfce.org/)
""Wayland" (https://img.shields.io/badge/Wayland-Display-FFBC00?style=for-the-badge)" (https://wayland.freedesktop.org/)

</div>The Linux environment is based on Debian 13 Trixie for ARM64. XFCE4 provides the desktop interface, while PRoot enables rootless execution of the Linux userspace.

Wayland and Xwayland technologies are part of the graphical integration work.

Native Components and Graphics

<div align="center">""C" (https://img.shields.io/badge/C-Native_Code-A8B9CC?style=for-the-badge&logo=c&logoColor=black)" (https://en.cppreference.com/w/c)
""Clang" (https://img.shields.io/badge/Clang-LLVM-262D3A?style=for-the-badge&logo=llvm&logoColor=white)" (https://clang.llvm.org/)
""Mesa" (https://img.shields.io/badge/Mesa-Graphics-750000?style=for-the-badge)" (https://mesa3d.org/)

</div>GassNative packages precompiled native libraries and investigates integration with the Linux graphics stack, including Mesa and Qualcomm Adreno graphics hardware.

The badges identify relevant technologies in the project's native and graphics ecosystem; they do not imply that every component is compiled from source within this repository.

Additional Development Ecosystem

<div align="center">""JavaScript" (https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)" (https://developer.mozilla.org/en-US/docs/Web/JavaScript)
""Rust" (https://img.shields.io/badge/Rust-000000?style=for-the-badge&logo=rust&logoColor=white)" (https://www.rust-lang.org/)

</div>JavaScript and Rust are relevant technologies in the broader development ecosystem and may be used by tools or applications inside Debian. They are not currently identified as implementation languages in the GassNative Android source tree.

Architecture

GassNative
|
+-- Android Application
|   |
|   +-- Java Activities
|   +-- Home Interface
|   +-- Profiles
|   +-- Settings
|   +-- Desktop Interface
|   +-- Crash Reports
|
+-- Native Integration
|   |
|   +-- PRoot
|   +-- Native Libraries
|   +-- Display Bridge
|
+-- Debian 13 Trixie ARM64
    |
    +-- XFCE4 Desktop
    +-- Linux Applications
    +-- Terminal
    +-- Wine / Hangover Components
    +-- Linux Graphics Stack

Windows Application Compatibility

GassNative includes a bundled Wine/Hangover archive intended for installation into the Debian environment.

The objective is to explore running compatible Windows applications on ARM64 Android devices through Linux compatibility components.

Windows compatibility depends on the application, CPU architecture, graphics support, required libraries, and additional translation components.

Bundled compatibility software does not guarantee that every Windows application or game will work.

Graphics Acceleration

Hardware-accelerated Linux graphics are an ongoing area of research and development.

The project investigates:

- Android SurfaceView integration with a Linux desktop.
- Wayland and Xwayland display paths.
- Mesa graphics drivers.
- Qualcomm Adreno GPU support.
- Freedreno and Turnip compatibility.
- Native rendering performance on ARM64 Android devices.

Graphics acceleration remains experimental. Software rendering or incomplete functionality may occur on some devices.

Requirements

- Android 7.0 or newer.
- ARM64-compatible Android device.
- Sufficient free storage for the application, Debian environment, and additional packages.
- Internet access for Debian installation and downloading packages when required.

The actual storage requirement depends on the installed desktop applications and compatibility components.

Installation

1. Open the "GassNative Releases" (https://github.com/Gass-OS/GassNative-/releases) page.
2. Download the APK attached to the latest release.
3. Allow installation from the relevant source if Android requests permission.
4. Install and launch GassNative.
5. Follow the Debian installation process.
6. Open the integrated desktop when setup is complete.

Only install APKs from sources you trust.

Build from Source

GassNative uses a manual Android SDK build workflow rather than a Gradle project.

Clone the repository:

git clone https://github.com/Gass-OS/GassNative-.git
cd GassNative-

Build using the project script:

bash build.sh

The build requires a compatible Android SDK, Java development tools, Android packaging and signing utilities, and the project's required native libraries and assets.

The final APK is generated by the build script.

Project Structure

GassNative-/
|
+-- src/
|   +-- com/gass/gassnative/
|
+-- res/
|
+-- assets/
|   +-- hangover/
|
+-- jniLibs/
|   +-- arm64-v8a/
|
+-- build.sh
+-- README.md

The exact files and directories may evolve as development continues.

Project Status

Experimental — Active Development

GassNative is under active development. Features, performance, compatibility and installation behavior may change between releases.

Issues and reproducible bug reports are welcome.

Contributing

Contributions, testing, bug reports and technical discussions are welcome.

When reporting an issue, please include:

- Android version and device architecture.
- GassNative version.
- Steps to reproduce the problem.
- Relevant crash logs or diagnostic output.

Please avoid publishing private data or access tokens in issue reports.

Credits and Acknowledgements

Created by Aymen Gharsalli for the Gass-OS project.

The project builds on the work of the open-source communities behind:

- Debian
- Android
- PRoot
- XFCE
- Wayland
- Mesa
- Wine and Hangover
- LLVM and Clang

GassNative is an independent project and is not an official product of these projects or organizations.

License

Refer to the repository's license file for the applicable license terms. Third-party components remain subject to their respective licenses.

---

<div align="center">GassNative

Bringing a Linux desktop experience to Android.

Made with dedication by Aymen Gharsalli · Gass-OS

"GitHub Repository" (https://github.com/Gass-OS/GassNative-) · "Releases" (https://github.com/Gass-OS/GassNative-/releases)

</div>
