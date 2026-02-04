package com.devsusana.hometutorpro.data.mappers

import com.devsusana.hometutorpro.data.models.StudentDataModel
import com.devsusana.hometutorpro.domain.entities.Student

/**
 * Mapper for converting between StudentDataModel and Student domain entity.
 */

fun StudentDataModel.toDomain(): Student {
    return Student(
        id = id,
        professorId = professorId,
        name = name,
        age = age,
        address = address,
        parentPhones = parentPhones,
        studentPhone = studentPhone,
        studentEmail = studentEmail,
        subjects = subjects,
        course = course,
        pricePerHour = pricePerHour,
        pendingBalance = pendingBalance,
        educationalAttention = educationalAttention,
        lastPaymentDate = lastPaymentDate,
        color = color,
        isActive = isActive,
        lastClassDate = lastClassDate
    )
}

fun Student.toData(): StudentDataModel {
    return StudentDataModel(
        id = id,
        professorId = professorId,
        name = name,
        age = age,
        address = address,
        parentPhones = parentPhones,
        studentPhone = studentPhone,
        studentEmail = studentEmail,
        subjects = subjects,
        course = course,
        pricePerHour = pricePerHour,
        pendingBalance = pendingBalance,
        educationalAttention = educationalAttention,
        lastPaymentDate = lastPaymentDate,
        color = color,
        isActive = isActive,
        lastClassDate = lastClassDate
    )
}
