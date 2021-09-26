package app.todos.api

import app.todos.Application
import app.todos.model.ToDoStatus
import app.todos.model.ToDoUpdate
import app.todos.repository.ToDosRepository
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.test.autoconfigure.jooq.AutoConfigureJooq
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit


@WebMvcTest(ToDosApiController::class)
@ExtendWith(SpringExtension::class)
@AutoConfigureJooq
@ComponentScan(basePackageClasses = [Application::class])
@EnableAutoConfiguration(exclude = [R2dbcAutoConfiguration::class])
@ActiveProfiles("test")
@EnableScheduling
internal class ToDosApiControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var repository: ToDosRepository

    companion object {
        private const val NOT_DONE_ENDPOINT = "/todos/planned"
        private const val POST_ENDPOINT = "/todos"
    }


    @BeforeEach
    fun init() {
        repository.deleteAll()
    }

    @Test
    fun create() {
        val dueDate = utcDateTime().plusDays(10)
            .let { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(it) }
        val description = "ToDo Description"
        val json = aTodoJson(dueDate, description)

        mvc.post("/todos") {
            content = json
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
            jsonPath("$.due_date") { value(dueDate) }
            jsonPath("$.description") { value(description) }
            jsonPath("$.status") { value(ToDoStatus.NOT_DONE.value) }
            jsonPath("$.creation_date") { isString() }
        }
    }

    @Test
    fun details() {
        val dueDate = utcDateTime().plusDays(10)
            .let { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(it) }
        val json = aTodoJson(dueDate)

        val created = mvc.post(POST_ENDPOINT) {
            content = json
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it)
        }
        val uuid = created.read<String>("$.uuid")

        mvc.get("/todo/$uuid").andDo { print() }
            .andExpect {
                status { is2xxSuccessful() }
                jsonPath("$.uuid") { value(uuid) }
                jsonPath("$.due_date") { value(dueDate) }
                jsonPath("$.description") { value(created.read<String>("$.description")) }
                jsonPath("$.status") { value(created.read<String>("$.status")) }
                jsonPath("$.creation_date") { value(created.read<String>("$.creation_date")) }
            }
    }

    @Test
    fun update() {
        val dueDate = utcDateTime().plusDays(10)
            .let { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(it) }
        val json = aTodoJson(dueDate)

        val created = mvc.post(POST_ENDPOINT) {
            content = json
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it)
        }

        val uuid = created.read<String>("$.uuid")

        mvc.put("/todo/$uuid") {
            content = """{
                "status": "${ToDoUpdate.Status.DONE}"   
            }""".trimIndent()
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { is2xxSuccessful() }
            jsonPath("$.due_date") { value(dueDate) }
            jsonPath("$.description") { value(created.read<String>("$.description")) }
            jsonPath("$.status") { value("${ToDoUpdate.Status.DONE}") }
            jsonPath("$.creation_date") { value(created.read<String>("$.creation_date")) }
            jsonPath("$.done_date") { isString() }
        }

        val notDoneUpdate = mvc.put("/todo/$uuid") {
            content = """{
                "status": "${ToDoUpdate.Status.NOT_DONE}"   
            }""".trimIndent()
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { is2xxSuccessful() }
            jsonPath("$.due_date") { value(dueDate) }
            jsonPath("$.description") { value(created.read<String>("$.description")) }
            jsonPath("$.status") { value("${ToDoUpdate.Status.NOT_DONE}") }
            jsonPath("$.creation_date") { value(created.read<String>("$.creation_date")) }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it)
        }
        assertThrows<PathNotFoundException> {
            notDoneUpdate.read<String>("$.done_date")
        }

    }

    @Test
    fun getAllNotDone() {
        val createdOne = mvc.post(POST_ENDPOINT) {
            content = aTodoJson(description = "Description One")
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it)
        }
        val createdTwo = mvc.post(POST_ENDPOINT) {
            content = aTodoJson(description = "Description Two")
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it)
        }
        val createdThree = mvc.post(POST_ENDPOINT) {
            content = aTodoJson(description = "Description Three")
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it)
        }

        mvc.get(NOT_DONE_ENDPOINT).andDo { print() }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(3) }
            jsonPath("$[0].uuid") { value(createdOne.read<String>("$.uuid")) }
            jsonPath("$[1].uuid") { value(createdTwo.read<String>("$.uuid")) }
            jsonPath("$[2].uuid") { value(createdThree.read<String>("$.uuid")) }

        }
        val uuid = createdOne.read<String>("$.uuid")
        mvc.put("/todo/$uuid") {
            content = """{
                "status": "${ToDoUpdate.Status.DONE}"   
            }""".trimIndent()
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { is2xxSuccessful() }
        }

        mvc.get(NOT_DONE_ENDPOINT).andDo { print() }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].uuid") { value(createdTwo.read<String>("$.uuid")) }
            jsonPath("$[1].uuid") { value(createdThree.read<String>("$.uuid")) }
        }
    }

    @Test
    fun `fetch todo with past_due state`() {
        val dueDate = utcDateTime().minusMinutes(1)
            .let { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(it) }
        val uuid = mvc.post(POST_ENDPOINT) {
            content = aTodoJson(description = "Description", dueDate = dueDate)
            contentType = MediaType.APPLICATION_JSON
        }.andDo { print() }.andExpect {
            status { isCreated() }
            jsonPath("$.uuid") { isString() }
        }.andReturn().response.contentAsString.let {
            JsonPath.parse(it).read<String>("$.uuid").let(UUID::fromString)
        }
        await.atMost(15, TimeUnit.SECONDS).until {
            repository.fetchAllNotDone().count() == 1L
        }

        await.atMost(15, TimeUnit.SECONDS).until {
            repository.getById(uuid)?.status == "PAST_DUE"
        }

        mvc.get(NOT_DONE_ENDPOINT).andDo { print() }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].status") { value("${ToDoStatus.PAST_DUE}") }
        }
    }


    private fun aTodoJson(
        dueDate: String = utcDateTime().plusDays(10)
            .let { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(it) },
        description: String = "ToDo Description"
    ) = """{
          "description": "$description",
          "due_date": "$dueDate"
        }""".trimIndent()

    private fun utcDateTime() = OffsetDateTime.now(ZoneId.of("UTC"))
}