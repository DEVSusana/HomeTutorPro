# Reglas y Directivas de Negocio — HomeTutorPro

**Objetivo:** Este documento define las reglas de negocio y políticas del dominio de la aplicación HomeTutorPro. Los casos de uso (Use Cases) y la lógica de la aplicación deben respetar de forma inflexible estas directrices.

---

## 1. Gestión de Estudiantes y Tutores
* **Identificación Única:** Cada Estudiante y Tutor debe estar asociado a una cuenta de usuario única e identificada mediante un UID único generado por autenticación (Auth).
* **Campos Obligatorios:** No se permiten perfiles de estudiantes sin nombre, dirección y correo electrónico válido.

---

## 2. Gestión de Clases y Calendario (Modelo Excepciones vs Horario Base)
La gestión del calendario diferencia estrictamente entre el **Horario Base (Plantilla Recurrente)** y las **Sesiones Reales del Calendario (Excepciones Temporales)**.

### A. Horario Base Recurrente (`Schedule`):
* Cada alumno tiene configurado un horario base fijo (día de la semana, hora de inicio y fin) en su perfil. Este horario se repite de manera infinita en el futuro y solo cambia si se edita el perfil de forma permanente en los detalles del alumno.

### B. Ocurrencias y Excepciones Temporales (`ScheduleException`):
Las modificaciones en el calendario operan únicamente sobre la fecha de una sesión concreta (`date: Long` como timestamp) a través de excepciones temporales, sin alterar el horario base del alumno para las semanas siguientes.

* **Cancelación de Sesión (`CANCELLED`):**
  - Cancela la clase de un alumno para una fecha específica.
  - **Liberación de Hueco:** Libera inmediatamente el hueco de esa hora y fecha específicas (`isFreeSlot = true`), permitiendo que el tutor asigne ese espacio a otro estudiante de forma temporal.
* **Reubicación o Cambio de Horario/Día (`RESCHEDULED`):**
  - Mueve la clase de un alumno a otra hora o a otro día de la semana (`newDayOfWeek`) para una fecha específica.
  - **Validación de Solapamiento:** Se debe calcular la fecha de destino (`targetDate`) y asegurar que el nuevo bloque horario no colisione con el horario regular o con las excepciones de otros alumnos en esa fecha.
  - **Liberación de Hueco de Origen:** El bloque horario original de origen de esa sesión queda marcado automáticamente como libre para esa fecha específica, permitiendo que sea ocupado por otro alumno de forma temporal.
* **Clase Extra (`EXTRA`):**
  - Añade una sesión de tutoría adicional para un alumno en una fecha específica, configurada con el ID especial de origen `EXTRA`.
  - **No liberación de hueco habitual:** Al ser una clase adicional, no se asocia al horario recurrente del alumno, por lo que la sesión de su horario base habitual sigue activa y ocupada para esa fecha.

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
