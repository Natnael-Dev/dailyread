# Third-Party Licenses & Dependency Provenance Registry

Project: **DailyRead**  
Application ID: `io.github.natnael_dev.dailyread`  
Repository License: **Apache License, Version 2.0**

This document provides a comprehensive audit of all third-party software dependencies, libraries, fonts, and visual assets incorporated into the DailyRead production release.

---

## 1. Production Runtime Dependencies

All production runtime dependencies utilize permissive, open-source licenses compatible with the Apache-2.0 project license and F-Droid / IzzyOnDroid inclusion policies.

| Component / Artifact | Group & Module | Version | License | Copyright / Provenance |
|---|---|---|---|---|
| **Readium Shared** | `org.readium.kotlin-toolkit:readium-shared` | 3.1.1 | **BSD-3-Clause** | © European Digital Reading Lab (EDRLab) |
| **Readium Streamer** | `org.readium.kotlin-toolkit:readium-streamer` | 3.1.1 | **BSD-3-Clause** | © European Digital Reading Lab (EDRLab) |
| **Readium Navigator** | `org.readium.kotlin-toolkit:readium-navigator` | 3.1.1 | **BSD-3-Clause** | © European Digital Reading Lab (EDRLab) |
| **Readium PDFium Navigator** | `org.readium.kotlin-toolkit:readium-adapter-pdfium-navigator` | 3.1.1 | **BSD-3-Clause** | © European Digital Reading Lab (EDRLab) |
| **Readium PDFium Document** | `org.readium.kotlin-toolkit:readium-adapter-pdfium-document` | 3.1.1 | **BSD-3-Clause** | © European Digital Reading Lab (EDRLab) |
| **Room Runtime** | `androidx.room:room-runtime` | 2.7.2 | **Apache-2.0** | © The Android Open Source Project |
| **Room KTX** | `androidx.room:room-ktx` | 2.7.2 | **Apache-2.0** | © The Android Open Source Project |
| **Jetpack Compose BOM** | `androidx.compose:compose-bom` | 2026.03.01 | **Apache-2.0** | © The Android Open Source Project |
| **Compose Material 3** | `androidx.compose.material3:material3` | (via BOM) | **Apache-2.0** | © The Android Open Source Project |
| **Compose UI** | `androidx.compose.ui:ui` | (via BOM) | **Apache-2.0** | © The Android Open Source Project |
| **Compose Tooling Preview** | `androidx.compose.ui:ui-tooling-preview` | (via BOM) | **Apache-2.0** | © The Android Open Source Project |
| **AndroidX Core KTX** | `androidx.core:core-ktx` | 1.18.0 | **Apache-2.0** | © The Android Open Source Project |
| **AndroidX Activity Compose** | `androidx.activity:activity-compose` | 1.13.0 | **Apache-2.0** | © The Android Open Source Project |
| **AndroidX Lifecycle Runtime** | `androidx.lifecycle:lifecycle-runtime-ktx` | 2.10.0 | **Apache-2.0** | © The Android Open Source Project |
| **AndroidX Fragment KTX** | `androidx.fragment:fragment-ktx` | 1.8.6 | **Apache-2.0** | © The Android Open Source Project |
| **AndroidX Glance** | `androidx.glance:glance` | 1.1.1 | **Apache-2.0** | © The Android Open Source Project |
| **Glance AppWidget** | `androidx.glance:glance-appwidget` | 1.1.1 | **Apache-2.0** | © The Android Open Source Project |
| **Kotlin Coroutines Core** | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.10.2 | **Apache-2.0** | © JetBrains s.r.o. |
| **Desugar JDK Libs** | `com.android.tools:desugar_jdk_libs` | 2.1.4 | **Apache-2.0** | © The Android Open Source Project |

---

## 2. Build & Compilation Tooling

| Component | Identifier | Version | License | Note |
|---|---|---|---|---|
| **Android Gradle Plugin** | `com.android.application` | 9.0.1 | **Apache-2.0** | Build system |
| **Kotlin Gradle Plugin** | `org.jetbrains.kotlin.jvm` / `android` | 2.3.20 | **Apache-2.0** | Compiler plugin |
| **Compose Compiler Plugin** | `org.jetbrains.kotlin.plugin.compose` | 2.3.20 | **Apache-2.0** | UI compiler |
| **Kotlin Serialization Plugin** | `org.jetbrains.kotlin.plugin.serialization` | 2.3.20 | **Apache-2.0** | Serialization |
| **KSP (Kotlin Symbol Processing)**| `com.google.devtools.ksp` | 2.3.12 | **Apache-2.0** | Room code generator |
| **Room Compiler** | `androidx.room:room-compiler` | 2.7.2 | **Apache-2.0** | Annotation processor |

---

## 3. Test Dependencies (Non-Packaged)

These dependencies are used exclusively for local unit tests and on-device instrumented verification; they are not bundled into production release APKs.

| Artifact | Identifier | Version | License |
|---|---|---|---|
| **JUnit** | `junit:junit` | 4.13.2 | **EPL-1.0** |
| **Kotlin Coroutines Test** | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.2 | **Apache-2.0** |
| **AndroidX Test Core** | `androidx.test:core` | 1.7.0 | **Apache-2.0** |
| **AndroidX Test Ext JUnit** | `androidx.test.ext:junit` | 1.3.0 | **Apache-2.0** |
| **AndroidX Test Runner** | `androidx.test:runner` | 1.7.0 | **Apache-2.0** |
| **AndroidX Espresso Core** | `androidx.test.espresso:espresso-core` | 3.7.0 | **Apache-2.0** |

---

## 4. Typography & Font Families

The DailyRead design system (`warm_botanical_editorial`) specifies two primary font families for editorial typography:

### 4.1 Literata
- **Role:** Editorial headlines, book titles, journal dates, and long-form reading passages.
- **Foundry / Authors:** TypeTogether (Veronika Burian, José Scaglione, Irene Vlachou, Vera Evstafieva). Commissioned by Google.
- **License:** **SIL Open Font License (OFL), Version 1.1**
- **Verification:** Fully compliant for offline application bundling, embedding, and redistribution under OFL-1.1. No commercial or proprietary restrictions.
- **Fallback / Strategy:** Implemented in Compose using `FontFamily.Serif` fallback with clean seam for bundled TTF resources.

### 4.2 Manrope
- **Role:** Interface metadata, navigation, reading velocity, metric chips, and buttons.
- **Foundry / Authors:** Mikhail Sharanda, Mirko Velimirovic.
- **License:** **SIL Open Font License (OFL), Version 1.1**
- **Verification:** Fully compliant for offline application bundling, embedding, and redistribution under OFL-1.1. No commercial or proprietary restrictions.
- **Fallback / Strategy:** Implemented in Compose using `FontFamily.SansSerif` fallback with clean seam for bundled TTF resources.

---

## 5. Iconography

### Material Symbols
- **Role:** Standard application iconography (navigation icons, actions, progress indicators).
- **Author:** Google LLC
- **License:** **Apache License, Version 2.0**
- **Verification:** Distributed as vector graphics and Compose Material icons under Apache-2.0.

---

## 6. Owner-Provided Visual Assets & Provenance

| Asset / Directory | Provenance | License / Permitted Use | Notes |
|---|---|---|---|
| `stitch_dailyread_android_app_design/` | Dexter (Project Owner) via Stitch export | Proprietary to DailyRead / Apache-2.0 project asset | UI layouts, HTML mockups, rendered PNG screens, design token specifications (`warm_botanical_editorial/DESIGN.md`) |
| `img_assets/concept_02_nordic_paper_ink.png` | Dexter (Project Owner) | Proprietary to DailyRead / Apache-2.0 project asset | Visual concept alternative |
| `img_assets/pkg01_5_design_alternatives.png` | Dexter (Project Owner) | Proprietary to DailyRead / Apache-2.0 project asset | Visual design exploration palette |
| `img_assets/pkg01_master_visual_overview.png` | Dexter (Project Owner) | Proprietary to DailyRead / Apache-2.0 project asset | Master visual overview reference |

---

## 7. Full License Texts

### 7.1 Apache License, Version 2.0
```text
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### 7.2 BSD 3-Clause License (Readium Kotlin Toolkit)
```text
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### 7.3 SIL Open Font License, Version 1.1 (Literata, Manrope)
```text
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded, 
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the conditions in the license.
```
