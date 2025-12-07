//
// build.gradle in TeamCode
//

import java.time.Year
// Most of the definitions for building your module reside in a common, shared
// file 'build.common.gradle'. Being factored in this way makes it easier to
// integrate updates to the FTC into your code. If you really need to customize
// the build definitions, you can place those customizations in this file, but
// please think carefully as to whether such customizations are really necessary
// before doing so.


// Custom definitions may go here

// ktlint configuration - plugins block must come before apply statements
plugins {
    id("com.android.application")
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.dokka)
}

// Include common definitions from above.
apply { from("../build.common.gradle") }

// Dokka configuration
dokka {
    dokkaPublications.html {
        // Customize colors and styling
        moduleName.set("Pioneer Robotics Documentation")
        
        pluginsConfiguration.html {
            // Custom CSS for colors
            // customStyleSheets.from("dokka-styles.css")
            
            // Footer text
            footerMessage.set("© ${Year.now().value} Pioneer Robotics")
        }
    }
}

ktlint {
    android = true
    ignoreFailures = true  // Allow build to succeed, but still show warnings
    
    // Disable ktlint check tasks to prevent build failures
    filter {
        exclude("**/build/**")
    }
}

android {
    namespace = "pioneer"
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":FtcRobotController"))
    implementation(libs.ftc.inspection)
    implementation(libs.ftc.blocks)
    implementation(libs.ftc.robotcore)
    implementation(libs.ftc.robotserver)
    implementation(libs.ftc.onbotjava)
    implementation(libs.ftc.hardware)
    implementation(libs.ftc.common)
    implementation(libs.ftc.vision)
    implementation(libs.androidx.appcompat)
    implementation(libs.acmerobotics.dashboard)
    testImplementation(libs.junit)
}

//kotlin {
//    jvmToolchain(8)
//}

tasks.register("dokkaHtmlMultiModule") {
    dependsOn(":TeamCode:dokkaGeneratePublicationHtml")
    doLast {
        copy {
            from(layout.buildDirectory.dir("dokka/html"))
            into(layout.projectDirectory.dir("../docs"))
        }
    }
}