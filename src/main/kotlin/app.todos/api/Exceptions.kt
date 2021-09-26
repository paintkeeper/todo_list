package app.todos.api

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletResponse


sealed class ApiException(msg: String, val code: Int) : RuntimeException(msg)

class NotFoundException(msg: String, code: Int = HttpStatus.NOT_FOUND.value()) : ApiException(msg, code)
class ServerException(msg: String, code: Int = HttpStatus.INTERNAL_SERVER_ERROR.value()) : ApiException(msg, code)

@ControllerAdvice
class DefaultExceptionHandler {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(value = [ApiException::class])
    fun onApiException(ex: ApiException, response: HttpServletResponse) {
        logger.error { "${ex.message}" }
        logger.debug { "Sending error Code: ${ex.code} Message:[${ex.message}]" }
        response.sendError(ex.code, ex.message)
    }

    @ExceptionHandler(value = [NotImplementedError::class])
    fun onNotImplemented(ex: NotImplementedError, response: HttpServletResponse) {
        logger.error { "${ex.message}" }
        response.sendError(HttpStatus.NOT_IMPLEMENTED.value())
    }

    @ExceptionHandler(value = [Exception::class])
    fun onDefault(ex: Exception, response: HttpServletResponse) {
        logger.error { "${ex.message}" }
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.message)
    }

}
