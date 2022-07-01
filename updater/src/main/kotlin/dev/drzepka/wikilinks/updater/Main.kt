package dev.drzepka.wikilinks.updater

import dev.drzepka.wikilinks.updater.aws.EC2Facade
import dev.drzepka.wikilinks.updater.service.ConfigRepository
import dev.drzepka.wikilinks.updater.service.GeneratorDispatcher
import dev.drzepka.wikilinks.updater.service.UpdateChecker

fun main() {
    println("Starting WikiLinks updater")

    val configRepository = ConfigRepository()
    val ec2Facade = EC2Facade()

    val newVersion = UpdateChecker(configRepository).getNewVersion()
    GeneratorDispatcher(configRepository, ec2Facade).handleVersion(newVersion)
}


