# Contribuyendo a HomeTutorPro 🤝

¡Gracias por tu interés en mejorar HomeTutorPro! Para mantener la calidad del código y la coherencia técnica, sigue estas pautas.

## 🚀 Proceso de Desarrollo

1.  **Fork** del proyecto.
2.  Crea una rama para tu funcionalidad: `git checkout -b feature/NuevaFuncionalidad`.
3.  Asegúrate de seguir los estándares de **Kotlin Style Guide**.
4.  **Importante:** Cada nueva funcionalidad debe incluir sus respectivos tests unitarios.

## 🏗️ Estándares de Arquitectura

Cualquier contribución debe respetar la separación de capas:
* No pongas lógica de negocio en los ViewModels (usa UseCases).
* Toda interacción con la base de datos o API debe pasar por un Repositorio.
* Usa `State` en Compose para evitar recomposiciones innecesarias.

## 🧪 Testing

Antes de enviar un Pull Request, ejecuta la suite de tests completa:
```bash
./gradlew test
./gradlew connectedAndroidTest
