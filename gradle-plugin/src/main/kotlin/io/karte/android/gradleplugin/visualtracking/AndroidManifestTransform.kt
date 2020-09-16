package io.karte.android.gradleplugin.visualtracking

import io.karte.android.gradleplugin.logger
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileWriter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class AndroidManifestTransform(private val androidManifestPath: String) {
    private val document: Document
    private val androidNsPrefix: String

    private companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private val COMMON_SCHEMES = arrayOf("http", "https", "file", "data", "content")
        private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder()
    }

    init {
        document = documentBuilder.parse(androidManifestPath)
        androidNsPrefix = document.lookupPrefix(ANDROID_NS)
    }

    fun execute() {
        // There must be only one <application> tag.
        val applicationElement =
            document.documentElement.getElementsByTagName("application").item(0) as Element

        appendVisualTrackingEnableOption(applicationElement)
        appendPairingActivityElement(applicationElement)

        writesToFile()
    }

    private fun appendVisualTrackingEnableOption(applicationElement: Element) {
        val metaElement = document.createElement("meta-data").apply {
            setAttribute("$androidNsPrefix:name", "io.karte.android._VisualTracking")
            setAttribute("$androidNsPrefix:value", "true")
        }
        applicationElement.appendChild(metaElement)
    }

    private fun appendPairingActivityElement(applicationElement: Element) {
        val schemeDefinedInActivity = applicationElement
            .getElementsByTagNameAsList("activity")
            .flatMap { it.getElementsByTagNameAsList("intent-filter") }
            .flatMap { it.getElementsByTagNameAsList("data") }
            .mapNotNull { it.getAttributeNodeNS(ANDROID_NS, "scheme") }
            .map { it.value }
            .filter { it !in COMMON_SCHEMES }
            .distinct()

        val schemeDefinedAsMetaData = applicationElement.getElementsByTagNameAsList("meta-data")
            .find {
                it.getAttributeNS(
                    ANDROID_NS,
                    "name"
                ) == "io.karte.android.Tracker.PairingUrlScheme"
            }
            ?.getAttributeNodeNS(ANDROID_NS, "value")?.value

        val allSchemes = schemeDefinedInActivity + listOfNotNull(schemeDefinedAsMetaData)

        if (allSchemes.isEmpty()) {
            logger.info(
                "Not append PairingActivity to AndroidManifest $androidManifestPath" +
                    " because no scheme is defined."
            )
            return
        }
        logger.info(
            "Append PairingActivity to AndroidManifest $androidManifestPath for schemes $allSchemes"
        )

        applicationElement.appendChild(buildPairingActivityElement(allSchemes))
    }

    private fun buildPairingActivityElement(schemes: List<String>): Element {

        val activityElement = document.createElement("activity").apply {
            setAttribute(
                "$androidNsPrefix:name",
                PAIRING_ACTIVITY
            )
            setAttribute("$androidNsPrefix:label", "Start Karte Pairing")
        }

        schemes.map { scheme ->
            val actionElement = document.createElement("action").apply {
                setAttribute("$androidNsPrefix:name", "android.intent.action.VIEW")
            }

            val categoryElement = document.createElement("category").apply {
                setAttribute("$androidNsPrefix:name", "android.intent.category.DEFAULT")
            }

            val dataElement = document.createElement("data").apply {
                setAttribute("$androidNsPrefix:scheme", scheme)
                setAttribute("$androidNsPrefix:host", "_krtp")
            }

            return@map document.createElement("intent-filter").apply {
                appendChild(actionElement)
                appendChild(categoryElement)
                appendChild(dataElement)
            }
        }.forEach { activityElement.appendChild(it) }
        return activityElement
    }

    private fun writesToFile() {
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            // Default settings. Generated Manifest can be see in build/intermediates/merged_manifests/<variant>/AndroidManifest.xml
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        }
        transformer.transform(
            DOMSource(document),
            StreamResult(FileWriter(File(androidManifestPath)))
        )
    }

    private fun Element.getElementsByTagNameAsList(tagName: String): List<Element> {
        val nodeList = getElementsByTagName(tagName)
        val ret = mutableListOf<Element>()
        for (i in 0 until nodeList.length) {
            ret.add(nodeList.item(i) as Element)
        }
        return ret
    }
}
