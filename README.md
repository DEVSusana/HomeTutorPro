<p align="center">
  <img src="./docs/screenshots/banner.png" width="100%" alt="HomeTutorPro Banner" />
</p>

<p align="center">
  <img src="./docs/screenshots/ic_app_icon.png" width="128" height="128" alt="HomeTutorPro Icon" />
</p>

<h1 align="center">HomeTutorPro</h1>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=android" alt="Build Status"></a>
  <a href="#"><img src="https://img.shields.io/badge/Coverage-82%25-blue?style=for-the-badge&logo=codecov" alt="Coverage"></a>
</p>

<p align="center">
  <strong>Gestión Educativa Inteligente y Segura</strong><br>
  Una solución integral para tutores modernos construida con Clean Architecture y Jetpack Compose.
</p>

---

## ✨ Características Principales

* **👥 Gestión de Alumnos:** Perfiles con historial y contacto.
* **📅 Horario Inteligente:** Vistas semanales y mensuales para control total.
* **💰 Control Financiero:** Seguimiento de saldos y precios por hora.
* **📂 Recursos Compartidos:** Gestión de archivos y materiales en la nube.
* **🔒 Seguridad:** Base de datos local cifrada con SQLCipher.
* **🌍 Multi-idioma:** Soporte completo para Español e Inglés.

## 📸 Screenshots

| Dashboard Principal | Horario Semanal | Calendario Mensual |
| :---: | :---: | :---: |
| ![Dashboard](./docs/screenshots/dashboard.png) | ![Semanal](./docs/screenshots/horario_semanal.png) | ![Mensual](./docs/screenshots/horario_mensual.png) |

| Perfil Estudiante | Gestión Financiera | Recursos Cloud |
| :---: | :---: | :---: |
| ![Perfil](./docs/screenshots/perfil_personal.png) | ![Finanzas](./docs/screenshots/finanzas_alumno.png) | ![Recursos](./docs/screenshots/recursos_compartidos.png) |

## 🏗️ Arquitectura y Tech Stack

El proyecto utiliza **Clean Architecture** con **MVVM** y **UDF** (Unidirectional Data Flow).

* **UI:** Jetpack Compose para una interfaz moderna y reactiva.
* **DI:** Hilt para inyección de dependencias.
* **DB:** Room con cifrado SQLCipher (Soporte Android 15+).
* **Backend:** Firebase (Auth, Firestore, Storage).
* **Testing:** Suite de +150 tests (Unitarios e Instrumentados).

## 🚀 Instalación

1. Clona el repositorio.
2. Añade tu `google-services.json` de Firebase en `/app`.
3. Sincroniza Gradle y ejecuta el proyecto.
3.  Sincroniza el proyecto con Gradle en Android Studio.
4.  Ejecuta la variante de `debug` en tu emulador o dispositivo físico.
