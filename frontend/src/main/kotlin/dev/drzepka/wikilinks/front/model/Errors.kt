package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.error.ErrorResponse
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

data class ErrorInfo(val message: String)

data class ResponseException(val status: HttpStatusCode, val response: ErrorResponse) :
    Exception("Received error response from WikiLinks server")

suspend fun decodeErrorResponse(httpResponse: HttpResponse): Nothing {
    val errorResponse = try {
        httpResponse.body()
    } catch (e: Exception) {
        console.error("Error while decoding error response. Original body: ${httpResponse.bodyAsText()}")
        ErrorResponse.unknown()
    }

    throw ResponseException(httpResponse.status, errorResponse)
}
