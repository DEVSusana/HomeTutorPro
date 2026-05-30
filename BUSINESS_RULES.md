# Reglas y Directivas de Negocio — HomeTutorPro

**Objetivo:** Este documento define las reglas de negocio y políticas del dominio de la aplicación HomeTutorPro. Los casos de uso (Use Cases) y la lógica de la aplicación deben respetar de forma inflexible estas directrices.

---

## 1. Gestión de Estudiantes y Tutores
* **Identificación Única:** Cada Estudiante y Tutor debe estar asociado a una cuenta de usuario única e identificada mediante un UID único generado por autenticación (Auth).
* **Campos Obligatorios:** No se permiten perfiles de estudiantes sin nombre, dirección y correo electrónico válido.

---

## 2. Gestión de Clases y Tutorías
* **Sin Solapamientos:** Un tutor no puede tener agendadas dos tutorías en un mismo bloque de tiempo (rango de horas solapado). La lógica de reserva debe validar el horario antes de registrar la tutoría.
* **Estados de la Clase:** Una tutoría agendada solo puede pasar por los siguientes estados: `Scheduled` (Agendada) -> `InProgress` (En progreso) -> `Completed` (Completada) o `Cancelled` (Cancelada).
* **Cancelaciones:** No se permite la cancelación de una tutoría por parte de un estudiante si faltan menos de 2 horas para el inicio de la sesión, a menos que sea un usuario Premium.

---

## 3. Funciones Premium y Facturación (Billing)
* **Acceso Limitado:** Los usuarios no Premium (gratuitos) tienen acceso a un máximo de 3 tutorías agendadas activas simultáneamente.
* **Acceso Premium:** Los usuarios Premium disfrutan de agendamiento ilimitado y acceso completo al asistente interactivo de IA (Sue).
* **Estado de Suscripción:** Toda validación de lógica de compra debe pasar a través de `PremiumBillingService`. Si el servicio falla o no devuelve confirmación activa, el usuario debe tratarse como gratuito por defecto (Fallback Seguro).

---

## 4. Asistente IA Integrado (Sue)
* **Activación:** El asistente Sue (`SueViewModel`) solo debe procesar peticiones si el usuario tiene una sesión de autenticación activa.
* **Control de Contexto:** Las interacciones del asistente IA con la base de datos de estudiantes y clases deben realizarse a través de los casos de uso (`QueryStudentsForAgentUseCase` y `ManageScheduleForAgentUseCase`) para asegurar que se respetan las reglas de visibilidad y acceso del usuario actual.

---

## 5. Sincronización e Integridad de Datos
* **Persistencia local prioritaria:** Ante fallos de conexión a Internet, la aplicación debe guardar todos los cambios en la base de datos local (Room) y marcar los registros correspondientes con una bandera de sincronización pendiente (`isSyncPending`).
* **Resolución de Conflictos:** Al sincronizar con la nube (Firestore), si hay conflicto entre los datos locales y remotos, prevalecerá el dato con la marca de tiempo (`timestamp`) de modificación más reciente.
