package com.kieronquinn.app.pixellaunchermods.utils.extensions

import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

// https://medium.com/android-news/how-to-generate-xml-with-kotlin-extension-functions-and-lambdas-in-android-app-976229f1e4d8

fun XmlSerializer.document(
    docName: String = "UTF-8",
    xmlStringWriter: StringWriter = StringWriter(),
    init: XmlSerializer.() -> Unit
): String {
    startDocument(docName, true)
    xmlStringWriter.buffer.setLength(0)
    setOutput(xmlStringWriter)
    init()
    endDocument()
    return xmlStringWriter.toString()
}

fun XmlSerializer.element(name: String, init: XmlSerializer.() -> Unit) {
    startTag("", name)
    init()
    endTag("", name)
}

fun XmlSerializer.element(
    name: String,
    content: String,
    init: XmlSerializer.() -> Unit
) {
    startTag("", name)
    init()
    text(content)
    endTag("", name)
}

fun XmlSerializer.element(name: String, content: String) = element(name) {
    text(content)
}

fun XmlSerializer.attribute(name: String, value: String) = attribute("", name, value)