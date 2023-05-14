package to.bnt.plugin.counts

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBEmptyBorder
import com.jetbrains.rd.util.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.GridLayout
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

private val LOG = logger<CountsToolWindow>()

fun Panel.addFolder(folder: CountsFolder, indent: Int) {
    var leafFolder = folder
    val foldersName = StringBuilder(folder.name)

    // concatenate folders if it's a single child
    while (leafFolder.children.size == 1) {
        val child = leafFolder.children.first().value

        if (child is CountsFolder) {
            leafFolder = child
            foldersName.append(PATH_SEPARATOR, child.name)
        } else {
            break
        }
    }

    row {
        panel {
            row {
                // label creates horizontal padding
                repeat(indent) {
                    label("")
                }
                icon(AllIcons.Nodes.Folder)
                label(foldersName.toString())
            }
        }
    }.layout(RowLayout.PARENT_GRID)

    for (child in leafFolder.children.values) {
        addNode(child, indent + 1)
    }
}

fun Panel.addFile(file: CountsFile, indent: Int) {
    row {
        panel {
            row {
                // label creates horizontal padding
                repeat(indent) {
                    label("")
                }
                icon(AllIcons.FileTypes.Any_type)
                label(file.name)
            }
        }
        label(file.counts.classes.toString())
        label(file.counts.functions.toString())
    }.layout(RowLayout.PARENT_GRID)
}

fun Panel.addNode(node: CountsTree, indent: Int) {
    when (node) {
        is CountsFolder -> {
            addFolder(node, indent)
        }

        is CountsFile -> {
            addFile(node, indent)
        }
    }
}

class CountsToolWindow : ToolWindowFactory, DumbAware {
    private fun createTable(counts: CountsFolder): JComponent {
        val panel = panel {
            row {
                label("Name").resizableColumn()
                label("Classes")
                label("Functions")
            }.layout(RowLayout.PARENT_GRID)

            separator()

            addNode(counts, 0)
        }

        panel.border = JBEmptyBorder(10)
        return panel
    }

    private lateinit var loader: JBLoadingPanel
    private lateinit var scrollPane: JBScrollPane

    private fun updateTable(table: JComponent) {
        val rememberScrollPosition = scrollPane.viewport.viewPosition
        scrollPane.setViewportView(table)
        scrollPane.viewport.viewPosition = rememberScrollPosition
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager

        scrollPane = JBScrollPane(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        loader = JBLoadingPanel(GridLayout(), contentManager)

        loader.add(scrollPane)
        loader.startLoading()

        val content = contentManager.factory.createContent(loader, null, false)
        contentManager.addContent(content)

        project.service<CountsService>().let {
            it.coroutineScope.launch(Dispatchers.Default) {
                val counts = it.getCounts()
                val table = createTable(counts)

                ApplicationManager.getApplication().invokeLater {
                    updateTable(table)
                    loader.stopLoading()
                }

                it.setChangeListener { newCounts ->
                    val newTable = createTable(newCounts)

                    ApplicationManager.getApplication().invokeLater {
                        updateTable(newTable)
                    }
                }
            }
        }
    }
}