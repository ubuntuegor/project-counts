package to.bnt.plugin.counts

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

private val LOG = logger<CountsService>()

@Service(Service.Level.PROJECT)
class CountsService(private val project: Project) : Disposable {
    private var projectCounts: CountsFolder? = null

    val coroutineScope = CoroutineScope(SupervisorJob())

    override fun dispose() {
        coroutineScope.cancel()
    }

    private fun calculateProjectCounts(): CountsFolder {
        val psiManager = PsiManager.getInstance(project)
        val roots = ProjectRootManager.getInstance(project).contentRoots

        val tree = CountsFolder("")

        for (root in roots) {
            // skip root if it was already covered by an ancestor
            if (tree.pathExists(root.path)) continue

            VfsUtilCore.iterateChildrenRecursively(root, null) {
                if (!isSupportedFileType(it.fileType)) return@iterateChildrenRecursively true

                val psi = psiManager.findFile(it) ?: return@iterateChildrenRecursively true
                val counts = getCountsForPsi(psi) ?: return@iterateChildrenRecursively true

                tree.setCountsForPath(it.path, counts)

                true
            }
        }

        return tree
    }

    // returns true if listeners should be notified
    fun handleEvent(event: VFileEvent): Boolean {
        if (projectCounts == null) return false

        return when (event) {
            is VFileContentChangeEvent -> {
                val file = event.file
                if (!isSupportedFileType(file.fileType)) return false

                val oldCounts = projectCounts!!.getCountsForPath(file.path)
                val counts = getCountsForVfs(project, file)

                if (counts != oldCounts) {
                    if (counts == null) projectCounts!!.removePath(file.path)
                    else projectCounts!!.setCountsForPath(file.path, counts)
                    true
                } else false
            }

            is VFilePropertyChangeEvent -> {
                // is rename
                if (VirtualFile.PROP_NAME == event.propertyName) {
                    val oldCounts = projectCounts!!.removePath(event.oldPath)
                    val shouldNotify = oldCounts != null
                    if (!isSupportedFileType(event.file.fileType)) return shouldNotify

                    val counts = oldCounts ?: getCountsForVfs(project, event.file) ?: return false
                    projectCounts!!.setCountsForPath(event.file.path, counts)
                    true
                } else false
            }

            is VFileCreateEvent -> {
                val file = event.file ?: return false
                if (!isSupportedFileType(file.fileType)) return false
                projectCounts!!.setCountsForPath(file.path, Counts.default())
                true
            }

            is VFileCopyEvent -> {
                val counts = projectCounts!!.getCountsForPath(event.file.path) ?: return false
                projectCounts!!.setCountsForPath(event.path, counts)
                true
            }

            is VFileMoveEvent -> {
                val counts = projectCounts!!.removePath(event.oldPath) ?: return false
                projectCounts!!.setCountsForPath(event.newPath, counts)
                true
            }

            is VFileDeleteEvent -> {
                projectCounts!!.removePath(event.path) ?: return false
                true
            }

            else -> false
        }
    }

    fun getCounts(): CountsFolder {
        if (projectCounts == null) {
            ApplicationManager.getApplication().runReadAction {
                projectCounts = calculateProjectCounts()
            }

            addVfsListener()
        }

        return projectCounts!!
    }

    private fun addVfsListener() {
        project.messageBus.connect(this)
            .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
                // handle deletion events before they're deleted from the project index
                override fun before(events: List<VFileEvent>) {
                    val fileIndex = ProjectRootManager.getInstance(project).fileIndex
                    events
                        .filter {
                            it is VFileDeleteEvent && fileIndex.isInContent(it.file)
                        }
                        .any { handleEvent(it) }
                        .let { if (it) notifyListener() }
                }

                override fun after(events: List<VFileEvent>) {
                    val fileIndex = ProjectRootManager.getInstance(project).fileIndex
                    events
                        .filter {
                            val file = it.file ?: return@filter false
                            fileIndex.isInContent(file)
                        }
                        .any { handleEvent(it) }
                        .let { if (it) notifyListener() }
                }
            })
    }

    private var changeListener: (CountsFolder) -> Unit = {}

    fun setChangeListener(listener: (CountsFolder) -> Unit) {
        changeListener = listener
    }

    private fun notifyListener() {
        changeListener(projectCounts!!)
    }
}
