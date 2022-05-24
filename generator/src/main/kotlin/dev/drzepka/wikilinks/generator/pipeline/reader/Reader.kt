package dev.drzepka.wikilinks.generator.pipeline.reader

import java.io.Closeable

interface Reader : Iterator<String>, Closeable
