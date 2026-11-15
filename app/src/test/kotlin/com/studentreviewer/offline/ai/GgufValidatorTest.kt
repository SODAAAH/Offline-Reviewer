package com.studentreviewer.offline.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GgufValidatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testValidGgufFile() {
        val file = tempFolder.newFile("test_qwen.gguf")
        FileOutputStream(file).use { fos ->
            val buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0x46554747)
            buf.putInt(3)
            buf.putLong(128L)
            buf.putLong(0L)
            fos.write(buf.array(), 0, buf.position())
        }

        val metadata = GgufValidator.validateFile(file)
        assertTrue(metadata.isValid)
        assertEquals(3, metadata.version)
        assertEquals(128L, metadata.tensorCount)
    }

    @Test
    fun testInvalidMagic() {
        val file = tempFolder.newFile("corrupt.gguf")
        FileOutputStream(file).use { fos ->
            val buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0x12345678)
            buf.putInt(3)
            fos.write(buf.array())
        }

        val metadata = GgufValidator.validateFile(file)
        assertFalse(metadata.isValid)
        assertTrue(metadata.errorMessage?.contains("Invalid magic") == true)
    }

    @Test
    fun testUnsupportedVersion() {
        val file = tempFolder.newFile("old.gguf")
        FileOutputStream(file).use { fos ->
            val buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            buf.putInt(0x46554747)
            buf.putInt(1)
            fos.write(buf.array())
        }

        val metadata = GgufValidator.validateFile(file)
        assertFalse(metadata.isValid)
        assertTrue(metadata.errorMessage?.contains("Unsupported GGUF version") == true)
    }
}
