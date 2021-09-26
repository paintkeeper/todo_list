package app.todos

import app.todos.api.ToDosApiService
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationTest {

    @MockkBean
    lateinit var apiServiceMock: ToDosApiService

    @Test
    fun initContext() {
    }
}