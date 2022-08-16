package dev.drzepka.wikilinks.common.model.error

@kotlinx.serialization.Serializable
data class ErrorResponse(val code: ErrorCode, val message: String) {

    companion object {
        fun unknown(message: String? = null): ErrorResponse =
            ErrorResponse(ErrorCode.UNKNOWN, message ?: "Unknown error.")

        fun sourcePageNotFound(source: String): ErrorResponse =
            ErrorResponse(ErrorCode.SOURCE_PAGE_NOT_FOUND, "Source page ($source) wasn't found in the index.")

        fun targetPageNotFound(target: String): ErrorResponse =
            ErrorResponse(ErrorCode.TARGET_PAGE_NOT_FOUND, "Target page ($target) wasn't found in the index.")

        fun sourceAndTargetPageNotFound(source: String, target: String): ErrorResponse = ErrorResponse(
            ErrorCode.PAGES_NOT_FOUND,
            "Neither source ($source) nor target ($target) page weren't found."
        )
    }
}
