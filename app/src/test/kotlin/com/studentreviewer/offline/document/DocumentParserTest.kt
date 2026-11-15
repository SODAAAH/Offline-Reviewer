package com.studentreviewer.offline.document

import com.studentreviewer.offline.document.parser.DocxDocumentParser
import com.studentreviewer.offline.document.parser.MarkdownDocumentParser
import com.studentreviewer.offline.document.parser.PdfDocumentParser
import com.studentreviewer.offline.document.parser.PptxDocumentParser
import com.studentreviewer.offline.document.parser.TxtDocumentParser
import com.studentreviewer.offline.document.parser.XlsxDocumentParser
import com.studentreviewer.offline.document.util.DocumentTextCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentParserTest {

    @Test
    fun testDocumentTextCleaner() {
        val rawText = "This is an oper-\n  ation on soft- \n ware engineering.   Multiple    spaces."
        val cleaned = DocumentTextCleaner.clean(rawText)
        assertTrue(cleaned.contains("operation"))
        assertTrue(cleaned.contains("software"))
        assertTrue(!cleaned.contains("   "))
    }

    @Test
    fun testTxtDocumentParser() {
        val parser = TxtDocumentParser()
        val text = "Paragraph 1: Introduction to Networks.\n\nParagraph 2: The OSI model has seven layers."
        val stream = ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
        val chunks = parser.parse(stream, 1L, "networks.txt")

        assertEquals(1, chunks.size)
        assertTrue(chunks[0].content.contains("Introduction to Networks"))
        assertTrue(chunks[0].content.contains("seven layers"))
    }

    @Test
    fun testMarkdownDocumentParser() {
        val parser = MarkdownDocumentParser()
        val md = """
            # Central Processing Unit
            The CPU executes instructions.
            
            ## Registers
            - Program Counter
            - Instruction Register
            - Accumulator
        """.trimIndent()
        val stream = ByteArrayInputStream(md.toByteArray(StandardCharsets.UTF_8))
        val chunks = parser.parse(stream, 2L, "cpu.md")

        assertTrue(chunks.isNotEmpty())
        val first = chunks[0]
        assertEquals("Central Processing Unit", first.sectionHeader)
        assertTrue(first.content.contains("The CPU executes instructions"))
    }

    @Test
    fun testDocxDocumentParser() {
        val parser = DocxDocumentParser()
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        zos.putNextEntry(ZipEntry("word/document.xml"))
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p>
                  <w:pPr>
                    <w:pStyle w:val="Heading1"/>
                  </w:pPr>
                  <w:r><w:t>Operating Systems</w:t></w:r>
                </w:p>
                <w:p>
                  <w:r><w:t>An OS manages computer hardware resources.</w:t></w:r>
                </w:p>
              </w:body>
            </w:document>
        """.trimIndent()
        zos.write(xmlContent.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val chunks = parser.parse(ByteArrayInputStream(baos.toByteArray()), 3L, "os.docx")
        assertTrue(chunks.isNotEmpty())
        assertEquals("Operating Systems", chunks[0].sectionHeader)
        assertTrue(chunks[0].content.contains("An OS manages computer hardware"))
    }

    @Test
    fun testPptxDocumentParser() {
        val parser = PptxDocumentParser()
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)
        zos.putNextEntry(ZipEntry("ppt/slides/slide1.xml"))
        val xmlContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
              <p:cSld>
                <p:spTree>
                  <p:sp>
                    <p:txBody>
                      <a:p><a:r><a:t>Data Structures</a:t></a:r></a:p>
                      <a:p><a:r><a:t>Binary Search Trees provide O(log n) lookup.</a:t></a:r></a:p>
                    </p:txBody>
                  </p:sp>
                </p:spTree>
              </p:cSld>
            </p:sld>
        """.trimIndent()
        zos.write(xmlContent.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val chunks = parser.parse(ByteArrayInputStream(baos.toByteArray()), 4L, "dsa.pptx")
        assertEquals(1, chunks.size)
        assertEquals(1, chunks[0].slideNumber)
        assertTrue(chunks[0].content.contains("Data Structures"))
        assertTrue(chunks[0].content.contains("Binary Search Trees"))
    }

    @Test
    fun testXlsxDocumentParser() {
        val parser = XlsxDocumentParser()
        val baos = ByteArrayOutputStream()
        val zos = ZipOutputStream(baos)

        zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
        val sharedXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="2" uniqueCount="2">
              <si><t>Stack</t></si>
              <si><t>LIFO Data Structure</t></si>
            </sst>
        """.trimIndent()
        zos.write(sharedXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()

        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()
        zos.write(sheetXml.toByteArray(StandardCharsets.UTF_8))
        zos.closeEntry()
        zos.close()

        val chunks = parser.parse(ByteArrayInputStream(baos.toByteArray()), 5L, "flashcards.xlsx")
        assertEquals(1, chunks.size)
        assertTrue(chunks[0].content.contains("Stack: LIFO Data Structure"))
    }

    @Test
    fun testPdfDocumentParser() {
        val parser = PdfDocumentParser()
        val pdfContent = """
            %PDF-1.4
            1 0 obj
            << /Length 75 >>
            stream
            BT
            /F1 12 Tf
            (Computer Science is the study of computation.) Tj
            ET
            endstream
            endobj
            %%EOF
        """.trimIndent()
        val stream = ByteArrayInputStream(pdfContent.toByteArray(StandardCharsets.ISO_8859_1))
        val chunks = parser.parse(stream, 6L, "cs.pdf")

        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks[0].content.contains("Computer Science is the study of computation"))
    }
}
