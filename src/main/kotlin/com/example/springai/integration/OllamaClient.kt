package com.example.springai.integration

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class OllamaClient {
    private val rest = RestTemplate()
    private val base = "http://localhost:11434" // Ollama local server

    // Embedding: returns FloatArray embedding for text
    fun embed(text: String): FloatArray {
        // NOTE: Ollama API shape may differ. This implementation posts to /api/embeddings
        // Adjust model parameter and request format according to your Ollama version.
        val url = "$base/api/embeddings"
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val body = mapOf("model" to "llama2", "input" to text)
        val req = HttpEntity(body, headers)
        val resp = rest.postForObject(url, req, Map::class.java)
        val data = resp?.get("data") as? List<*>
        val first = data?.getOrNull(0) as? Map<*, *>
        val emb = first?.get("embedding") as? List<*>
        return emb?.map { (it as Number).toFloat() }?.toFloatArray() ?: FloatArray(1536)
    }

    // Simple generation call (optional)
    fun generate(prompt: String): String {
        val url = "$base/api/generate"
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val body = mapOf("model" to "llama2", "prompt" to prompt)
        val req = HttpEntity(body, headers)
        val resp = rest.postForObject(url, req, Map::class.java)
        return resp?.toString() ?: ""
    }
}
