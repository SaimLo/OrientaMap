package com.showroomprive.orientamap.data.importer

import com.showroomprive.orientamap.domain.model.GeoBounds
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RgeAltiWfsClientTest {

    @Test
    fun buildsIgnWfsRequestForProjectBounds() {
        val url = RgeAltiWfsClient().buildRequestUrl(
            GeoBounds(
                north = 48.90,
                south = 48.80,
                east = 2.40,
                west = 2.30
            )
        )

        assertTrue(url.startsWith("https://data.geopf.fr/wfs/ows?"))
        assertTrue(url.contains("SERVICE=WFS"))
        assertTrue(url.contains("REQUEST=GetFeature"))
        assertTrue(url.contains("TYPENAMES=ELEVATION.CONTOUR.LINE%3Acourbe"))
        assertTrue(url.contains("OUTPUTFORMAT=application%2Fjson"))
        assertTrue(url.contains("BBOX=2.3%2C48.8%2C2.4%2C48.9%2CEPSG%3A4326"))
    }

    @Test
    fun turnsHttpFailureIntoAnIgnWfsException() {
        val server = ServerSocket(0)
        val body = "service unavailable"
        val serverThread = thread(start = true) {
            server.accept().use { socket ->
                socket.getInputStream().bufferedReader().readLine()
                socket.getOutputStream().bufferedWriter().use { writer ->
                    writer.write(
                        "HTTP/1.1 503 Service Unavailable\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: ${body.toByteArray().size}\r\n" +
                            "Connection: close\r\n\r\n" +
                            body
                    )
                    writer.flush()
                }
            }
        }

        try {
            val client = RgeAltiWfsClient(
                endpoint = "http://127.0.0.1:${server.localPort}/wfs"
            )
            val error = assertThrows(RgeAltiWfsException::class.java) {
                runBlocking {
                    client.fetchGeoJson(GeoBounds(48.90, 48.80, 2.40, 2.30))
                }
            }
            assertTrue(error.message.orEmpty().contains("HTTP 503"))
        } finally {
            server.close()
            serverThread.join(1_000)
        }
    }
}
