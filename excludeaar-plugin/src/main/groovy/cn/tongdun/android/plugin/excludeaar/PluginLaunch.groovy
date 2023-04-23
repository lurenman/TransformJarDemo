package cn.tongdun.android.plugin.excludeaar

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginLaunch implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println("-----------------------------------------")
        println("|                                       |")
        println("|            ExcludeaarPlugin!            |")
        println("|                                       |")
        println("-----------------------------------------")

        // 创建配置项
        project.extensions.create(Constant.EXTENSIONS_NAME, PluginConfig)

        BaseExtension ext = project.getExtensions().findByType(BaseExtension.class)
        ext?.with {
            it.registerTransform(new ExcludeAarTransform(project));
        }
    }

}
