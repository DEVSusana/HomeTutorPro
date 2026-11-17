package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import javax.inject.Inject

/**
 * Internal helper for [SueRuleIntentDetector] that encapsulates all keyword detection
 * and regex matching for Sue intent classification.
 */
internal class SueIntentKeywords @Inject constructor(
    private val entityExtractor: SueEntityExtractor
) {

    fun containsScheduleKeywords(query: String) =
        listOf(
            "horario", "schedule", "clase", "clases", "class",
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "semana", "week", "hoy", "today", "mañana", "tomorrow", "tarde", "esta tarde",
            "ayer", "esta mañana", "luego", "que viene", "pasado", "el próximo", "la próxima",
            "próxima clase", "siguiente clase", "hueco", "libre"
        ).any { it in query }

    fun containsNextClassKeywords(query: String) =
        listOf(
            "proxima clase", "próxima clase", "siguiente clase",
            "proxima", "próxima", "siguiente",
            "cuando tengo", "cuándo tengo",
            "cuando es", "cuándo es",
            "cuando empieza", "cuándo empieza",
            "next class"
        ).any { it in query }

    fun containsFreeSlotKeywords(query: String): Boolean {
        val days = listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana")
        if (days.any { it in query }) {
            return false
        }
        return listOf(
            "hueco libre", "huecos libres", "día libre", "días libres",
            "tengo libre", "estoy libre", "tengo disponible", "estoy disponible",
            "espacio libre", "rato libre", "momento libre",
            "free slot", "available slot", "am i free", "am i available"
        ).any { it in query }
    }

    fun containsFreeSlotWithDayKeywords(query: String): Boolean {
        val hasFreeIntent = listOf(
            "hueco", "huecos", "libre", "libres", "disponible", "disponibles",
            "tiempo libre", "espacio", "rato", "momento", "free", "available"
        ).any { it in query }
        val hasDay = listOf(
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo", "hoy", "mañana", "esta tarde", "esta mañana", "tarde"
        ).any { it in query }
        return hasFreeIntent && hasDay
    }

    fun containsCancelKeywords(query: String): Boolean {
        if (containsCancelledQueryKeywords(query)) {
            return false
        }
        if (listOf("cancelada", "canceladas", "cancelado", "cancelados", "cancelacion", "cancelación", "cancelaciones").any { it in query }) {
            return false
        }
        val directPhrases = listOf(
            "cancela ", "cancela la", "cancela el", "cancela a", "cancela con",
            "cancelar ", "cancelar la", "cancelar el", "cancelar a", "cancelar con",
            "cancelale", "cancélale", "cancelala", "cancélala",
            "anula ", "anula la", "anula el", "anula a", "anula con",
            "anular ", "anular la", "anular el", "anular a", "anular con",
            "anulale", "anúlale", "anulala", "anúlala",
            "quitar la clase", "quita la clase", "quitar clase", "quita clase",
            "quitarle la clase", "quítale la clase", "quitale la clase",
            "quítala", "quitala", "quítale", "quitale", "quitarle",
            "quita a ", "quitar a ", "quitale a ", "quítale a ",
            "quita con ", "quitar con ", "quitale con ", "quítale con ",
            "quita la de ", "quítale la de ", "quitale la de ",
            "borra la clase", "borrar la clase", "bórrale la clase", "borrale la clase",
            "elimina la clase", "eliminar la clase", "elimínale la clase", "eliminale la clase",
            "no voy a poder dar clase", "no puedo dar clase", "no podre dar clase", "no podré dar clase",
            "cancel ", "cancel the", "cancel a",
            "remove class", "delete class"
        )
        if (directPhrases.any { it in query }) return true

        val hasCancelVerb = listOf(
            "quita", "quitar", "quitale", "quítale",
            "cancela", "cancelar", "cancelale", "cancélale",
            "anula", "anular", "anulale", "anúlale"
        ).any { it in query }
        val hasClassWord = listOf("clase", "clases", "horario", "la de ").any { it in query }
        return hasCancelVerb && hasClassWord
    }

    fun containsCancelledQueryKeywords(query: String): Boolean {
        val hasExceptionQuery = listOf(
            "cancelada", "canceladas", "cancelado", "cancelados",
            "reprogramada", "reprogramadas", "reprogramado", "reprogramados",
            "clase cancelada", "clases canceladas",
            "hay cancelada", "hay canceladas", "tengo cancelada", "tengo canceladas",
            "algo cancelado", "algo cancelada",
            "cancele", "cancelé", "he cancelado", "he cancelada",
            "anulada", "anuladas", "anulado", "anulados", "anule", "anulé",
            "cancelacion", "cancelación", "cancelaciones",
            "que cancelaciones", "qué cancelaciones", "cuales canceladas", "cuáles canceladas",
            "clase extra", "clases extra", "clases extras", "clase adicional", "clases adicionales",
            "reprogramaciones", "excepciones", "excepcion", "excepción"
        ).any { it in query }

        val isActionCommand = listOf(
            "cancela ", "cancela la", "cancela el", "cancela a",
            "cancelar ", "cancelar la", "cancelar el", "cancelar a",
            "anula ", "anula la", "anula el", "anula a",
            "añade una clase extra", "pon una clase extra", "agrega una clase extra",
            "añadir clase extra", "poner clase extra"
        ).any { it in query }

        return hasExceptionQuery && !isActionCommand
    }

    fun containsRescheduleKeywords(query: String): Boolean {
        val specificKeywords = listOf(
            "mueve", "mover", "muevele", "muévele", "muevela", "muévela", "muevesela", "muévesela", "mueveme", "muéveme", "mueveles",
            "traslada", "trasladar", "trasladale", "trasládale",
            "reprograma", "reprogramar", "reprograme", "reprogramale", "reprográmale", "reprogramala", "reprográmala",
            "pospone", "posponer", "pospón", "pospon", "posponle", "pospónle",
            "adelanta", "adelantar", "adelantale", "adelántale",
            "pasa", "pasar", "pásame", "pasame", "pasale", "pásale", "pasala", "pásala",
            "reschedule", "postpone"
        )
        if (specificKeywords.any { it in query }) return true

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cambiale", "cámbiale", "cambiala", "cámbiala", "cambiame", "cámbiame", "change", "move"
        ).any { it in query }
        val hasScheduleContext = listOf(
            "la clase", "el horario", "la hora", "las horas", "la de ", "el de ",
            "al lunes", "al martes", "al miércoles", "al miercoles", "al jueves", "al viernes", "al sábado", "al sabado", "al domingo",
            "para el lunes", "para el martes", "para el miércoles", "para el miercoles", "para el jueves", "para el viernes", "para el sábado", "para el sabado", "para el domingo",
            "para hoy", "para mañana", "para manana",
            "a las ", "de las ",
            "class", "schedule", "the class",
            "to monday", "to tuesday", "to wednesday", "to thursday", "to friday", "to saturday", "to sunday",
            "to today", "to tomorrow",
            "to ", "at ", "from "
        ).any { it in query }
        return hasChangeVerb && hasScheduleContext
    }

    fun containsRegisterPaymentKeywords(query: String): Boolean {
        val safeKeywords = listOf(
            "registra un pago", "registrar un pago",
            "ha pagado", "abonó", "abono", "pago de",
            "register payment", "made a payment", "payment of"
        )
        if (safeKeywords.any { it in query }) return true

        val hasPago = "pagó" in query || "paid" in query
        val hasTimeOrAmount = listOf(
            "hoy", "ayer", "esta semana", "acaba de", "ahora",
            "€", "euro", "euros", "dollar", "dollars"
        ).any { it in query }
        return hasPago && hasTimeOrAmount
    }

    fun containsAddBalanceKeywords(query: String): Boolean {
        val hasVerb = listOf(
            "suma", "sumar", "anade", "añade", "anadir", "añadir",
            "agrega", "agregar", "sumale", "súmale", "add"
        ).any { it in query }
        val hasBalanceTarget = listOf("saldo", "deuda", "balance", "debt").any { it in query }
        if (hasVerb && hasBalanceTarget) return true

        return listOf(
            "suma saldo", "sumar saldo", "añade saldo", "anade saldo", "añadir saldo", "anadir saldo",
            "suma a la deuda", "añade a la deuda", "anade a la deuda", "súmale", "sumale",
            "add balance", "add to debt", "add to balance", "add to the debt"
        ).any { it in query }
    }

    fun containsUpdateNotesKeywords(query: String): Boolean {
        val safeKeywords = listOf(
            "apunte", "apuntes", "observacion", "observaciones",
            "observation", "observations"
        )
        if (safeKeywords.any { it in query }) return true

        val hasNoteWord = listOf("nota", "notas", "comentario", "comentarios", "note", "notes", "comment", "comments").any { it in query }
        val hasWriteVerb = listOf(
            "añade", "añadir", "agrega", "agregar", "pon", "poner",
            "escribe", "escribir", "actualiza", "actualizar",
            "modifica", "modificar", "cambia", "cambiar",
            "add", "write", "update", "change"
        ).any { it in query }
        return hasNoteWord && hasWriteVerb
    }

    fun containsUpdateAddressKeywords(query: String): Boolean {
        if (query.startsWith("cual es la direccion") || query.startsWith("cuál es la dirección") ||
            query.startsWith("dónde vive") || query.startsWith("donde vive")) {
            return false
        }

        val hasDirectAction = listOf(
            "cambia la direccion", "cambia la dirección",
            "cambiar la direccion", "cambiar la dirección",
            "cámbiale la direccion", "cámbiale la dirección", "cambiale la direccion", "cambiale la dirección",
            "actualiza la direccion", "actualiza la dirección",
            "actualizar la direccion", "actualizar la dirección",
            "actualízale la direccion", "actualízale la dirección", "actualizale la direccion", "actualizale la dirección",
            "modifica la direccion", "modifica la dirección",
            "modificar la direccion", "modificar la dirección",
            "modifícale la direccion", "modifícale la dirección", "modificale la direccion", "modificale la dirección",
            "ponle la direccion", "ponle la dirección", "pónle la direccion", "pónle la dirección",
            "pon la direccion", "pon la dirección",
            "pon de direccion", "pon de dirección",
            "nueva direccion", "nueva dirección",
            "edita la direccion", "edita la dirección",
            "editar la direccion", "editar la dirección",
            "edítale la direccion", "edítale la dirección", "editale la direccion", "editale la dirección",
            "guarda la direccion", "guarda la dirección",
            "guardar la direccion", "guardar la dirección",
            "guárdale la direccion", "guárdale la dirección", "guardale la direccion", "guardale la dirección",
            "anade la direccion", "añade la direccion", "anade la dirección", "añade la dirección",
            "anadir direccion", "añadir direccion", "anadir dirección", "añadir dirección",
            "añádele la direccion", "añádele la dirección", "añadele la direccion", "añadele la dirección",
            "update address", "change address", "set address", "edit address"
        ).any { it in query }

        val hasPorteroAction = listOf(
            "el portero de ", "portero de ", "pon el portero", "ponle el portero", "pónle el portero",
            "cambia el portero", "actualiza el portero", "edita el portero", "edítale el portero", "editale el portero",
            "añade el portero", "añádele el portero", "añadele el portero", "anade el portero",
            "el telefonillo de ", "pon el telefonillo", "ponle el telefonillo", "pónle el telefonillo",
            "cambia el telefonillo", "actualiza el telefonillo", "edita el telefonillo", "edítale el telefonillo",
            "añade el telefonillo", "añádele el telefonillo"
        ).any { it in query }

        val hasAddressStatement = (query.startsWith("la direccion de ") || query.startsWith("la dirección de ") ||
                query.startsWith("el portero de ") || query.startsWith("el telefonillo de ")) &&
                listOf(" es ", " seria ", " será ").any { it in query }

        val hasWriteVerb = listOf(
            "pon", "pone", "poner", "ponle", "pónle", "ponme", "pónme", "ponsela", "pónsela",
            "anade", "añade", "anadir", "añadir", "añádele", "añadele", "anadele", "anádele", "añadela", "añádela", "añademe", "añádeme",
            "agrega", "agregar", "agrégale", "agregale",
            "actualiza", "actualizar", "actualízale", "actualizale", "actualízala", "actualizala",
            "cambia", "cambiar", "cámbiale", "cambiale", "cámbiala", "cambiala",
            "modifica", "modificar", "modifícale", "modificale", "modifícala", "modificala",
            "guarda", "guardar", "guárdale", "guardale", "guárdala", "guardala",
            "edita", "editar", "edítale", "editale", "edítala", "editala", "edítame", "editame",
            "mete", "meter", "métele", "metele", "métela", "metela"
        ).any { it in query }

        val hasAddressTarget = listOf(
            "en su direccion", "en su dirección", "a su direccion", "a su dirección",
            "de direccion", "de dirección", "su direccion", "su dirección",
            "la direccion", "la dirección",
            "el portero", "de portero", "el telefonillo", "de telefonillo", "el portal", "el timbre", "el intercom",
            "codigo de portero", "código de portero", "codigo del portero", "código del portero",
            "codigo de porte", "código de porte", "codigo de acceso", "código de acceso",
            "codigo de portal", "código de portal", "codigo portal", "código portal"
        ).any { it in query }

        val isAddressUpdatePhrase = hasWriteVerb && hasAddressTarget

        val hasRelocationWord = listOf(
            "se ha mudado", "se muda", "se mudó", "se mudo",
            "ha cambiado de casa", "cambia de casa", "cambió de casa", "cambio de casa",
            "la nueva casa de", "nueva casa de", "su nueva casa es",
            "ahora vive en", "ha pasado a vivir en", "vive ahora en"
        ).any { it in query }

        return hasDirectAction || hasPorteroAction || hasAddressStatement || isAddressUpdatePhrase || hasRelocationWord
    }

    fun containsUpdatePriceKeywords(query: String): Boolean {
        if (containsCreateStudentKeywords(query) || containsRegisterPaymentKeywords(query) || containsAddBalanceKeywords(query)) {
            return false
        }
        val hasPriceWord = listOf(
            "precio", "tarifa", "por hora", "/hora", "/h", "euros la hora", "euros hora", "la hora"
        ).any { it in query }

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cámbiale", "cambiale",
            "actualiza", "actualizar", "actualízale", "actualizale",
            "modifica", "modificar", "modifícale", "modificale",
            "pon", "pone", "poner", "ponle", "pónle",
            "edita", "editar", "edítale", "editale",
            "sube", "subir", "súbele", "subele",
            "baja", "bajar", "bájale", "bajale",
            "ajusta", "ajustar", "ajústale", "ajustale"
        ).any { it in query }

        val isPriceStatement = (query.startsWith("el precio de ") || query.startsWith("la tarifa de ")) &&
                listOf(" es ", " seria ", " será ", " pasa a ser ").any { it in query }

        return (hasPriceWord && hasChangeVerb) || isPriceStatement
    }

    fun containsUpdatePhoneKeywords(query: String): Boolean {
        val hasPhoneWord = listOf(
            "telefono", "teléfono", "movil", "móvil", "tlf", "celular", "numero", "número"
        ).any { it in query }

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cámbiale", "cambiale",
            "actualiza", "actualizar", "actualízale", "actualizale",
            "modifica", "modificar", "modifícale", "modificale",
            "pon", "pone", "poner", "ponle", "pónle",
            "guarda", "guardar", "guárdale", "guardale",
            "edita", "editar", "edítale", "editale"
        ).any { it in query }

        val isPhoneStatement = listOf(
            "el telefono de ", "el teléfono de ", "el movil de ", "el móvil de ",
            "el nuevo telefono de ", "el nuevo teléfono de ", "el nuevo movil de ", "el nuevo móvil de ",
            "el numero de ", "el número de ", "el nuevo numero de ", "el nuevo número de ",
            "el celular de ", "el nuevo celular de "
        ).any { query.startsWith(it) } && listOf(" es ", " seria ", " será ", " es: ", ": ").any { it in query }

        return (hasPhoneWord && hasChangeVerb) || isPhoneStatement
    }

    fun containsUpdateCourseKeywords(query: String): Boolean {
        if (containsCreateStudentKeywords(query)) return false
        val hasCourseWord = listOf(
            "curso", "nivel", "grado"
        ).any { it in query }

        val hasChangeVerb = listOf(
            "cambia", "cambiar", "cámbiale", "cambiale",
            "actualiza", "actualizar", "actualízale", "actualizale",
            "modifica", "modificar", "modifícale", "modificale",
            "pon", "pone", "poner", "ponle", "pónle",
            "edita", "editar", "edítale", "editale"
        ).any { it in query }

        val isCourseStatement = listOf(
            "el curso de ", "el nivel de ", "el grado de ",
            "el nuevo curso de ", "el nuevo nivel de ", "el nuevo grado de "
        ).any { query.startsWith(it) } && listOf(" es ", " seria ", " será ", " es: ", ": ").any { it in query }

        return (hasCourseWord && hasChangeVerb) || isCourseStatement
    }

    fun containsStartClassKeywords(query: String) =
        listOf("inicia una clase", "inicia clase", "empieza clase", "comienza clase", "start class").any { it in query }

    fun containsCreateStudentKeywords(query: String) =
        listOf(
            "crear un nuevo estudiante", "crear un nuevo alumno", "crear una nueva estudiante", "crear una nueva alumna",
            "crea un nuevo estudiante", "crea un nuevo alumno", "crea una nueva estudiante", "crea una nueva alumna",
            "crear un estudiante", "crear un alumno", "crea al alumno", "crea el alumno", "crea un alumno",
            "crea al estudiante", "crea el estudiante", "crea un estudiante", "crear al estudiante", "crear al alumno",
            "crear a un estudiante", "crear a un alumno", "crea a un estudiante", "crea a un alumno",
            "crea alumno", "crear alumno", "crea estudiante", "crear estudiante",
            "anade un alumno", "añade un alumno", "anade al alumno", "añade al alumno",
            "anade un estudiante", "añade un estudiante", "anade al estudiante", "añade al estudiante",
            "anadir estudiante", "añadir estudiante", "anadir alumno", "añadir alumno",
            "anade estudiante", "añade estudiante", "anade alumno", "añade alumno",
            "agrega un alumno", "agregar un alumno", "agrega al alumno", "agregar al alumno", "agrega alumno", "agregar alumno",
            "registra un alumno", "registrar un alumno", "registra al alumno", "registrar al alumno", "registra alumno", "registrar alumno",
            "un alumno nuevo", "un nuevo alumno", "una nueva alumna", "una alumna nueva",
            "nuevo alumno", "nuevo estudiante", "nueva estudiante", "nueva alumna",
            "create student", "add student"
        ).any { it in query }

    fun containsDeleteStudentKeywords(query: String) =
        listOf("elimina al alumno", "elimina al estudiante", "borra al alumno", "borra al estudiante", "eliminar alumno", "delete student", "remove student").any { it in query }

    fun containsAddExtraClassKeywords(query: String): Boolean {
        val hasWriteVerb = listOf(
            "anade", "añade", "anadir", "añadir", "agrega", "agregar",
            "pon", "pone", "poner", "ponle", "pónle", "ponme", "pónme", "ponles", "ponsela", "pónsela",
            "programa", "programar", "prográmale", "programale",
            "crea", "crear", "créale", "creale",
            "mete", "meter", "métele", "metele",
            "incluye", "incluir",
            "add", "create", "schedule", "insert"
        ).any { it in query }

        val hasExtra = listOf(
            "clase extra", "clases extra", "clases extras",
            "clase de refuerzo", "clases de refuerzo", "refuerzo",
            "clase adicional", "clases adicionales",
            "tutoria adicional", "tutoría adicional", "tutorias adicionales", "tutorías adicionales",
            "extra class", "extra classes", "additional class", "additional classes"
        ).any { it in query }

        val hasClassTarget = listOf(
            "una clase a ", "una clase para ", "una clase con ", "una clase de ",
            "clase a ", "clase para ", "clase con ", "clase de ",
            "una tutoria a ", "una tutoría a ", "tutoria a ", "tutoría a ",
            "tutoria con ", "tutoría con ", "tutoria para ", "tutoría para ",
            "a class for ", "a class to ", "a class with "
        ).any { it in query }

        val hasInvertedTarget = Regex("""\b(?:pon|ponle|pónle|ponme|añade|anade|agrega|programa)\s+a\s+\w+\s+(?:una\s+)?(?:clase|tutoria|tutoría)\b""").containsMatchIn(query)

        val hasDirectPonStudent = Regex("""\b(?:pon|ponle|pónle|ponme)\s+a\s+\w+\b""").containsMatchIn(query) &&
                (entityExtractor.extractDayOfWeek(query) != null || entityExtractor.extractRelativeDayOfWeek(query) != null || entityExtractor.extractTime(query) != null)

        val hasGenericScheduleIntent = hasWriteVerb && listOf("una clase", "clase").any { it in query } &&
                (entityExtractor.extractDayOfWeek(query) != null || entityExtractor.extractRelativeDayOfWeek(query) != null || entityExtractor.extractTime(query) != null)

        return hasWriteVerb && (hasExtra || hasClassTarget || hasInvertedTarget || hasDirectPonStudent || hasGenericScheduleIntent)
    }

    fun containsCreateScheduleKeywords(query: String) =
        listOf(
            "anade un horario", "añade un horario", "anadir horario", "añadir horario",
            "programa una clase los", "crear horario", "create schedule", "add schedule"
        ).any { it in query }

    fun containsDeleteScheduleKeywords(query: String) =
        listOf(
            "elimina el horario", "borra el horario",
            "quita el horario", "quitar el horario",
            "quitar horario", "delete schedule", "remove schedule"
        ).any { it in query }

    fun containsStudentCountKeywords(query: String) =
        listOf("cuántos alumnos", "cuantos alumnos", "número de alumnos", "numero de alumnos", "cantidad de alumnos").any { it in query }

    fun containsStudentDetailsKeywords(query: String) =
        listOf(
            "información de", "informacion de",
            "ficha de",
            "datos de",
            "detalles de",
            "info de",
            "dirección de", "direccion de",
            "dónde vive", "donde vive",
            "portero de", "portal de",
            "teléfono de", "telefono de",
            "contacto de"
        ).any { it in query }

    fun containsStudentWeeklyClassesKeywords(query: String): Boolean {
        val hasQuantityWord = listOf("cuántas", "cuantas", "cuántos", "cuantos", "how many").any { it in query }
        val hasClassWord = listOf("clase", "clases", "sesión", "sesiones", "veces", "class", "classes", "sessions", "times").any { it in query }
        val hasStudentReference = listOf(
            "de ", "con ", "tiene ", "tengo de ", "tengo con ",
            " a ", "al ", "le he dado", "he dado", "dado a", "dadas a", "le di", "di a",
            "impartido", "impartidas", "llamado", "llamada", "alumno", "estudiante",
            "for ", "with ", "does ", "have for ", "given to ", "taught to ", "teach to ", "teach ", "to ", "student named ", "called "
        ).any { it in query }
        return hasQuantityWord && hasClassWord && hasStudentReference
    }

    fun containsDayScheduleKeywords(query: String): Boolean {
        val excludes = listOf(
            "cancelada", "canceladas", "cancelado", "cancelados",
            "hueco", "huecos", "libre", "libres",
            "cuántas", "cuantas", "cuántos", "cuantos",
            "how many"
        )
        if (excludes.any { it in query }) {
            return false
        }
        return listOf(
            "clase", "clases", "horario", "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana",
            "class", "classes", "schedule", "today", "tomorrow"
        ).any { it in query }
    }

    fun containsStudentsWithBalanceKeywords(query: String) =
        listOf(
            "quién me debe", "quien me debe",
            "quiénes me deben", "quienes me deben",
            "quién debe", "quien debe",
            "tienen deuda", "deben dinero",
            "deuda pendiente", "saldo pendiente",
            "pendiente de pago", "sin pagar",
            "no han pagado", "no ha pagado",
            "who owes me", "who hasn't paid"
        ).any { it in query }

    fun containsStudentBalanceKeywords(query: String) =
        listOf(
            "cuánto me debe", "cuanto me debe",
            "cuánto debe", "cuanto debe",
            "qué me debe", "que me debe",
            "saldo de", "deuda de", "balance de",
            "cuánto tiene pendiente", "cuanto tiene pendiente",
            "how much does", "how much owes"
        ).any { it in query }

    fun containsTodaySummaryKeywords(query: String) =
        listOf(
            "resumen de hoy", "resumen hoy",
            "cómo está mi día", "como esta mi dia",
            "cómo va mi día", "como va mi dia",
            "qué tengo hoy", "que tengo hoy",
            "summary today", "today's summary"
        ).any { it in query }
}
