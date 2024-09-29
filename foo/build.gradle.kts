plugins {
    id("com.anshtya.kotlin")
}

tasks.runKotlin.configure {
    mainClass = "com.anshtya.foo.FooKt"
}

dependencies {
    implementation(kotlin("stdlib:1.9.23"))
    implementation(project(":bar"))
}