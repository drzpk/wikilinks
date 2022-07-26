# WikiLinks - backend

## Database layout

WikiLinks uses SQLite database. It was chosen to minimize resource usage (it doesn't require a separate database engine
process) and to make Wikipedia dump upgrades as simple as possible (it's just a matter of moving and deleting a few
files).

[SQLDelight](https://cashapp.github.io/sqldelight/) library is used to make interaction with database easier, including
Kotlin multiplatform support.

WikiLinks uses multiple [database schemas](src/commonMain/sqldelight). They are separated, because their lifecycles
differ.

### Link database

Contains connections between Wikipedia articles. This database is replaced everytime a new Wikipedia dump version comes
out. Once created, it is used in read-only mode.

Table schema:

* Pages
    * `id`
    * `title`
    * `redirects_to` - if not null, contains page id this page redirects to
* Links
    * `page_id`
    * `in_links_count` - how many pages link to this page
    * `out_links_count` - how many pages this page links to
    * `in_links` - identifiers of pages that link to this page, separated by comma
    * `out_links` - identifiers of pages this page links to, separated by comma

Keeping all incoming and outgoing links in a single row allows for way faster search.

### Cache database

Contains all information used to build a search result, that would otherwise have to be downloaded from the Wikipedia
API. Cache currently does not expire, the whole database is deleted when the link database is replaced.

Table schema:

* PageCache
    * `page_id`
    * `created_at` - when a cache entry was created, in ISO 8601 format
    * `last_accessed_at` - when a cache entry was most recently used, in ISO 8601 format
    * `hits` - how many times an entry was accessed
    * `url_title` - title of an article visible in the article url
    * `display_title` - title of an article visible in the article itself
    * `description` - short description of an article
    * `image_url` - url of an article thumbnail, if the article has one

### History database

Contains all historical search results. This database is the only one that survives Wikipedia version updates.

Table schema:

* SearchHistory
    * `id`
    * `date` - search date, in epoch milliseconds
    * `version` - Wikipedia dump version used for the search (e.g. *20220620*)
    * `source` - source page id
    * `target` - target page id
    * `path_count` - number of distinct paths that were found during this search
    * `degs_of_separation` - degrees of separation for given search
    * `search_duration_paths_ms` - how much time it took to find all paths in the link database, in milliseconds.
    * `search_duration_total_ms` - total search time, including communication with Wikipedia API, cache search, etc., in
      milliseconds
    * `cache_hit_ratio` - radio of page information found in cache to all page information that was needed
