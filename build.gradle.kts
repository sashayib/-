import org.gradle.api.tasks.Exec

tasks.register<Exec>("npmLint") {
    commandLine("npm", "run", "lint")
}

tasks.register<Exec>("npmBuild") {
    commandLine("npm", "run", "build")
}

tasks.register("assembleDebug") {
    dependsOn("npmBuild")
}

tasks.register("assembleRelease") {
    dependsOn("npmBuild")
}

tasks.register("build") {
    dependsOn("npmLint", "npmBuild")
}

tasks.register("lint") {
    dependsOn("npmLint")
}

tasks.register("lintDebug") {
    dependsOn("npmLint")
}

tasks.register("check") {
    dependsOn("npmLint")
}
