rootProject.name = "aliucord-plugins"

include(
    "CustomTags",
)

rootProject.children.forEach {
    it.projectDir = file("plugins/kotlin/${it.name}")
}