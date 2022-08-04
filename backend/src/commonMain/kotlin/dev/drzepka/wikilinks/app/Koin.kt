package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.cache.PageCacheService
import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.db.*
import dev.drzepka.wikilinks.app.db.infrastructure.DatabaseProvider
import dev.drzepka.wikilinks.app.service.DumpUpdaterService
import dev.drzepka.wikilinks.app.service.FrontendResourceService
import dev.drzepka.wikilinks.app.service.HealthService
import dev.drzepka.wikilinks.app.service.HistoryService
import dev.drzepka.wikilinks.app.service.search.LinkSearchService
import dev.drzepka.wikilinks.app.service.search.PageInfoService
import dev.drzepka.wikilinks.app.service.search.PathFinderService
import dev.drzepka.wikilinks.common.model.dump.DumpLanguage
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

private val databaseProvider = DatabaseProvider()

fun coreModule() = module {
    single { databaseProvider.getLinksDatabase(DumpLanguage.EN) } // todo
    single<LinksRepository> { DbLinksRepository(get()) }

    single { PathFinderService(get()) }
    single { LinkSearchService(get(), getOrNull()) }
}

fun fullModule(scope: CoroutineScope) = module {
    single { databaseProvider }
    single { databaseProvider.getCacheDatabase(DumpLanguage.EN) } // todo
    single { databaseProvider.getHistoryDatabase() }

    single<ConfigRepository> { FileConfigRepository(Configuration.databasesDirectory!!) }
    single<PagesRepository> { DbPagesRepository(get()) }
    single<HistoryRepository> { DbHistoryRepository(get()) }

    single { PageCacheService(get()) }
    single { PageInfoService(get(), get()) }
    single { HistoryService(get(), get()) }
    single { FrontendResourceService() }
    single(createdAtStart = true) { DumpUpdaterService(scope, get()) }
    single(createdAtStart = true) { HealthService(get()) }
}

object KoinApp : KoinComponent {
    val searchService: LinkSearchService by inject()
    val frontendResourceService: FrontendResourceService by inject()
    val historyService: HistoryService by inject()
    val dumpUpdaterService: DumpUpdaterService by inject()
    val healthService: HealthService by inject()
}
