package com.example

import com.example.data.*
import com.example.viewmodel.AttendanceViewModel
import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

class GradeAndPersistenceUnitTest {

    @Test
    fun testGradePointsConversion() {
        assertEquals(10.0, AttendanceViewModel.gradeToPoints("O")!!, 0.001)
        assertEquals(9.0, AttendanceViewModel.gradeToPoints("A+")!!, 0.001)
        assertEquals(8.0, AttendanceViewModel.gradeToPoints("A")!!, 0.001)
        assertEquals(7.0, AttendanceViewModel.gradeToPoints("B+")!!, 0.001)
        assertEquals(6.0, AttendanceViewModel.gradeToPoints("B")!!, 0.001)
        assertEquals(5.0, AttendanceViewModel.gradeToPoints("C")!!, 0.001)
        assertEquals(0.0, AttendanceViewModel.gradeToPoints("U")!!, 0.001)
        assertNull(AttendanceViewModel.gradeToPoints(null))
        assertNull(AttendanceViewModel.gradeToPoints("Invalid"))
    }

    @Test
    fun testSemesterGpaCalculation() {
        val coursesSem1 = listOf(
            SemesterCourse(id = 1, semester = 1, name = "Data Structures", credits = 4.0, grade = "O"),
            SemesterCourse(id = 2, semester = 1, name = "Algorithms", credits = 3.0, grade = "A+"),
            SemesterCourse(id = 3, semester = 1, name = "Computer Networks", credits = 3.0, grade = "A"),
            SemesterCourse(id = 4, semester = 1, name = "Web Lab", credits = 1.5, grade = "B+")
        )

        var sumPoints = 0.0
        var sumCredits = 0.0
        for (c in coursesSem1) {
            val gp = AttendanceViewModel.gradeToPoints(c.grade)
            if (gp != null) {
                sumPoints += c.credits * gp
                sumCredits += c.credits
            }
        }
        val expectedGpa = sumPoints / sumCredits
        assertEquals(11.5, sumCredits, 0.001)
        assertEquals(8.826, expectedGpa, 0.001)
    }

    @Test
    fun testCreditWeightedCgpaAcrossSelectedSemesters() {
        val sem1Courses = listOf(
            SemesterCourse(id = 1, semester = 1, name = "Sub 1", credits = 4.0, grade = "O"),
            SemesterCourse(id = 2, semester = 1, name = "Sub 2", credits = 4.0, grade = "A"),
            SemesterCourse(id = 3, semester = 1, name = "Sub 3", credits = 2.0, grade = "A")
        )

        val sem2Courses = listOf(
            SemesterCourse(id = 4, semester = 2, name = "Sub 4", credits = 4.5, grade = "O"),
            SemesterCourse(id = 5, semester = 2, name = "Sub 5", credits = 4.0, grade = "B+"),
            SemesterCourse(id = 6, semester = 2, name = "Sub 6", credits = 3.0, grade = "B")
        )

        val allCourses = sem1Courses + sem2Courses
        val selectedSemesters = setOf(1, 2)

        var totalCreds = 0.0
        var totalPoints = 0.0
        allCourses.filter { selectedSemesters.contains(it.semester) }.forEach { c ->
            val gp = AttendanceViewModel.gradeToPoints(c.grade)
            if (gp != null) {
                totalPoints += c.credits * gp
                totalCreds += c.credits
            }
        }
        val cgpa = totalPoints / totalCreds
        assertEquals(21.5, totalCreds, 0.001)
        assertEquals(179.0, totalPoints, 0.001)
        assertEquals(8.3255, cgpa, 0.001)
    }

    @Test
    fun testCaMarksAndEndSemGradesPersistence() {
        val exportData = ExportData(
            subjects = listOf(
                Subject(id = 1, name = "Cloud Computing", baseAttended = 10, baseMissed = 2, isLabIntegrated = true),
                Subject(id = 2, name = "Cryptography", baseAttended = 12, baseMissed = 1, isLabIntegrated = false)
            ),
            timetableSlots = listOf(
                TimetableSlot(id = 1, dayOfWeek = 1, subjectId = 1, startTime = "09:00 AM", periodName = "Period 1", endTime = "09:50 AM", classType = "Lecture"),
                TimetableSlot(id = 2, dayOfWeek = 1, subjectId = 1, startTime = "10:00 AM", periodName = "Period 2", endTime = "10:50 AM", classType = "Lab")
            ),
            attendanceRecords = listOf(
                AttendanceRecord(id = 1, subjectId = 1, dateString = "2026-10-11", status = "ATTENDED", slotId = 1, isExtra = false)
            ),
            caMarks = listOf(
                CaMark(id = 1, subjectName = "Cloud Computing", ca1Marks = 46.0, ca1Total = 50.0, ca2Marks = 48.0, ca2Total = 50.0)
            ),
            semesterCourses = listOf(
                SemesterCourse(id = 1, semester = 1, name = "Cloud Computing", credits = 4.0, grade = "O")
            )
        )

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(ExportData::class.java)
        val json = adapter.toJson(exportData)
        assertNotNull(json)
        assertTrue(json.contains("Cloud Computing"))
        assertTrue(json.contains("isLabIntegrated\":true"))
        assertTrue(json.contains("classType\":\"Lab\""))

        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals(2, parsed!!.subjects.size)
        assertEquals(1, parsed.caMarks.size)
        assertEquals("Cloud Computing", parsed.caMarks[0].subjectName)
        assertEquals(1, parsed.semesterCourses.size)
        assertEquals("O", parsed.semesterCourses[0].grade)
    }
}
