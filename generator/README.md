# WikiLinks - Generator

Generator's job is to download and convert Wikipedia MySQL dump files to a format that is more suitable for
searching connections between articles.

## Workflow

The size of a Wikipedia dump is so large, that would require a significant amount of memory to be processable in a
single pass. In order to make the generator less memory-intensive, another approach was taken: dump processing is split
into a number of different steps, that process dump files sequentially and store most of the data on disk.

### Steps

These steps don't reflect source code in the exact way, some items are omitted and others are simplified. Refer to the
code if you're looking for the exact representation of the steps.

Unless otherwise specified, all file operations take place in the `$WORKING_DIRECTORY`, referred to as the  *working
directory* from now on.

#### 1. Dump version resolution

The first step is to find the most recent version of dump available on https://dumps.wikimedia.org/enwiki.
The algorithm checks whether all required files are available for download. These files are located based on suffixes in
their names: *page*, *pagelinks*, and *redirect* (each suffix denotes a table name in the
Wikipedia [database schema](https://www.mediawiki.org/w/index.php?title=Manual:Database_layout/diagram&action=render).
Example of such file: *enwiki-20220720-pagelinks.sql.gz*.

If current dump version (set by the database swap step) is the same as the resolved version, generator is stopped.

#### 2. Dump files download

Files resolved in the previous step are downloaded into the working directory. If file is partially downloaded (for
example if previous generator execution was terminated, or a network error occurred), it resumes downloading. Files
completely downloaded are skipped.

#### 3. Database initialization

Database file along with all tables is created.

#### 4. Page table population

File *enwiki-<version>-page.sql.gz* is read and data is extracted. From
the [page table](https://www.mediawiki.org/wiki/Manual:Page_table), the `page_id` and `page_title` columns are
extracted, but only if `page_namespace == 0`. All articles are assigned
to [namespace](https://en.wikipedia.org/wiki/Wikipedia:Namespace) 0.

Extracted data is streamed to the resulting link database, and in the same time, the bidirectional
lookup `page_id -> page_title` is kept in memory for use in the next steps. All mappings consume about 3 GiB of memory.

#### 5. Page redirect resolution

File *enwiki-<version>-redirect.sql.gz* is read and data is extracted. From
the [redirect table](https://www.mediawiki.org/wiki/Manual:Redirect_table), the `rd_from` and `rd_title` columns are
extracted, but only if `rd_namepace == 0`.

Each redirect in form `from_id -> to_title` is transformed, using the lookup from the previous step,
to `from_id -> to_id`. This data is used to update the redirect column in the resulting link database (see the
documentation of the backend module for more information). At the same, another lookup is created and kept in
memory: `source_page_id -> target_page_id`.

#### 6. Links extraction

File *enwiki-<version>-pagelinks.sql.gz* is read and data is extracted.
The [pagelinks table](https://www.mediawiki.org/wiki/Manual:Pagelinks_table) contains all internal Wikipedia links, but
the storage format is not optimal for fast searching, because each link is a separate table row. The format used by this
project will be revealed in one of the next steps.

Similarly to the previous steps, only links with `pl_from_namespace == 0` are processed. Links stored in the format
of `from_id -> to_title`, so before storing them in the resulting database, they must be converted to
format `from_id -> to_id` using the page lookup.

In this step, the redirect lookup is used to determine whether either or both sides of a link is a redirect. If so, it
is replaced with the target it links to before it is stored on disk. After that, both sides of the
link entry are checked if they are present in the page lookup. If either one is not, it means that the entry leads
nowhere (or to a page in another namespace, which is effectively the same) and is not saved to disk. The algorithm
described in this paragraph is implemented in
the [LinksProcessor](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/processor/LinksProcessor.kt) class.

Link entries that have come this far, are written to the `id_links.txt.gz` file. Each line has the following
format: `<from>,<to>`.

After this step is complete, both of the lookups are not needed anymore and are removed from memory.

#### 7. Sorting links by source and target page ID - todo: mention custom implementation of external sort algorithm

This step's task is to read the `id_links.txt.gz` file created by the previous step and sort it by source and target
page id. The outcome of this step are two files: `id_links_sorted_source.txt.gz` and `id_links_sorted_target.txt.gz`.
They
use the same format as the source file before sorting.

The next step requires the two inputs: links sorted by source and target page id. The total number of links that were
processed by the previous step is almost 1 billion (at the time of writing this sentence) so sorting them in memory
would probably require dozens of GiB. In order to make it possible for generator to be launched on personal computers or
save some money when launched on virtual machines in cloud, a custom implementation of
the [external sorting algorithm](https://en.wikipedia.org/wiki/External_sorting#External_merge_sort) was written.

The external sorting algorithm is implemented by
the [LinksFileSorter](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/sort/LinksFileSorter.kt). The algorithm
works by splitting a large file into a smaller chunks that can be sorted in memory and saved to isk, one at a time. When
all chunks are sorted, there are merged. Notice that the chunks are already sorted, so they be partially loaded into
memory.

The number of chunks is calculated based on maximum heap size. The exact formula was determined experimentally - it was
adjusted until generator was no loger throwing out of memory exceptions.

#### 8. Links table population

The link table has the following columns:

* page_id
* in_links_count
* out_links_count
* in_links
* out_links

Each row must be inserted in a complete form, any updates on already inserted row would cause major performance issues,
given the huge number of link entries. That's the reason why the entries must be sorted before being processed by this
step.

Let's consider the following stream of link entries sorted by source link:

```
10,100
10,200
10,300
20,100
20,200
```

When reading the stream, algorithm keeps track of all target links for given source link. If the source link changes,
it means that all available target links for given source are already known. The same applies for link entries
sorted by target link. By synchronizing two readers - one for links sorted by source and one for sorted by target,
the algorithm can obtain data required for one table row and insert it in a single query. The described algorithm can be
found in the [LinksPipelineManager](src/main/kotlin/dev/drzepka/wikilinks/generator/LinksPipelineManager.kt) class.

This was the last step involved in building the resulting database.

#### 9. Database swap

The next step is to swap the database currently being used by the application with a new one. Generator creates
an empty file named `maintenance_mode` in the `$DATABASES_DIRECTORY`. The application periodically check for existence
of this file and if it's detected, releases all database connections. In the meantime, the generator waits 30 seconds
for the application to take action. After this time has passed, new database file from the working directory to
the `$DATABASES_DIRECTORY` and the cache database from the `$DATABASES_DIRECTORY` is removed.

#### 10. Temporary files deletion

The last step is to delete all temporary files created by one of the previous steps. Wikipedia dump files are also
deleted, unless the `SKIP_DELETING_DUMPS` environment variable is defined.

### File processing

Wikipedia dump files as well as generator's temporary files are stored in GZip archives. Generator reads these files
line by line using the [GZipReader](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/reader/GZipReader.kt)
class.

Each Wikipedia dump used by generator contains MySQL statements used to recreate a single table.
Generator is only interested in `INSERT` statements. They are read in the following manner:

1. [SqlDumpReader](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/reader/SqlDumpReader.kt) (a subclass of
   GZipReader) filters lines from a dump and returns only those containing the insert statements.
2. Whole lines are put into a queue, from which they are picked up by a number
   of [SqlWorker](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/worker/SqlWorker.kt)s working in parallel on
   multiple threads. Their task is to coordinate deserialization of insert statements into a series of Kotlin basic
   types (`String`, `Number`, `null`), that can be further processed.
3. The deserialization itself if handled
   by [SqlStatementParser](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/SqlStatementParser.kt)
   and [ValueParser](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/ValueParser.kt) that work in tandem and
   when given a line with an insert statement, converts it to object form. SqlStatementParser splits the statement into
   a list of `VALUES`, whereas ValueParser deserializes each value.
4. Deserialized values are read
   by [WriterWorker](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/worker/WriterWorker.kt), which coordinates
   a [Writer](src/main/kotlin/dev/drzepka/wikilinks/generator/pipeline/writer/Writer.kt). It's writer responsibility to
   understand the value format and process it accordingly (store it in a database or a file).

## Error recovery

Generator supports basic error recovery. Each step can save its status and react appropriately, when generator is
restarted after a failure. Step data is stored in the `flow_storage.json` file.

Dump file download can also recover from errors. If partially downloaded file is detected, the download process resumes
from the moment it was interrupted.
