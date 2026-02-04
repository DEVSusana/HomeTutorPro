package com.devsusana.hometutorpro.core.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devsusana.hometutorpro.domain.entities.Student

/**
 * Helper object for sharing files via email and WhatsApp.
 * Provides utilities to create share intents for different platforms.
 */
object ShareHelper {
    
    /**
     * Shares a file via email.
     * Opens email client with the file attached and pre-filled recipient and subject.
     * 
     * @param context Android context
     * @param student Student to send the file to
     * @param fileUri URI of the file to share
     * @param fileName Name of the file
     * @param notes Optional notes to include in the email body
     */
    fun shareViaEmail(
        context: Context,
        student: Student,
        fileUri: Uri,
        fileName: String,
        notes: String = ""
    ) {
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
            
            // Set recipient if email is available
            if (!student.studentEmail.isNullOrBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(student.studentEmail))
            }
            
            putExtra(Intent.EXTRA_SUBJECT, "Material de estudio - ${student.name}")
            
            // Build email body
            val emailBody = buildString {
                appendLine("Hola,")
                appendLine()
                appendLine("Te envío el siguiente material de estudio: $fileName")
                if (notes.isNotBlank()) {
                    appendLine()
                    appendLine("Notas:")
                    appendLine(notes)
                }
                appendLine()
                appendLine("Saludos")
            }
            putExtra(Intent.EXTRA_TEXT, emailBody)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(Intent.createChooser(emailIntent, "Enviar por Email"))
        } catch (e: ActivityNotFoundException) {
            // No email client available, use generic share
            shareViaGeneric(context, fileUri, fileName, notes)
        }
    }
    
    /**
     * Shares a file via WhatsApp.
     * Opens WhatsApp with the file attached and pre-filled message.
     * Falls back to generic share if WhatsApp is not installed.
     * 
     * @param context Android context
     * @param student Student to send the file to
     * @param fileUri URI of the file to share
     * @param fileName Name of the file
     * @param notes Optional notes to include in the message
     */
    fun shareViaWhatsApp(
        context: Context,
        student: Student,
        fileUri: Uri,
        fileName: String,
        notes: String = ""
    ) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
            setPackage("com.whatsapp")
            
            // Set recipient if phone is available
            if (student.studentPhone.isNotBlank()) {
                // Format phone number: remove non-digits, ensure country code if needed (assuming local for now or user provides it)
                // WhatsApp requires international format without '+' prefix usually for direct links, but for ACTION_SEND with jid it's different.
                // Actually, ACTION_SEND with setPackage("com.whatsapp") and EXTRA_TEXT broadcasts to contact list.
                // To send to specific person, we usually need "jid" extra or use ACTION_VIEW with api.whatsapp.com
                // However, sending a FILE to a specific person via Intent is tricky. 
                // Standard ACTION_SEND with "jid" (phone@s.whatsapp.net) works on some versions but isn't officially documented as public API.
                // Let's try the "jid" extra which is commonly used.
                
                // Remove all non-digit characters
                val cleanPhone = student.studentPhone.filter { it.isDigit() }
                // Assuming the user enters the full number including country code, or we might need to prepend it.
                // For MVP, let's assume the number is usable as is or try to sanitize it.
                // If it doesn't have country code, this might fail to match. 
                // Let's just strip non-digits.
                
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
            
            // Build message
            val message = buildString {
                appendLine("Material para ${student.name}")
                appendLine("Archivo: $fileName")
                if (notes.isNotBlank()) {
                    appendLine()
                    appendLine("Notas:")
                    appendLine(notes)
                }
            }
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(whatsappIntent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp not installed, fallback to generic share
            shareViaGeneric(context, fileUri, fileName, notes)
        }
    }
    
    /**
     * Shares a file using the generic Android share sheet.
     * Used as fallback when specific apps are not available.
     */
    private fun shareViaGeneric(
        context: Context,
        fileUri: Uri,
        fileName: String,
        notes: String
    ) {
        val genericIntent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
            
            val message = buildString {
                appendLine("Archivo: $fileName")
                if (notes.isNotBlank()) {
                    appendLine()
                    appendLine(notes)
                }
            }
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(genericIntent, "Compartir archivo"))
    }
}
