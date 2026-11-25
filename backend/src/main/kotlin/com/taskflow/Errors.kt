package com.taskflow

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErrorResponse(val timestamp: Instant, val status: Int, val code: String, val message: String, val path: String, val fieldErrors: Map<String, String>? = null)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun api(ex: ApiException, req: HttpServletRequest) = ResponseEntity.status(ex.status).body(ErrorResponse(Instant.now(), ex.status.value(), ex.code, ex.message, req.requestURI, ex.fieldErrors))
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.badRequest().body(ErrorResponse(Instant.now(), 400, "VALIDATION_ERROR", "Validation failed", req.requestURI, fields))
    }
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun invalidBody(ex: HttpMessageNotReadableException, req: HttpServletRequest) = response(HttpStatus.BAD_REQUEST, "INVALID_BODY", "Invalid request body", req)
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException::class, org.springframework.web.bind.MissingServletRequestParameterException::class)
    fun invalidParameter(ex: Exception, req: HttpServletRequest) = response(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Invalid request parameter", req)
    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception, req: HttpServletRequest): ResponseEntity<ErrorResponse> {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler::class.java).error("Unhandled request error", ex)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req)
    }
    private fun response(status: HttpStatus, code: String, message: String, req: HttpServletRequest) = ResponseEntity.status(status).body(ErrorResponse(Instant.now(), status.value(), code, message, req.requestURI))
}
