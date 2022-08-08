package dev.drzepka.wikilinks.inttest.search

import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseProvider
import dev.drzepka.wikilinks.app.utils.http
import dev.drzepka.wikilinks.common.model.LinkSearchRequest
import dev.drzepka.wikilinks.common.model.Path
import dev.drzepka.wikilinks.common.model.database.DatabaseFile
import dev.drzepka.wikilinks.common.model.database.DatabaseType
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.io.File

@Suppress("HttpUrlsUsage")
@Testcontainers
abstract class BaseLinkSearchTest(imageVariant: String) {

    private val databasesDirectory = File("./databases-test")
    private val imageTag =
        System.getProperty("imagePrefix") + "/application-$imageVariant:" + System.getProperty("imageVersion")

    private val network = Network.newNetwork()

    @Container
    private val mockServer = MockServerContainer(DockerImageName.parse("mockserver/mockserver"))
        .withNetwork(network)
        .withNetworkAliases("mockServer")

    @Container
    private val application = GenericContainer(DockerImageName.parse(imageTag))
        .withExposedPorts(8080)
        .withNetwork(network)
        .withEnv("DATABASES_DIRECTORY", "/databases")
        .withEnv("WIKIPEDIA_ACTION_API_URL_EN", "http://mockServer:${MockServerContainer.PORT}/api.xyz")
        .withFileSystemBind(databasesDirectory.absolutePath, "/databases")
        .waitingFor(Wait.forHttp("/").forStatusCode(200))

    init {
        if (!databasesDirectory.isDirectory)
            databasesDirectory.mkdir()
        databasesDirectory.listFiles()!!.forEach { it.delete() }

        initializeDatabase()
    }

    @BeforeEach
    fun setupEach() {
        application.followOutput(Slf4jLogConsumer(LoggerFactory.getLogger("wikilinks-container")))

        MockServerClient(mockServer.host, mockServer.serverPort)
            .`when`(
                request()
                    .withPath("/api.xyz")
                    .withQueryStringParameter("action", "query")
            )
            .respond(
                response()
                    .withHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    .withBody(PageInfoMockResponse.create(1, 2, 3))
            )
    }

    @Test
    fun test() {
        val url = "http://${application.host}:${application.getMappedPort(8080)}/api/links/search"
        val response = runBlocking {
            val httpResponse = http.post(url) {
                contentType(ContentType.Application.Json)
                val source = LinkSearchRequest.SearchPoint(id = 1)
                val target = LinkSearchRequest.SearchPoint(id = 2)
                setBody(LinkSearchRequest(source, target, DumpLanguage.EN))
            }

            if (httpResponse.status != HttpStatusCode.OK)
                throw IllegalStateException("Received error response: ${httpResponse.bodyAsText()}")

            httpResponse.body<LinkSearchResult>()
        }

        val paths = response.paths
        assertEquals(1, paths.size)
        assertTrue(Path(1, 2) in paths)

        assertTrue(response.pages.containsKey(1))
        assertTrue(response.pages.containsKey(2))
        assertTrue(response.pages.containsKey(3))
    }

    private fun initializeDatabase() {
        val linksFile = DatabaseFile.create(DatabaseType.LINKS, DumpLanguage.EN, "20220601")
        val linksDb = DatabaseProvider().getOrCreateUnprotectedLinksDatabase(linksFile, databasesDirectory.absolutePath)

        linksDb.pagesQueries.insert(1, "title_1")
        linksDb.pagesQueries.insert(2, "title_2")
        linksDb.pagesQueries.insert(3, "title_3")

        linksDb.linksQueries.insert(1, 0, 2, "", "2,3")
        linksDb.linksQueries.insert(2, 1, 0, "1", "")
        linksDb.linksQueries.insert(3, 1, 0, "1", "")
    }
}
