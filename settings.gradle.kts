logger.lifecycle("""
## Building BlueMap ...
Java: ${System.getProperty("java.version")}
JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")})
Arch: ${System.getProperty("os.arch")} 
""")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven ("https://maven.minecraftforge.net" )
        maven ("https://maven.fabricmc.net/" )
        maven ("https://registry.npmmirror.com/-/binary/node" )
        maven ("https://maven.aliyun.com/repository/public" )
    }
}


rootProject.name = "bluemap"

includeBuild("api")

include(":core")
include(":common")

implementation("cli")
implementation("paper")
implementation("spigot")

fun implementation(name: String) {
    val project = ":$name"
    include(project)
    project(project).projectDir = file("implementations/$name")
}
