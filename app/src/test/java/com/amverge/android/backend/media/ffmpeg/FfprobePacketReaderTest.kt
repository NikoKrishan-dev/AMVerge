package com.amverge.android.backend.media.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FfprobePacketReaderTest {

    @Test
    fun `parsePacketsCsv correctly parses keyframe and non-keyframe packets`() {
        val sampleCsv = """
            0.000000,0.000000,K__
            0.041667,0.041667,___
            0.083333,0.083333,___
            2.500000,2.500000,K_
        """.trimIndent()

        val packets = FfprobePacketReader.parsePacketsCsv(sampleCsv)

        assertEquals(4, packets.size)

        // Packet 0: Keyframe at 0s
        assertEquals(0L, packets[0].ptsUs)
        assertEquals(0L, packets[0].dtsUs)
        assertTrue(packets[0].isKeyframe)
        assertEquals(0L, packets[0].demuxIndex)

        // Packet 1: Non-keyframe
        assertEquals(41667L, packets[1].ptsUs)
        assertFalse(packets[1].isKeyframe)
        assertEquals(1L, packets[1].demuxIndex)

        // Packet 3: Keyframe at 2.5s
        assertEquals(2_500_000L, packets[3].ptsUs)
        assertTrue(packets[3].isKeyframe)
        assertEquals(3L, packets[3].demuxIndex)
    }

    @Test
    fun `parsePacketsCsv ignores blank lines and comments`() {
        val sampleWithBlanks = """
            # This is a comment
            0.000000,0.000000,K__

            1.000000,1.000000,___
        """.trimIndent()

        val packets = FfprobePacketReader.parsePacketsCsv(sampleWithBlanks)
        assertEquals(2, packets.size)
        assertTrue(packets[0].isKeyframe)
        assertFalse(packets[1].isKeyframe)
    }
}
