# SpacePlan 🚀

**SpacePlan** is a modern, high-precision Android application built with Jetpack Compose and Material 3, designed for precise spatial planning, layout calculation, and object fitting estimation. Whether you are designing interior layouts, organizing furniture, planning shelving, parking spaces, or calculating structural grid partitions, SpacePlan provides instant calculations with millimeter precision and adaptive units.

---

## ✨ Features

- **Precise Spatial Calculations**: Calculate maximum fitting object counts, required gaps, spacing distributions, and dimensional constraints.
- **Flexible Edge Gap Modes**: Support for equal edge spacing, custom edge gaps, and continuous item intervals.
- **Unit Conversion**: Seamlessly convert and calculate across metric (millimeters, centimeters, meters) and imperial (inches, feet) units.
- **Interactive Layout Visualizations**: Visual layout graphs and representation cards showing item distribution, gaps, and clearances.
- **History & Recent Calculations**: Save, review, and manage past calculations for easy reference.
- **Material 3 & Dynamic Theming**: Fully adaptive UI featuring Material You dynamic color support, light/dark themes, and sleek Jetpack Compose components.
- **Robust Validation & Error Handling**: Real-time input validation preventing invalid dimensions, negative gaps, and edge-case overflow.

---

## 📱 Tech Stack & Architecture

- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/)
- **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF) and StateFlow
- **Language**: [Kotlin](https://kotlinlang.org/) & Coroutines
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **Testing**: JUnit 4 / JUnit 5 unit test suites for calculator logic, formatting, and persistence.

---

## 🛠️ Getting Started

### Prerequisites
- Android Studio Ladybug or newer / IntelliJ IDEA
- JDK 17 or higher
- Android SDK (API level 24 or higher; target API 34+)

### Building and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/abdullah-ateeq/SpacePlan.git
   ```
2. Open the project in Android Studio.
3. Sync project with Gradle files.
4. Run the app on an Android emulator or physical device.

---

## 🧪 Testing

Run unit tests via Gradle:
```bash
./gradlew test
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

---
*Created by [Abdullah Ateeq](https://github.com/abdullah-ateeq)*
