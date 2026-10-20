package com.devsusana.hometutorpro.domain.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoorCodeExtractorTest {

    @Test
    fun `extractDoorCode returns formatted code with colon and digits`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Mayor 12, 3ºB, Portero: 1402")
        assertEquals("Portero: 1402", result)
    }

    @Test
    fun `extractDoorCode returns formatted code with space and digits`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Mayor 12, 3ºB, portero 14B")
        assertEquals("Portero: 14B", result)
    }

    @Test
    fun `extractDoorCode returns formatted code with hash prefix`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Mayor 12, 3ºB, portero: #1402")
        assertEquals("Portero: #1402", result)
    }

    @Test
    fun `extractDoorCode extracts from dial action phrase`() {
        val result = DoorCodeExtractor.extractDoorCode("Av. Reina Mercedes 4. Marcar 1402 en el portero")
        assertEquals("Portero: 1402", result)
    }

    @Test
    fun `extractDoorCode extracts from simple marcar phrase`() {
        val result = DoorCodeExtractor.extractDoorCode("Av. Reina Mercedes 4. Marcar el 1402")
        assertEquals("Portero: 1402", result)
    }

    @Test
    fun `extractDoorCode extracts pulsar action with telefonillo target`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Sol 1, pulsar 23B en el telefonillo")
        assertEquals("Telefonillo: 23B", result)
    }

    @Test
    fun `extractDoorCode handles telefonillo with floor`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Real 5, Telefonillo: 4º A")
        assertEquals("Telefonillo: 4º A", result)
    }

    @Test
    fun `extractDoorCode handles direct telefonillo number`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Real 5, Telefonillo 12")
        assertEquals("Telefonillo: 12", result)
    }

    @Test
    fun `extractDoorCode handles intercom with hash`() {
        val result = DoorCodeExtractor.extractDoorCode("Intercom: #1234")
        assertEquals("Intercom: #1234", result)
    }

    @Test
    fun `extractDoorCode handles interfono`() {
        val result = DoorCodeExtractor.extractDoorCode("Edificio A, Interfono 3B")
        assertEquals("Intercom: 3B", result)
    }

    @Test
    fun `extractDoorCode handles codigo portal`() {
        val result = DoorCodeExtractor.extractDoorCode("Código portal: 5678")
        assertEquals("Código: 5678", result)
    }

    @Test
    fun `extractDoorCode handles codigo de acceso`() {
        val result = DoorCodeExtractor.extractDoorCode("Código de acceso: 1234")
        assertEquals("Código: 1234", result)
    }

    @Test
    fun `extractDoorCode handles timbre with family name`() {
        val result = DoorCodeExtractor.extractDoorCode("Timbre: Familia Gómez")
        assertEquals("Timbre: Familia Gómez", result)
    }

    @Test
    fun `extractDoorCode handles direct timbre surname`() {
        val result = DoorCodeExtractor.extractDoorCode("Plaza España 2, Timbre Pérez")
        assertEquals("Timbre: Pérez", result)
    }

    @Test
    fun `extractDoorCode handles door code english`() {
        val result = DoorCodeExtractor.extractDoorCode("Door code: 9922")
        assertEquals("Door code: 9922", result)
    }

    @Test
    fun `extractDoorCode handles buzzer english`() {
        val result = DoorCodeExtractor.extractDoorCode("Buzzer: 4B")
        assertEquals("Buzzer: 4B", result)
    }

    @Test
    fun `extractDoorCode returns null for address without door code`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Alcalá 45, 2º Izquierda")
        assertNull(result)
    }

    @Test
    fun `extractDoorCode returns null for street named Codigo Civil`() {
        val result = DoorCodeExtractor.extractDoorCode("Calle Código Civil 12")
        assertNull(result)
    }

    @Test
    fun `extractDoorCode returns null for null or empty strings`() {
        assertNull(DoorCodeExtractor.extractDoorCode(null, null))
        assertNull(DoorCodeExtractor.extractDoorCode("", ""))
        assertNull(DoorCodeExtractor.extractDoorCode("   ", "   "))
    }

    @Test
    fun `extractDoorCode falls back to notes when address has no code`() {
        val result = DoorCodeExtractor.extractDoorCode(
            address = "Calle Alcalá 45",
            notes = "El telefonillo es el 1402, tocar dos veces"
        )
        assertEquals("Telefonillo: 1402", result)
    }

    @Test
    fun `extractDoorCode prioritizes address when both have code`() {
        val result = DoorCodeExtractor.extractDoorCode(
            address = "Calle Alcalá 45, Portero: 12A",
            notes = "Código: 9999"
        )
        assertEquals("Portero: 12A", result)
    }

    @Test
    fun `extractDoorCode handles conversational conversational update sentences`() {
        assertEquals("Portero: 4B", DoorCodeExtractor.extractDoorCode("edítale el portero a Hugo a 4B"))
        assertEquals("Portero: 387", DoorCodeExtractor.extractDoorCode("edita el portero de Hugo a 387"))
        assertEquals("Portero: 14B", DoorCodeExtractor.extractDoorCode("ponle a Hugo el portero 14B"))
        assertEquals("Portero: 14B", DoorCodeExtractor.extractDoorCode("ponle a Hugo de portero el 14B"))
        assertEquals("Portero: 387", DoorCodeExtractor.extractDoorCode("añade a Hugo el portero 387"))
        assertEquals("Portero: 4B", DoorCodeExtractor.extractDoorCode("cámbiale el portero a Hugo Morales por 4B"))
    }
}
