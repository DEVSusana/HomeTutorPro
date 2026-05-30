# Reglas y Directivas de Negocio — HomeTutorPro

**Objetivo:** Este documento define las reglas de negocio y políticas del dominio de la aplicación HomeTutorPro. Los casos de uso (Use Cases) y la lógica de la aplicación deben respetar de forma inflexible estas directrices.

---

## 1. Gestión de Estudiantes y Tutores
* **Identificación Única:** Cada Estudiante y Tutor debe estar asociado a una cuenta de usuario única e identificada mediante un UID único generado por autenticación (Auth).
* **Campos Obligatorios:** No se permiten perfiles de estudiantes sin nombre, dirección y correo electrónico válido.

---

## 2. Gestión de Clases y Tutorías
* **Sin Solapamientos:** Un tutor no puede tener agendadas dos tutorías en un mismo bloque de tiempo (rango de horas solapado). La lógica de reserva debe validar el horario antes de registrar la tutoría.
* **Reprogramación y Movimiento de Clases:** Se permite reprogramar o mover tutorías a una nueva fecha u hora. Al realizar un cambio de horario:
  - Se debe validar rigurosamente que el nuevo bloque horario de destino esté libre y no se solape con otras tutorías.
  - El bloque horario de origen ocupado previamente por la tutoría debe liberarse inmediatamente para que quede disponible para otras reservas, permitiendo reordenar el calendario de forma eficiente.
* **Estados de la Clase:** Una tutoría agendada solo puede pasar por los siguientes estados: `Scheduled` (Agendada) -> `InProgress` (En progreso) -> `Completed` (Completada) o `Cancelled` (Cancelada).

---

## 3. Estado de la Aplicación y Monetización
* **Versión Normal Única:** Actualmente, la aplicación funciona como una versión estándar única sin distinción entre planes gratuitos y premium.
* **Suscripciones Desactivadas:** Aunque existan clases y servicios relacionados con facturación (`PremiumBillingService`), no se aplican restricciones de uso ni límites al agendamiento.
* **Control de Costes Cloud:** El motor de sincronización de Firebase/Firestore y servicios adicionales en la nube están desactivados temporalmente por motivos de costes de infraestructura. Todo el funcionamiento debe operar en modo local priorizando la base de datos Room.

---

## 4. Asistente IA Integrado (Sue)
* **Activación:** El asistente Sue (`SueViewModel`) solo debe procesar peticiones si el usuario tiene una sesión de autenticación activa.
* **Control de Contexto:** Las interacciones del asistente IA con la base de datos de estudiantes y clases deben realizarse a través de los casos de uso (`QueryStudentsForAgentUseCase` y `ManageScheduleForAgentUseCase`) para asegurar que se respetan las reglas de visibilidad y acceso del usuario actual.

---

## 5. Sincronización e Integridad de Datos
* **Persistencia local prioritaria:** Todo el almacenamiento de datos opera en modo local sobre la base de datos Room debido a la desactivación temporal de los servicios Cloud.
* **Preparación para la Nube:** En caso de que se reactive la sincronización, los datos locales pendientes deben marcarse con `isSyncPending` y, ante conflictos, prevalecerá el dato con el `timestamp` de modificación más reciente.
