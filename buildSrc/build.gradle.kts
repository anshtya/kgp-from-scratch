plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("myKotlin") {
            id = "com.anshtya.kotlin"
            implementationClass = "com.anshtya.kotlin.KotlinPlugin"
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.23")
}