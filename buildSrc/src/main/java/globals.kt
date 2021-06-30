import libraries.androidTestRulesSpec
import libraries.androidTestRunnerSpec
import libraries.espressoCoreSpec
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

private fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? =
        add("androidTestImplementation", dependencyNotation)


fun DependencyHandlerScope.useEspresso(project: Project) {
    with(project) {
        androidTestImplementation("androidx.test.ext:junit:1.0.0")
        androidTestImplementation(androidTestRunnerSpec)
        androidTestImplementation(androidTestRulesSpec)
        androidTestImplementation(espressoCoreSpec)
    }
}

fun Project.projectRepositories() {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    }
}
