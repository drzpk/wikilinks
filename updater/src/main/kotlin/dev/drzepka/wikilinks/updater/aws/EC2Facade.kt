package dev.drzepka.wikilinks.updater.aws

import dev.drzepka.wikilinks.updater.model.InstanceSummary
import dev.drzepka.wikilinks.updater.model.InstanceTag
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.*
import java.time.Instant

class EC2Facade {
    private val client = Ec2Client.builder().build()

    fun getInstanceSummary(locatorTagValue: String): InstanceSummary? {
        val tagName = InstanceTag.LOCATOR.text
        val request = DescribeInstancesRequest.builder()
            .filters(createFilters(locatorTagValue))
            .build()

        val result = client.describeInstances(request)
        val instances = result.reservations()
            .flatMap { it.instances() }
            .filter { it.state().name() != InstanceStateName.TERMINATED }

        if (instances.isEmpty()) {
            println("No generator instance was found")
            return null
        } else if (instances.size > 1) {
            println("Warning: more that one instance with tag $tagName was found, only the first one will be used")
        }

        val instance = instances.first()
        return InstanceSummary(instance.instanceId(), instance.state().name(), extractCreationTime(instance))
    }

    fun launchInstance(launchTemplateId: String, locatorTagValue: String, dumpVersion: String) {
        val launchTemplate = LaunchTemplateSpecification.builder()
            .launchTemplateId(launchTemplateId)
            .build()

        val request = RunInstancesRequest.builder()
            .launchTemplate(launchTemplate)
            .minCount(1)
            .maxCount(1)
            .build()

        val response = client.runInstances(request)
        val id = response.instances().first().instanceId()

        // Tags must be added after creating the instance so tags from the template are not overridden.
        addTags(id, locatorTagValue, dumpVersion)
    }

    fun terminateInstance(id: String) {
        val request = TerminateInstancesRequest.builder()
            .instanceIds(id)
            .build()

        println("Terminating instance $id")
        client.terminateInstances(request)
        println("Instance $id has been terminated")
    }

    private fun createFilters(locatorTagValue: String): List<Filter> = listOf(
        Filter.builder().name("tag-key").values(InstanceTag.LOCATOR.text).build(),
        Filter.builder().name("tag-value").values(locatorTagValue).build()
    )

    private fun addTags(instanceId: String, locatorTagValue: String, dumpVersion: String) {
        val tags = listOf(
            Tag.builder().key(InstanceTag.LOCATOR.text).value(locatorTagValue).build(),
            Tag.builder().key(InstanceTag.CREATED_AT.text).value(Instant.now().toString()).build(),
            Tag.builder().key(InstanceTag.DUMP_VERSION.text).value(dumpVersion).build()
        )

        val request = CreateTagsRequest.builder()
            .resources(instanceId)
            .tags(tags)
            .build()
        client.createTags(request)
    }

    private fun extractCreationTime(instance: Instance): Instant {
        var time = instance.tags()
            .firstOrNull { it.key() == InstanceTag.CREATED_AT.text }
            ?.value()
            ?.let { Instant.parse(it) }

        if (time == null) {
            println("No ${InstanceTag.CREATED_AT} tag was found in instance ${instance.instanceId()}, assuming it was created a long time ago.")
            time = Instant.MIN
        }

        return time!!
    }
}
