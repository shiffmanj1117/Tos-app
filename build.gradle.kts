tasks.register<Exec>("assembleDebug") {
    commandLine("npm", "run", "build")
}

tasks.register<Exec>("assembleRelease") {
    commandLine("npm", "run", "build")
}
