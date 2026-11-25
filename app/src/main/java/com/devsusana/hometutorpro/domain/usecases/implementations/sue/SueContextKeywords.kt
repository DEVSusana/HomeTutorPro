package com.devsusana.hometutorpro.domain.usecases.implementations.sue

/**
 * Keyword matching helpers used by [SueContextBuilder] to decide which local Room context
 * blocks must be injected into the LLM prompt.
 */
internal object SueContextKeywords {

    fun containsScheduleKeywords(query: String) =
        listOf(
            "horario", "schedule", "clase", "clases", "class",
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "semana", "week", "hoy", "today", "mañana", "manana", "tomorrow", "tarde", "esta tarde",
            "ayer", "esta mañana", "esta manana", "luego", "que viene", "pasado", "el próximo", "el proximo", "la próxima", "la proxima",
            "próxima clase", "proxima clase", "siguiente clase", "hueco", "libre"
        ).any { it in query }

    fun containsNextClassKeywords(query: String) =
        listOf(
            "próxima clase", "proxima clase", "siguiente clase", "próxima", "proxima", "siguiente",
            "próximo", "proximo", "la próxima", "la proxima", "el próximo", "el proximo",
            "cuándo tengo", "cuando tengo", "cuándo es", "cuando es", "cuándo empieza", "cuando empieza",
            "a qué hora", "a que hora", "a qué hora es", "a que hora es", "a qué hora tengo", "a que hora tengo",
            "qué clase tengo", "que clase tengo", "next class"
        ).any { it in query }

    fun containsFreeSlotKeywords(query: String): Boolean {
        val days = listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana", "manana")
        if (days.any { it in query }) {
            return false
        }
        return listOf(
            "hueco libre", "huecos libres", "día libre", "dia libre", "días libres", "dias libres",
            "tengo libre", "estoy libre", "tengo disponible", "estoy disponible",
            "espacio libre", "rato libre", "momento libre",
            "free slot", "available slot", "am i free", "am i available"
        ).any { it in query }
    }

    fun containsFreeSlotWithDayKeywords(query: String): Boolean {
        val hasFreeIntent = listOf(
            "hueco", "huecos", "libre", "libres", "disponible", "disponibles",
            "espacio", "rato", "momento", "free", "available"
        ).any { it in query }
        val hasDay = listOf(
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo", "hoy", "mañana", "manana"
        ).any { it in query }
        return hasFreeIntent && hasDay
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
            "reprogramacion", "reprogramación", "reprogramaciones", "excepciones", "excepcion", "excepción"
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

    fun containsAllStudentsKeywords(query: String) =
        listOf(
            "alumno", "alumnos", "estudiante", "estudiantes",
            "student", "students", "quién", "quien", "lista", "list",
            "todos", "all"
        ).any { it in query }

    fun containsBalanceKeywords(query: String) =
        listOf(
            "saldo", "balance", "deuda", "debt", "dinero", "money",
            "cobrar", "pagar", "pago", "payment", "pendiente", "pending", "debe", "owe"
        ).any { it in query }

    fun containsCountKeywords(query: String) =
        listOf(
            "cuántos", "cuantos", "cuántas", "cuantas", "how many", "count",
            "total", "número", "numero", "number", "cuánta", "cuanta", "cuenta"
        ).any { it in query }
}
