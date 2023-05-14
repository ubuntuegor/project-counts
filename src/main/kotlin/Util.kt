package to.bnt.plugin.counts

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType

// to my understanding separators are normalized across OSes in VFS
const val PATH_SEPARATOR = "/"

fun splitPath(path: String) = path.split(PATH_SEPARATOR)

fun isSupportedFileType(ft: FileType) = ft is JavaFileType || ft is KotlinFileType

fun getCountsForPsi(psi: PsiFile): Counts? {
    var classes = 0
    var functions = 0

    when (psi) {
        is PsiJavaFile -> {
            psi.forEachDescendantOfType<PsiClass> { classes++ }
            psi.forEachDescendantOfType<PsiMethod> { functions++ }
        }

        is KtFile -> {
            psi.forEachDescendantOfType<KtClass> { classes++ }
            psi.forEachDescendantOfType<KtNamedFunction> { functions++ }
        }

        else -> return null
    }

    return Counts(classes, functions)
}

fun getCountsForVfs(project: Project, file: VirtualFile): Counts? {
    val psi = file.toPsiFile(project) ?: return null
    return getCountsForPsi(psi)
}
