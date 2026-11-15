package com.studentreviewer.offline.ai

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GgufMetadata(
    val isValid: Boolean,
    val version: Int,
    val tensorCount: Long,
    val metadataKvCount: Long,
    val architecture: String? = null,
    val quantization: String? = null,
    val fileSize: Long = 0,
    val errorMessage: String? = null
)

object GgufValidator {
    private const val GGUF_MAGIC = 0x46554747

    fun validateFile(file: File): GgufMetadata {
        if (!file.exists() || !file.canRead()) {
            return GgufMetadata(
                isValid = false,
                version = 0,
                tensorCount = 0,
                metadataKvCount = 0,
                fileSize = 0,
                errorMessage = "File does not exist or cannot be read."
            )
        }

        val fileSize = file.length()
        if (fileSize < 16) {
            return GgufMetadata(
                isValid = false,
                version = 0,
                tensorCount = 0,
                metadataKvCount = 0,
                fileSize = fileSize,
                errorMessage = "File is too small to be a valid GGUF model."
            )
        }

        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(16)
                raf.readFully(headerBytes)
                val buf = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

                val magic = buf.int
                if (magic != GGUF_MAGIC) {
                    return GgufMetadata(
                        isValid = false,
                        version = 0,
                        tensorCount = 0,
                        metadataKvCount = 0,
                        fileSize = fileSize,
                        errorMessage = "Invalid magic header: expected 0x46554747 (GGUF), found 0x${Integer.toHexString(magic)}"
                    )
                }

                val version = buf.int
                if (version != 2 && version != 3) {
                    return GgufMetadata(
                        isValid = false,
                        version = version,
                        tensorCount = 0,
                        metadataKvCount = 0,
                        fileSize = fileSize,
                        errorMessage = "Unsupported GGUF version $version. Only versions 2 and 3 are supported."
                    )
                }

                val tensorCount = buf.long
                val metadataKvBytes = ByteArray(8)
                raf.readFully(metadataKvBytes)
                val metaBuf = ByteBuffer.wrap(metadataKvBytes).order(ByteOrder.LITTLE_ENDIAN)
                val metadataKvCount = metaBuf.long

                var architecture: String? = null
                var quantization: String? = null

                var bytesRead = 24L
                val maxScan = (fileSize - 100).coerceAtMost(65536L)

                for (i in 0 until metadataKvCount.coerceAtMost(100L)) {
                    if (bytesRead >= maxScan) break
                    val keyLenBytes = ByteArray(8)
                    raf.readFully(keyLenBytes)
                    bytesRead += 8
                    val keyLen = ByteBuffer.wrap(keyLenBytes).order(ByteOrder.LITTLE_ENDIAN).long
                    if (keyLen <= 0 || keyLen > 256 || bytesRead + keyLen > maxScan) break

                    val keyBytes = ByteArray(keyLen.toInt())
                    raf.readFully(keyBytes)
                    bytesRead += keyLen
                    val key = String(keyBytes, Charsets.UTF_8)

                    val typeBytes = ByteArray(4)
                    raf.readFully(typeBytes)
                    bytesRead += 4
                    val valueType = ByteBuffer.wrap(typeBytes).order(ByteOrder.LITTLE_ENDIAN).int

                    if (valueType == 8) {
                        val strLenBytes = ByteArray(8)
                        raf.readFully(strLenBytes)
                        bytesRead += 8
                        val strLen = ByteBuffer.wrap(strLenBytes).order(ByteOrder.LITTLE_ENDIAN).long
                        if (strLen in 1..256 && bytesRead + strLen <= maxScan) {
                            val strBytes = ByteArray(strLen.toInt())
                            raf.readFully(strBytes)
                            bytesRead += strLen
                            val strVal = String(strBytes, Charsets.UTF_8)
                            if (key == "general.architecture") {
                                architecture = strVal
                            } else if (key == "general.quantization_version" || key == "general.file_type" || key.contains("quant")) {
                                quantization = strVal
                            }
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }

                val fileNameLower = file.name.lowercase()
                if (architecture == null) {
                    if (fileNameLower.contains("qwen")) {
                        architecture = "qwen2"
                    }
                }
                if (quantization == null) {
                    if (fileNameLower.contains("q4_k_m") || fileNameLower.contains("q4_k")) {
                        quantization = "Q4_K_M"
                    }
                }

                return GgufMetadata(
                    isValid = true,
                    version = version,
                    tensorCount = tensorCount,
                    metadataKvCount = metadataKvCount,
                    architecture = architecture,
                    quantization = quantization,
                    fileSize = fileSize,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            return GgufMetadata(
                isValid = false,
                version = 0,
                tensorCount = 0,
                metadataKvCount = 0,
                fileSize = fileSize,
                errorMessage = "Failed to parse GGUF headers: ${e.message}"
            )
        }
    }
}
