package cn.tongdun.android.plugin.excludeaar

import cn.tongdun.android.plugin.excludeaar.ExcludeClassVisitor
import cn.tongdun.android.plugin.excludeaar.PluginConfig
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * 字符串加密Transform
 * 参考https://blog.csdn.net/u011578734/article/details/114262419
 */
class ExcludeAarTransform extends Transform {
    def TAG = 'TongdunExcludeAarTransform >'

    private Project mProject
    /**
     * 扩展选项
     */
    private PluginConfig mConfig

    ExcludeAarTransform(Project project) {
        mProject = project
    }

    /**
     * 获取Transform名称
     * @return transform名称,可以在AS右侧Gradle中找到,路径app/tasks/other/transformClassWithStringObfuscateForDebug
     */
    @Override
    String getName() {
        return "ExcludeAarProcess"
    }

    /**
     * 需要处理的数据类型，有两种枚举类型
     * CLASSES 代表处理的 java 的 class 文件，RESOURCES 代表要处理 java 的资源
     * TransformManager內有多种组合
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 指 Transform 要操作内容的范围，官方文档 Scope 有 7 种类型：
     * 1. EXTERNAL_LIBRARIES        只有外部库
     * 2. PROJECT                   只有项目内容
     * 3. PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
     * 4. PROVIDED_ONLY             只提供本地或远程依赖项
     * 5. SUB_PROJECTS              只有子项目。
     * 6. SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * 7. TESTED_CODE               由当前变量(包括依赖项)测试的代码
     * TransformManager內有多种组合
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return ImmutableSet.of(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    /**
     * 是否增量编译
     * 所谓增量编译，是指当源程序的局部发生变更后进重新编译的工作只限于修改的部分及与之相关部分的内容，而不需要对全部代码进行编译
     *
     * @return false：否
     */
    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 文档：https://google.github.io/android-gradle-dsl/javadoc/3.4/
     *
     * @param transformInvocation transformInvocation
     * @throws TransformException* @throws InterruptedException* @throws IOException
     */
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        _transform(transformInvocation.getContext(), transformInvocation.getInputs(), transformInvocation.getOutputProvider(), transformInvocation.isIncremental())
    }

    /**
     * _transform
     * @param context context
     * @param collectionInput transform输入流，包含两种类型，目录格式和jar
     * @param outputProvider 是用来获取输出目录，我们要将操作后的文件复制到输出目录中。调用getContentLocation方法获取输出目录
     * @param isIncremental 是否增量编译
     * @throws IOException* @throws TransformException* @throws InterruptedException
     */
    private void _transform(Context context, Collection<TransformInput> collectionInput, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        mConfig = mProject.property(Constant.EXTENSIONS_NAME)
        if (!isIncremental) {
            //非增量,需要删除输出目录
            outputProvider.deleteAll()
        }
        if (collectionInput == null) {
            throw new IllegalArgumentException("TransformInput is null !!!")
        }
        if (outputProvider == null) {
            throw new IllegalArgumentException("TransformOutputProvider is null !!!")
        }

        // Transform的inputs有两种类型，一种是目录，一种是jar包，要分开遍历
        collectionInput.each { TransformInput transformInput ->
            //对类型为“文件夹”的input进行遍历
            transformInput.directoryInputs.each { DirectoryInput directoryInput ->
                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
                //这里执行字节码的注入，不操作字节码的话也要将输入路径拷贝到输出路径
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
            //jar
            transformInput.jarInputs.each { JarInput jarInput ->

                //jar文件一般是第三方依赖库jar文件
                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = jarInput.name
                def jarPath = jarInput.file.getAbsolutePath()
                def md5Name = DigestUtils.md5Hex(jarPath)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                //gradle7+ jarName 是40位加密字符串，可以通过path判断
                if (jarName.contains(Constant.JAR_NAME_PREFIX) || jarPath.contains(Constant.JAR_NAME_PREFIX)) {
                    println("${TAG} process jarName:${jarName}")
                    println("${TAG} process jarPath:${jarPath}")
                    handleJar(jarInput, outputProvider, jarName, md5Name)
                } else {
                    //获取输出路径下的jar包名称；+MD5是为了防止重复打包过程中输出路径名不重复，否则会被覆盖。
                    def dest = outputProvider.getContentLocation(jarName + md5Name,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    //这里执行字节码的注入，不操作字节码的话也要将输入路径拷贝到输出路径
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
        }
    }


    private void scanClass(File file) {
        try {
            ClassReader cr = new ClassReader(file.bytes)
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
            ExcludeClassVisitor sc = new ExcludeClassVisitor(Opcodes.ASM7, cw, mConfig)
            cr.accept(sc, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG)
            // 写入文件
            byte[] code = cw.toByteArray()
            FileUtils.writeByteArrayToFile(file, code)
        } catch (Exception ignored) {
            println("----scanClass error: file name " + file.getAbsolutePath() + "---")
        }
    }

    private void handleJar(JarInput jarInput, TransformOutputProvider outputProvider, String jarName, String md5Name) {
        JarFile jarFile = new JarFile(jarInput.file)
        Enumeration<JarEntry> enumeration = jarFile.entries()
        File tmpFile = new File(jarInput.file.getParent() + File.separator + "classes_temp.jar")
        //避免上次的缓存被重复插入
        if (tmpFile.exists()) {
            tmpFile.delete()
        }
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile))
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            String entryName = jarEntry.getName()
            //println("${TAG} process class entryName:${entryName}")
            ZipEntry zipEntry = new ZipEntry(entryName)
            InputStream inputStream = jarFile.getInputStream(jarEntry)
            jarOutputStream.putNextEntry(zipEntry)
            byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)
            if ("fpprog/d.class".equals(entryName)) {
                ClassReader cr = new ClassReader(sourceClassBytes)
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
                ProcessClassVisitor cv = new ProcessClassVisitor(Opcodes.ASM7, cw, mConfig)
                cr.accept(cv, ClassReader.EXPAND_FRAMES)
                // 写入文件
                byte[] code = cw.toByteArray()
                jarOutputStream.write(code)
                FileUtils.writeByteArrayToFile(new File("plugin-libs/kkk.class"),code)

            } else {
                jarOutputStream.write(sourceClassBytes)
            }
            jarOutputStream.closeEntry()
        }
        if (jarOutputStream != null) {
            jarOutputStream.close()
        }
        if (jarFile != null) {
            jarFile.close()
        }
        def dest = outputProvider.getContentLocation(jarName + md5Name,
                jarInput.contentTypes, jarInput.scopes, Format.JAR)
        if (tmpFile != null) {
            FileUtils.copyFile(tmpFile, dest)
            tmpFile.delete()
        }
    }
}
