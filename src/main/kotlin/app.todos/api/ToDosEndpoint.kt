package app.todos.api

import app.todos.repository.ToDosRepository
import org.springframework.stereotype.Service
import app.todos.model.ToDo
import app.todos.model.ToDoStatus
import app.todos.model.ToDoUpdate
import java.util.UUID
import kotlin.streams.toList

@Service
class ToDosEndpoint(private val repository: ToDosRepository) : ToDosApiService {

    override fun create(toDo: ToDo): ToDo = repository.create(
        endDate = toDo.dueDate,
        desc = toDo.description,
        st = ToDoStatus.NOT_DONE.value
    ).let {
        ToDo(
            uuid = it.uuid,
            status = it.status?.runCatching(ToDoStatus::valueOf)?.getOrNull(),
            description = it.description,
            dueDate = it.dueDate,
            doneDate = it.doneDate,
            creationDate = it.creationDate
        )
    }

    override fun details(id: UUID): ToDo = repository.getById(id)
        ?.let {
            ToDo(
                uuid = it.uuid,
                status = ToDoStatus.valueOf(it.status),
                description = it.description,
                dueDate = it.dueDate,
                doneDate = it.doneDate,
                creationDate = it.creationDate
            )
        } ?: throw NotFoundException("ToDo Record not found.")

    override fun getAllPlanned(): List<ToDo> = repository
        .fetchAllNotDone()
        .map {
            ToDo(
                uuid = it.uuid,
                status = ToDoStatus.valueOf(it.status),
                description = it.description,
                dueDate = it.dueDate,
                doneDate = it.doneDate,
                creationDate = it.creationDate
            )
        }.toList()

    override fun update(id: UUID, toDoUpdate: ToDoUpdate): ToDo = repository.updateStatus(id, toDoUpdate.status.value)
        ?.let {
            ToDo(
                uuid = it.uuid,
                status = ToDoStatus.valueOf(it.status),
                description = it.description,
                dueDate = it.dueDate,
                doneDate = it.doneDate,
                creationDate = it.creationDate
            )
        } ?: throw NotFoundException("ToDo Record not found.")

}