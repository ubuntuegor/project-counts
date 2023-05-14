import com.intellij.openapi.components.service
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.VfsTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import to.bnt.plugin.counts.*
import java.io.File

class PluginModuleTest : BasePlatformTestCase() {
    fun testService() {
        myFixture.copyDirectoryToProject("initial", ".")

        val service = project.service<CountsService>()

        // test that counts are properly built
        run {
            val counts = service.getCounts()

            TestCase.assertEquals(Counts(3, 9), counts.getCountsForPath("/src/main.java"))
            TestCase.assertEquals(Counts(2, 6), counts.getCountsForPath("/src/kotlin/example.kt"))
            TestCase.assertEquals(Counts(0, 0), counts.getCountsForPath("/src/kotlin/empty.kt"))

            TestCase.assertFalse(counts.pathExists("/src/garbage/file.txt"))
            TestCase.assertFalse(counts.pathExists("/src/garbage/something.py"))
        }

        // test that new files get added to counts
        run {
            myFixture.copyFileToProject("additional/addedFile.kt", "./addedFile.kt")
            val counts = service.getCounts()

            TestCase.assertEquals(Counts(0, 3), counts.getCountsForPath("/src/addedFile.kt"))
        }

        // test that removed files disappear from counts
        run {
            val fileToDelete =
                FilenameIndex.getVirtualFilesByName("main.java", GlobalSearchScope.projectScope(project)).first()
            VfsTestUtil.deleteFile(fileToDelete)
            val counts = service.getCounts()

            TestCase.assertFalse(counts.pathExists("/src/main.java"))
        }
    }

    override fun getTestDataPath(): String {
        return File("./src/test/resources/testdata").absolutePath
    }
}
