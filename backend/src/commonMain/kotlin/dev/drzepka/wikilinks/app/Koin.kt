package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.cache.PageCacheService
import dev.drzepka.wikilinks.app.db.*
import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseRegistry
import dev.drzepka.wikilinks.app.service.DumpUpdaterService
import dev.drzepka.wikilinks.app.service.FrontendResourceService
import dev.drzepka.wikilinks.app.service.HealthService
import dev.drzepka.wikilinks.app.service.HistoryService
import dev.drzepka.wikilinks.app.service.search.LinkSearchService
import dev.drzepka.wikilinks.app.service.search.PageInfoService
import dev.drzepka.wikilinks.app.service.search.PathFinderService
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

fun coreModule() = module {
    single { DatabaseRegistry() }
    single<LinksRepository> { DbLinksRepository(get()) }

    single { PathFinderService(get()) }
    single { LinkSearchService(get(), get(), getOrNull()) }
}

fun fullModule(scope: CoroutineScope) = module {
    single<PagesRepository> { DbPagesRepository(get()) }
    single<HistoryRepository> { DbHistoryRepository(get()) }

    single { PageCacheService(get()) }
    single { PageInfoService(get(), get()) }
    single { HistoryService(get()) }
    single { FrontendResourceService() }
    single(createdAtStart = true) { DumpUpdaterService(scope, get()) }
    single(createdAtStart = true) { HealthService() }
}

object KoinApp : KoinComponent {
    val searchService: LinkSearchService by inject()
    val frontendResourceService: FrontendResourceService by inject()
    val historyService: HistoryService by inject()
    val healthService: HealthService by inject()
    val databaseRegistry: DatabaseRegistry by inject()
}
