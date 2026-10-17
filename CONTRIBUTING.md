# Contributing to HomeTutorPro 🤝

Thank you for your interest in improving HomeTutorPro! To maintain code quality and technical consistency, please follow these guidelines.

## 🚀 Development Process

1.  **Fork** the project.
2.  Create a branch for your feature: `git checkout -b feature/NewFeature`.
3.  Ensure you adhere to the **Kotlin Style Guide** standards.
4.  **Important:** Every new feature must include its respective unit tests.

## 🏗️ Architecture Standards

Any contribution must respect layer separation:
* Do not put business logic in ViewModels (use UseCases).
* All interactions with the database or API must go through a Repository.
* Use `State` in Compose to avoid unnecessary recompositions.

## 🧪 Testing

Before submitting a Pull Request, run the full test suite:
```bash
./gradlew test
./gradlew connectedAndroidTest
```
