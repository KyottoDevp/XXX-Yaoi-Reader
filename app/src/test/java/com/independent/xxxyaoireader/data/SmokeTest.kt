package com.independent.xxxyaoireader.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun sourceBaseUrlIsHttps() {
        assertEquals("https://3xyaoi.com", XXXYaoiClient.BASE_URL)
    }
}
