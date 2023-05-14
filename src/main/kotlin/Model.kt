package to.bnt.plugin.counts

// to my understanding separators are normalized across OSes in VFS
const val PATH_SEPARATOR = "/"

private fun splitPath(path: String) = path.split(PATH_SEPARATOR)

data class Counts(val classes: Int, val functions: Int) {
    companion object {
        fun default() = Counts(0, 0)
    }
}

open class CountsTree(val name: String)

class CountsFolder(name: String) : CountsTree(name) {
    val children = mutableMapOf<String, CountsTree>()

    private fun traversePath(path: String): CountsTree? {
        var node: CountsTree = this

        for (pathName in splitPath(path)) {
            if (node is CountsFolder) {
                val next = node.children[pathName] ?: return null
                node = next
            } else {
                return null
            }
        }

        return node
    }

    fun pathExists(path: String) = traversePath(path) != null

    fun getCountsForPath(path: String): Counts? {
        val file = traversePath(path) as? CountsFile ?: return null
        return file.counts
    }

    fun removePath(path: String): Counts? {
        val treePath = mutableListOf<CountsFolder>()
        var node: CountsTree = this

        for (pathName in splitPath(path)) {
            if (node is CountsFolder) {
                treePath.add(node)
                val next = node.children[pathName] ?: return null
                node = next
            } else {
                return null
            }
        }

        val removedCounts = (node as? CountsFile)?.counts

        treePath.last().children.remove(node.name)

        for ((leaf, parent) in treePath.reversed().windowed(2)) {
            if (leaf.children.isEmpty()) parent.children.remove(leaf.name)
            else break
        }

        return removedCounts
    }

    fun setCountsForPath(path: String, counts: Counts): CountsFile {
        var node: CountsFolder = this
        val pathNames = splitPath(path).toMutableList()
        val filename = pathNames.removeLast()

        for (pathName in pathNames) {
            val next = node.children[pathName]
            if (next == null || next !is CountsFolder) {
                val newNext = CountsFolder(pathName)
                node.children[pathName] = newNext
                node = newNext
            } else {
                node = next
            }
        }

        val file = CountsFile(filename, counts)

        node.children[filename] = file

        return file
    }
}

class CountsFile(name: String, val counts: Counts) : CountsTree(name)
