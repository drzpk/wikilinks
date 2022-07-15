package dev.drzepka.wikilinks.app

import dev.drzepka.wikilinks.app.cache.PageCacheService
import dev.drzepka.wikilinks.app.config.Configuration
import dev.drzepka.wikilinks.app.db.*
import dev.drzepka.wikilinks.app.service.FrontendResourceService
import dev.drzepka.wikilinks.app.service.HistoryService
import dev.drzepka.wikilinks.app.service.search.LinkSearchService
import dev.drzepka.wikilinks.app.service.search.PageInfoService
import dev.drzepka.wikilinks.app.service.search.PathFinderService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

val coreModule = module {
    single { DatabaseProvider.getLinksDatabase() }
    single<LinksRepository> { DbLinksRepository(get()) }

    single { PathFinderService(get()) }
    single { LinkSearchService(get(), getOrNull()) }
}

val fullModule = module {
    single { DatabaseProvider.getCacheDatabase() }
    single { DatabaseProvider.getHistoryDatabase() }

    single<ConfigRepository> { FileConfigRepository(Configuration.databasesDirectory!!) }
    single<PagesRepository> { DbPagesRepository(get()) }
    single<HistoryRepository> { DbHistoryRepository(get()) }

    single { PageCacheService(get()) }
    single { PageInfoService(get(), get()) }
    single { HistoryService(get(), get()) }
    single { FrontendResourceService() }
}

object KoinApp : KoinComponent {
    val searchService: LinkSearchService by inject()
    val frontendResourceService: FrontendResourceService by inject()
    val historyService: HistoryService by inject()
}
