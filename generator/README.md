# WikiLinks - Generator

Generator's job is to download and convert Wikipedia MySQL dump files to a format that is more suitable for
searching connections between articles.

The generator module downloads Wikipedia dumps for given languages, processes them, and generates search indexes.
When launched, it compares the most recent Wikipedia dump available for given language with the current versions
and exits if update is not needed. Generator should be launched periodically. Wikipedia releases new dumps
on 1st and 20th day of each month, but some files may take a longer time to become available,
so it's better to run generator the next day (`0 0 2,21 * *`).

## Usage

Generator requires minimum **6GiB of heap memory**. This is because at some point it needs to store
all pageId-pageTitle mappings in memory. This requirement is for the english version of Wikipedia, which
is [the largest](https://en.wikipedia.org/wiki/List_of_Wikipedias#Details_table). Other languages require proportionally
less memory.

### Environment variables

* `OUTPUT_LOCATION` - **required** - target location of generated indexes, see below for more details.
* `WORKING_DIRECTORY` - **required** - directory where downloaded Wikipedia dumps and temporary files are stored.
* `JAVA_TOOL_OPTIONS` - **recommended** - should set max heap size (e.g. `-Xmx6G`) that generator will use.
* `CURRENT_VERSION_LOCATION` - *optional* - Location of the current index versions, see below for more details.
* `SKIP_DELETING_DUMPS` - *optional* - if set, downloaded Wikipedia dumps will be retained after processing is done (
  they are deleted by default).
* `BATCH_MODE` - *optional* - disables interactive mode (progress updates are printed periodically, each in new line).
  Recommended to use when collecting generator output logs.
* `GENERATOR_ACTIVE_MAX_AGE` - *optional* - maximum duration (in seconds) of a single generator execution. Note that exceeding
  this value doesn't cause generator to stop. Instead, it is used to detect whether previous generator instance was
  abruptly stopped (and didn't have a chance to update its status).

#### Output location

The `OUTPUT_LOCATION` environment variable tells the generator where to put generated indexes.
It uses URI format. Files by default are put under the path `$OUTPUT_LOCATION/index-file.db` (for
example: `$OUTPUT_LOCATION/links-en-20220801.db.gz`). The path can be changed by using the `include-version-in-path`
parameter (more information below).

The following schemes are supported:

* **file** - moves output files to a directory in a local filesystem.
  Examples:
  ```
  file:///relative/directory/path
  file:////absolute/directory/path
  file:////absolute/directory/path?compress=true
  ```
  Note the difference between relative and absolute paths - the latter uses one additional forward slash before path.
* **s3** - moves output files to an Amazon S3 bucket.
  Example:
  ```
  s3://bucket-name/object/key/prefix?compress=true?include-version-in-path=true
  ```
  Additional S3-specific parameters:
    * `endpoint-host` - override default S3 endpoint, if using another S3-compatible storage provider

  To find out how to configure AWS credentials, follow [this](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html) guide.

Common parameters (URI query params):

* `compress=true` - tells the generator to compress files before moving them to their destination.
* `include-version-in-path=true` - adds version directory to the path (Example: after enabling this option the
  path `s3://bucket-name/dumps/links.db` will change to `s3://bucket-name/dumps/20220801/links.db`).

#### Current version location

The `CURRENT_VERSION_LOCATION` environment variable contains the location from which information about currently
used index versions can be obtained. This ensures that Wikipedia dumps are only downloaded when actually needed,
no matter how often generator is launched.

If the environment variable is not set, generator relies on the `version-manifest.properties`
file in the working directory, which stores most recent version information.

The following URI schemes are supported:

* **file** - should point to the same directory that is being used by the application module as database source (
  the `DATABASES_DIRECTORY` environment variable).
  Examples (the same rules apply as in the `file` schema in the `OUTPUT_LOCATION` environment variable):
  ```
  file:///relative/directory/path
  file:////absolute/directory/path
  ```
  If the generator and application modules run on the same filesystem, the `OUTPUT_LOCATION`
  and `CURRENT_VERSION_LOCATION` environment variables should be set to the same value.

### Program arguments

* `language` (**required**) - a comma-separated list of [ISO 639-1](https://en.wikipedia.org/wiki/ISO_639-1) language
  codes for which to generate search indexes. Supported languages are
  listed [here](../common/src/commonMain/kotlin/dev/drzepka/wikilinks/common/model/dump/DumpLanguage.kt).
* `version` (*optional*) - use specific version of Wikipedia dump. If not provided, the most recent version
  available will be used.

### Docker example

```shell
#!/bin/sh
docker run \
  --rm \
  -it \
  --name wikilinks-generator \
  -p 8080:8080 \
  -v "$(pwd)/databases:/databases" \
  -v "$(pwd)/dumps:/dumps" \
  -e OUTPUT_LOCATION=file:////databases \
  -e CURRENT_VERSION_LOCATION=file:////databases \
  -e WORKING_DIRECTORY=/dumps \
  ghcr.io/drzpk/wikilinks/generator:latest \
  language=pl,en
```

**Note**: don't forget to define the `SKIP_DELETING_DUMPS` environment variable if you wish to retain Wikipedia dumps
downloaded by Generator. Otherwise, they will be deleted upon successful completion.

## Workflow

The size of a Wikipedia dump is so large, that would require a significant amount of memory to be processable in a
single pass. In order to make the generator less memory-intensive, another approach was taken: dump processing is split
into a number of different steps, that process dump files sequentially and store most of the data on disk.

### Steps

All the below steps are processed separately for each language. They don't reflect source code in the exact way, some
items are omitted and others are simplified. Refer to the
code if you're looking for the exact representation of the steps.

Unless otherwise specified, all file operations take place in the `$WORKING_DIRECTORY`, henceforth referred to as
the *working directory*.

The `<language>` expression is a placeholder for [ISO 639-1](https://en.wikipedia.org/wiki/ISO_639-1)
language code. List of all supported languages is
available [here](../common/src/commonMain/kotlin/dev/drzepka/wikilinks/common/model/dump/DumpLanguage.kt).

#### 1. Dump version resolution

The first step is to find the most recent version of dump available on
`https://dumps.wikimedia.org/<language_code>wiki`.
The algorithm checks whether all required files are available for download. These files are located based on suffixes in
their names: *page*, *pagelinks*, and *redirect* (each suffix denotes a table name in the
Wikipedia [database schema](https://www.mediawiki.org/w/index.php?title=Manual:Database_layout/diagram&action=render).
Example of such file: *enwiki-20220720-pagelinks.sql.gz*.

Then, the most recent version is compared against the version currently being used - the details are available in the
*current version location* section above.

#### 2. Dump files download

Files resolved in the previous step are downloaded into the working directory. If file is partially downloaded (for
example if previous generator execution was terminated, or a network error occurred), it resumes downloading. Files
completely downloaded are skipped.

#### 3. Database initialization

Database file and all tables are created.

#### 4. Page table population

File `<language>wiki-<version>-page.sql.gz` is read and data is extracted. From
the [page table](https://www.mediawiki.org/wiki/Manual:Page_table), the `page_id` and `page_title` columns are
extracted, but only if `page_namespace == 0`. All articles are assigned
to [namespace](https://en.wikipedia.org/wiki/Wikipedia:Namespace) 0.

Extracted data is streamed to the resulting link database, and in the same time, the bidirectional
lookup `page_id -> page_title` is kept in memory for use in the next steps. All mappings consume about 3 GiB of memory.

#### 5. Page redirect resolution

File `<language>wiki-<version>-redirect.sql.gz` is read and data is extracted. From
the [redirect table](https://www.mediawiki.org/wiki/Manual:Redirect_table), the `rd_from` and `rd_title` columns are
extracted, but only if `rd_namepace == 0`.

Each redirect in form `from_id -> to_title` is transformed, using the lookup from the previous step,
to `from_id -> to_id`. This data is used to update the redirect column in the resulting link database (see the
documentation of the backend module for more information). At the same, another lookup is created and kept in
memory: `source_page_id -> target_page_id`.

#### 6. Links extraction

File `<language>wiki-<version>-pagelinks.sql.gz` is read and data is extracted.
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

#### 7. Sorting links by source and target page ID

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

This was the last step involved in building the resulting database. The database file is named using the following
format: `links-<language>-<version>.db`.

#### 9. Database file moving

The next step is to move the generated database into the chosen directory. Generator has the ability to move
files into different locations, as described in detail in the usage section.

When the output directory is the same as `DATABASE_DIRECTORY` in the application module and compression is disabled,
the file will be automatically picked up by the application module.

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
