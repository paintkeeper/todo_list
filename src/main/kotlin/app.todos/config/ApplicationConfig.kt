package app.todos.config

import app.todos.repository.ToDosRepository
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.atomic.AtomicBoolean

@Configuration
class ApplicationConfig(private val repository: ToDosRepository) {

    private val logger = KotlinLogging.logger {}

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val syncLock = AtomicBoolean(false)

    @Scheduled(fixedDelay = 5000)
    fun checkPastDue() {
        logger.debug { "Attempt to run planned cleanup." }
        takeIf { syncLock.compareAndSet(false, true) }
            .runCatching {
                logger.debug { "Planned cleanup started." }
                repository.updateOldRecords()
                syncLock.set(false)
                logger.debug { "Planned cleanup finished." }
            }.onFailure {
                logger.error { "Planned cleanup finished with errors. ${it.message}" }
                syncLock.set(false)
            }
    }

}