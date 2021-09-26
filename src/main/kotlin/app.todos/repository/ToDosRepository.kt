package app.todos.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import app.todos.repository.tables.Todos.TODOS
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

@Repository
class ToDosRepository(private val jooq: DSLContext) {

    @Transactional
    fun create(endDate: OffsetDateTime, desc: String, st: String) =
        jooq.newRecord(TODOS).apply {
            uuid = UUID.randomUUID()
            creationDate = now()
            dueDate = endDate
            description = desc
            status = st
        }.apply {
            store()
        }.apply {
            jooq.fetchOne(TODOS, TODOS.UUID.eq(uuid))
        }

    fun getById(uuid: UUID) = jooq.fetchOne(TODOS, TODOS.UUID.eq(uuid))

    fun fetchAllNotDone() = jooq.selectFrom(TODOS)
        .where(TODOS.STATUS.ne("DONE"))
        .fetchStream()

    @Transactional
    fun updateStatus(uuid: UUID, st: String) = jooq.fetchOne(TODOS, TODOS.UUID.eq(uuid))
        ?.apply {
            status = st
            when (st) {
                "DONE" -> doneDate = now()
                "NOT_DONE" -> doneDate = null
            }
        }?.apply {
            store()
        }?.apply {
            jooq.fetchOne(TODOS, TODOS.UUID.eq(uuid))
        }

    @Transactional
    fun deleteAll() = jooq.deleteFrom(TODOS).execute()

    @Transactional
    fun updateOldRecords() {
        val now = now()
        jooq.selectFrom(TODOS)
            .where(TODOS.STATUS.ne("DONE"))
            .fetchStream()
            .filter { it.dueDate.isBefore(now) }
            .forEach {
                it.status = "PAST_DUE"
                it.store()
            }
    }

    private fun now() = OffsetDateTime.now(ZoneId.of("UTC"))
}