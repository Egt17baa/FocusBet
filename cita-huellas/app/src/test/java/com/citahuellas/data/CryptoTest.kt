package com.citahuellas.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

fun memoryKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

class CryptoTest {
    private val key = memoryKey()
    private val crypto = Crypto { key }

    @Test fun roundTrip() {
        val plain = "X1234567L Ñoño".toByteArray()
        assertArrayEquals(plain, crypto.decrypt(crypto.encrypt(plain)))
    }

    @Test fun ivIsRandom() {
        val plain = "igual".toByteArray()
        assertFalse(crypto.encrypt(plain).contentEquals(crypto.encrypt(plain)))
    }

    @Test(expected = AEADBadTagException::class)
    fun tamperedFails() {
        val blob = crypto.encrypt("dato".toByteArray())
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
        crypto.decrypt(blob)
    }
}
