plugins {
    id("com.anshtya.kotlin")
}

tasks.compileKotlin.configure {
    doFirst {
        println(dependencies.joinToString("/n"))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib:1.9.23"))
}