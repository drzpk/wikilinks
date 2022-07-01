package dev.drzepka.wikilinks.updater.model

import software.amazon.awssdk.services.ec2.model.InstanceStateName
import java.time.Instant

data class InstanceSummary(val id: String, val state: InstanceStateName, val createdAt: Instant)
