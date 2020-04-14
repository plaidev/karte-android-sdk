package io.karte.android.unit

import io.karte.android.utilities.gunzip
import io.karte.android.utilities.gzip
import org.junit.Assert
import org.junit.Test

class GzipUtilTest {

    @Test
    fun testGzipped() {
        Assert.assertNotNull(gzip("abc"))
        Assert.assertNotNull(gzip("あいう"))
        Assert.assertNotNull(gzip(""))
        Assert.assertNull(gzip(null))
    }

    @Test
    fun testGunzipped() {
        Assert.assertEquals(null, gunzip("abc".toByteArray()))
        Assert.assertEquals("abc", gunzip(gzip("abc")))
        Assert.assertEquals("あいう", gunzip(gzip("あいう")))
        Assert.assertEquals("", gunzip(gzip("")))
        Assert.assertEquals(null, gunzip(gzip(null)))
    }
}