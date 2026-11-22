package com.citahuellas.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileStoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val key = memoryKey()
    private fun store() = ProfileStore(java.io.File(tmp.root, "profiles.bin"), Crypto { key })

    private val ana = Profile("1", "Ana Pérez", "X1234567L", "PERU", "600000000", "ana@example.com")

    @Test fun missingFileIsEmpty() {
        assertTrue(store().load().isEmpty())
    }

    @Test fun storeRoundTrip() {
        val s = store()
        s.upsert(ana)
        s.upsert(ana.copy(note = "renovación"))
        s.upsert(ana.copy(id = "2", fullName = "Luis"))
        assertEquals(listOf(ana.copy(note = "renovación"), ana.copy(id = "2", fullName = "Luis")), store().load())
        s.delete("1")
        assertEquals(listOf("2"), store().load().map { it.id })
    }

    @Test fun fileIsNotPlaintext() {
        store().upsert(ana)
        val bytes = java.io.File(tmp.root, "profiles.bin").readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(bytes.contains("X1234567L"))
    }
}
