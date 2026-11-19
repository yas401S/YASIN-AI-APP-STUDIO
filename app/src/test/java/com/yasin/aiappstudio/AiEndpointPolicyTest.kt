package com.yasin.aiappstudio

import org.junit.Assert.*
import org.junit.Test

class AiEndpointPolicyTest {
    @Test fun acceptsHttpsProviderEndpoints() {
        assertEquals("https://api.example.com/v1/chat/completions",
            AiEndpointPolicy.validate("https://api.example.com/v1/chat/completions"))
    }

    @Test fun trimsWhitespace() {
        assertEquals("https://api.example.com/v1",
            AiEndpointPolicy.validate("  https://api.example.com/v1  "))
    }

    @Test fun rejectsPlainHttpAndMissingHost() {
        assertThrows(IllegalArgumentException::class.java) {
            AiEndpointPolicy.validate("http://api.example.com/v1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AiEndpointPolicy.validate("https:///v1")
        }
    }

    @Test fun rejectsEmbeddedCredentialsAndFragments() {
        assertThrows(IllegalArgumentException::class.java) {
            AiEndpointPolicy.validate("https://user:secret@example.com/v1")
        }
        assertThrows(IllegalArgumentException::class.java) {
            AiEndpointPolicy.validate("https://api.example.com/v1#token")
        }
    }
}
