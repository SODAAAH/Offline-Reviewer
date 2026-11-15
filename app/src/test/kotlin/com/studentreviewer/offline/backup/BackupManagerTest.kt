package com.studentreviewer.offline.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManagerTest {

    @Test
    fun testBackupZipStructure() {
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        val manifest = """{"version":1,"exportTime":1700000000000,"subjectCount":2}"""
        zos.putNextEntry(ZipEntry("manifest.json"))
        zos.write(manifest.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        val subjects = """[{"id":1,"name":"Computer Science","colorHex":"#2563EB"}]"""
        zos.putNextEntry(ZipEntry("subjects.json"))
        zos.write(subjects.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val zis = ZipInputStream(ByteArrayInputStream(baos.toByteArray()))
        val entries = mutableMapOf<String, String>()
        var entry = zis.nextEntry
        while (entry != null) {
            entries[entry.name] = zis.readBytes().toString(StandardCharsets.UTF_8)
            zis.closeEntry()
            entry = zis.nextEntry
        }

        assertTrue(entries.containsKey("manifest.json"))
        assertTrue(entries.containsKey("subjects.json"))
        val manifestStr = entries["manifest.json"]!!
        assertTrue(manifestStr.contains("\"version\":1"))
        assertTrue(manifestStr.contains("\"subjectCount\":2"))
    }
}
