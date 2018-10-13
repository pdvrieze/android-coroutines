import groovy.util.Node
import org.gradle.api.XmlProvider

inline fun XmlProvider.dependencies(config: Node.() -> Unit): Unit {
    asNode().dependencies(config)
}

inline fun Node.dependencies(config: Node.() -> Unit): Node {
    @Suppress("UNCHECKED_CAST")
    val ch = children() as List<Node>
    val node: Node = ch.firstOrNull { it.name() == "dependencies" } ?: appendNode("dependencies")
    return node.apply(config)
}

fun Node.dependency(spec: String, type: String = "jar", scope: String = "compile", optional: Boolean = false): Node {
    return spec.split(':', limit = 3).run {
        val groupId = get(0)
        val artifactId = get(1)
        val version = get(2)
        dependency(groupId, artifactId, version, type, scope, optional)
    }
}

fun Node.dependency(groupId: String, artifactId: String, version: String, type: String = "jar", scope: String = "compile", optional: Boolean = false): Node {
    return appendNode("dependency").apply {
        appendNode("groupId", groupId)
        appendNode("artifactId", artifactId)
        appendNode("version", version)
        appendNode("type", type)
        if (scope != "compile") appendNode("scope", scope)
        if (optional) appendNode("optional", "true")
    }
}
