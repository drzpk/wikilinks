package dev.drzepka.wikilinks.front.component.searchresult

import dev.drzepka.wikilinks.common.model.searchresult.LinkSearchResult
import dev.drzepka.wikilinks.front.d3
import dev.drzepka.wikilinks.front.model.Node
import dev.drzepka.wikilinks.front.model.countRows
import dev.drzepka.wikilinks.front.model.toEdges
import dev.drzepka.wikilinks.front.model.toNodes
import io.kvision.html.Div
import io.kvision.html.TAG
import io.kvision.html.tag
import kotlinx.browser.window

class LinksGraph(result: LinkSearchResult) : Div() {
    private var d3Nodes: dynamic = null
    private var d3Edges: dynamic = null

    private val nodes = result.toNodes()
    private val edges = result.toEdges(nodes)
    private val nodeRows = nodes.countRows()

    init {
        id = CONTAINER_ID
        tag(TAG.SVG)

        if (result.paths.isNotEmpty())
            window.setTimeout({ initializeD3() })
    }

    private fun initializeD3() {
        setNodePositions(nodes)

        val nodeIds = nodes.map { it.id }
        val forceNode = d3.forceManyBody()
            .strength(-200)
        val forceLink = d3.forceLink(edges.toTypedArray())
            .id { it -> nodeIds[it.index as Int] }
            .strength(0.08)

        val simulation = d3.forceSimulation(nodes.toTypedArray())
            .force("link", forceLink)
            .force("charge", forceNode)
            .force("placement", placementForce())
            .on("tick") { tick() }

        if (!SIMULATION_ACTIVE) {
            simulation.stop()
            tick()
        }

        d3.selectAll("#$CONTAINER_ID svg > *").remove()
        val svg = d3.select("#$CONTAINER_ID svg")
            .attr("preserveAspectRatio", "xMinYMin meet")
            .attr("viewBox", arrayOf(0, 0, GRAPH_WIDTH, GRAPH_HEIGHT))

        d3Edges = svg.selectAll("g.edge")
            .data(edges.toTypedArray())
            .join("g")
            .attr("class", "edge")
            .append("line")
            .style("stroke", "black")
            .style("stroke-width", 1)

        d3Nodes = svg.selectAll("g.node")
            .data(nodes.toTypedArray())
            .join("g")
            .attr("class", "node")
            .call(drag(simulation))

        d3Nodes.append("circle")
            .attr("r", NODE_RADIUS)
            .style("fill", "red")

        val d3NodeTextContainer = d3Nodes.append("g")
        val d3Rects = d3NodeTextContainer.append("rect")
        d3NodeTextContainer.append("text")
            .text { d -> d.text }
            .attr("transform", "translate(" + NODE_RADIUS * 1.2 + ", " + NODE_RADIUS / 2 + ")")
            .attr("font-family", "system-ui")
            .each(js("function (d) { d.bbox = this.getBBox(); }"))

        d3Rects
            .attr("x") { d -> d.bbox.x }
            .attr("y") { d -> d.bbox.y }
            .attr("width") { d -> d.bbox.width }
            .attr("height") { d -> d.bbox.height }
            .attr("transform", "translate(" + NODE_RADIUS * 1.2 + ", " + NODE_RADIUS / 2 + ")")
            .style("fill", "#ffffff73")
    }

    private fun setNodePositions(nodes: Collection<Node>) {
        for (node in nodes) {
            val pos = getNodePosition(node)
            val dNode = node.asDynamic()

            dNode.originalFx = pos[0]
            if (node.fixed) {
                dNode.fx = pos[0]
                dNode.fy = pos[1]
            }
        }
    }

    private fun getNodePosition(node: Node): DoubleArray {
        val innerWidth = GRAPH_WIDTH - HORIZONTAL_PADDING
        val widthDelta = innerWidth / (nodeRows.size - 1)
        val x = PADDING_LEFT.toDouble() + widthDelta * node.column

        val y: Double
        val rowsInColumn = nodeRows[node.column]
        y = if (rowsInColumn > 1) {
            val innerHeight = GRAPH_HEIGHT - VERTICAL_PADDING
            val heightDelta = innerHeight / (rowsInColumn - 1)
            PADDING_TOP.toDouble() + heightDelta * node.posInColumn
        } else {
            GRAPH_HEIGHT / 2.0
        }

        return doubleArrayOf(x, y)
    }

    private fun tick() {
        d3Edges
            .attr("x1") { d -> d.source.x }
            .attr("y1") { d -> d.source.y }
            .attr("x2") { d -> d.target.x }
            .attr("y2") { d -> d.target.y }

        d3Nodes
            .attr("transform") { d -> "translate(" + d.x + ", " + d.y + ")" }
    }

    private fun drag(simulation: dynamic): dynamic {
        val dragStarted = evt@{ event: dynamic ->
            if (event.subject.fixed == true)
                return@evt

            if (SIMULATION_ACTIVE) {
                if (!event.active as Boolean) simulation.alphaTarget(0.3).restart()
                event.subject.fx = event.subject.x
                event.subject.fy = event.subject.y
            } else {
                tick()
            }
        }

        val dragged = evt@{ event: dynamic ->
            if (event.subject.fixed == true)
                return@evt

            if (SIMULATION_ACTIVE) {
                event.subject.fx = event.x
                event.subject.fy = event.y
            } else {
                event.subject.x = event.x
                event.subject.y = event.y
                tick()
            }
        }

        val dragEnded = evt@{ event: dynamic ->
            if (event.subject.fixed == true)
                return@evt

            if (SIMULATION_ACTIVE) {
                if (event.active != true) simulation.alphaTarget(0)
                event.subject.fx = null
                event.subject.fy = null
                simulation.alpha(1).restart()
            } else {
                tick()
            }
        }

        return d3.drag()
            .on("start", dragStarted)
            .on("drag", dragged)
            .on("end", dragEnded)
    }

    private fun placementForce(): () -> Unit {
        val factor = 1.9
        var nodes: Array<dynamic> = emptyArray()

        fun calculateSpeed(position: Double, targetPosition: Double): Double {
            // Ignore the alpha parameter (https://github.com/d3/d3-force#simulation_alpha), because
            // the goal of this force is always to move a node to its original position.
            val distance = targetPosition - position
            return distance / factor
        }

        val force = {
            for (node in nodes) {
                if (node.fx != null || node.originalFx == null)
                    continue

                node.vx += calculateSpeed(node.x as Double, node.originalFx as Double)
                if (node.y < PADDING_TOP)
                    node.vy += calculateSpeed(node.y as Double, PADDING_TOP.toDouble())
                else if (node.y > GRAPH_HEIGHT - PADDING_BOTTOM)
                    node.vy += calculateSpeed(node.y as Double, GRAPH_HEIGHT - PADDING_BOTTOM.toDouble())
            }
        }

        force.asDynamic().initialize = { _nodes: dynamic -> nodes = _nodes as Array<dynamic> }

        return force
    }

    companion object {
        private const val CONTAINER_ID = "links-graph-container"
        private const val SIMULATION_ACTIVE = true

        // Graph width and height are only used to determine aspect ratio of the svg element
        private const val GRAPH_WIDTH = 1000
        private const val GRAPH_HEIGHT = 400
        private const val PADDING_TOP = 40
        private const val PADDING_BOTTOM = 40
        private const val PADDING_LEFT = 20
        private const val PADDING_RIGHT = 80
        private const val NODE_RADIUS = 8

        private const val VERTICAL_PADDING = PADDING_TOP + PADDING_BOTTOM
        private const val HORIZONTAL_PADDING = PADDING_LEFT + PADDING_RIGHT
    }
}
