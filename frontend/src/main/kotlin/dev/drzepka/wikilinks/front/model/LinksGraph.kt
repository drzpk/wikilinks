package dev.drzepka.wikilinks.front.model

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult

data class Node(
    @JsName("id")
    val id: Int,
    @JsName("column")
    val column: Int,
    @JsName("posInColumn")
    val posInColumn: Int,
    @JsName("fixed")
    val fixed: Boolean,
    @JsName("text")
    val text: String,
    @JsName("url")
    val url: String
)

data class Edge(
    @JsName("id")
    val id: Int,
    @JsName("source")
    val source: Node,
    @JsName("target")
    val target: Node
)

fun LinkSearchResult.toNodes(): List<Node> {
    if (paths.isEmpty())
        return emptyList()

    val nodes = mutableMapOf<Int, Node>()
    val columnSizes = mutableMapOf<Int, Int>()
    val startNodeId = paths.first().pages.first()
    val endNodeId = paths.first().pages.last()

    for (path in paths) {
        path.pages.forEachIndexed { column, nodeId ->
            if (!nodes.containsKey(nodeId)) {
                val posInColumn = columnSizes[column] ?: 0
                columnSizes[column] = posInColumn + 1

                val node = Node(
                    nodeId,
                    column,
                    posInColumn,
                    nodeId == startNodeId || nodeId == endNodeId,
                    pages[nodeId]!!.title,
                    pages[nodeId]!!.url
                )
                nodes[nodeId] = node
            }
        }
    }

    return nodes.values.toList()
}

fun LinkSearchResult.toEdges(nodes: List<Node>): List<Edge> {
    val nodesById = nodes.associateBy { it.id }
    var nextEdgeId = 0

    return paths
        .flatMap { path -> path.pages.windowed(2, 1) }
        .distinct()
        .map { Edge(nextEdgeId++, nodesById[it[0]]!!, nodesById[it[1]]!!) }
}

fun List<Node>.countRows(): List<Int> = this
    .groupBy { it.column }
    .let { grouped -> (0 until grouped.size).map { grouped[it]!!.size } }

