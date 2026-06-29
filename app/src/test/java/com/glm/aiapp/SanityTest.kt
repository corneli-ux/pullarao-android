package com.glm.aiapp

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SanityTest {

    @Test
    fun `sanity check`() {
        assertThat(1 + 1).isEqualTo(2)
    }

    @Test
    fun `string concatenation works`() {
        val a = "GLM"
        val b = "AI"
        assertThat("$a $b App").isEqualTo("Pullarao 1 App")
    }
}
