package dev.drzepka.wikilinks.updater.service

import dev.drzepka.wikilinks.updater.Config
import dev.drzepka.wikilinks.updater.aws.EC2Facade
import dev.drzepka.wikilinks.updater.model.InstanceSummary
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import java.time.Duration
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class GeneratorDispatcherTest {

    @MockK(relaxed = true)
    private lateinit var configRepository: ConfigRepository

    @MockK(relaxUnitFun = true)
    private lateinit var ec2Facade: EC2Facade

    @InjectMockKs
    private lateinit var dispatcher: GeneratorDispatcher

    @BeforeEach
    fun beforeEach() {
        System.setProperty("DATABASES_DIRECTORY", "dir")
        System.setProperty("LAUNCH_TEMPLATE_ID", "templateId")
    }

    @Test
    fun `should launch generator when there's no instance already running`() {
        every { ec2Facade.getInstanceSummary(any()) } returns null

        dispatcher.handleVersion("version")

        verify { ec2Facade.launchInstance("templateId", Config.LOCATOR_TAG_VALUE, "version") }
        verify(exactly = 0) { ec2Facade.terminateInstance(any()) }
    }

    @Test
    fun `should not start generator if it's already running and age isn't exceeded`() {
        val createdAt = Instant.now().minus(Duration.ofSeconds(Config.GENERATOR_INSTANCE_MAX_AGE.seconds / 2))
        every { ec2Facade.getInstanceSummary(any()) } returns
                InstanceSummary("instanceId", InstanceStateName.RUNNING, createdAt)

        dispatcher.handleVersion("version")

        verify(exactly = 0) { ec2Facade.launchInstance(any(), any(), any()) }
        verify(exactly = 0) { ec2Facade.terminateInstance(any()) }
    }

    @Test
    fun `should terminate existing generator if it's running for too long - when starting new generator`() {
        val createdAt = Instant.now().minus(Config.GENERATOR_INSTANCE_MAX_AGE).minus(Duration.ofSeconds(1))
        every { ec2Facade.getInstanceSummary(any()) } returns  // Null at the second call means that the instance was terminated
                InstanceSummary("instanceId", InstanceStateName.RUNNING, createdAt) andThen null

        dispatcher.handleVersion("version")

        verify { ec2Facade.terminateInstance("instanceId") }
        verify(exactly = 2) { ec2Facade.getInstanceSummary(any()) }
        verify { ec2Facade.launchInstance("templateId", Config.LOCATOR_TAG_VALUE, "version") }
    }

    @Test
    fun `should terminate existing generator if it's running for too long - when doing periodical check`() {
        val createdAt = Instant.now().minus(Config.GENERATOR_INSTANCE_MAX_AGE).minus(Duration.ofSeconds(1))
        every { ec2Facade.getInstanceSummary(any()) } returns
                InstanceSummary("instanceId", InstanceStateName.RUNNING, createdAt) andThen null

        dispatcher.handleVersion(null)

        verify { ec2Facade.terminateInstance("instanceId") }
        verify(exactly = 0) { ec2Facade.launchInstance(any(), any(), any()) }
    }

    @Test
    fun `should should ensure that maintenance mode is disabled when terminating existing generator`() {
        val createdAt = Instant.now().minus(Config.GENERATOR_INSTANCE_MAX_AGE).minus(Duration.ofSeconds(1))
        every { ec2Facade.getInstanceSummary(any()) } returns
                InstanceSummary("instanceId", InstanceStateName.RUNNING, createdAt) andThen null
        every { configRepository.isMaintenanceModeActive() } returns true

        dispatcher.handleVersion(null)

        verify { configRepository.deactivateMaintenanceMode() }
    }
}
