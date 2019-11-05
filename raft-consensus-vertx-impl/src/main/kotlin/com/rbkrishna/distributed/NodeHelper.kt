package com.rbkrishna.distributed


import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions

import java.io.File
import java.io.IOException
import java.util.function.Consumer

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
object Runner {

    private val CORE_EXAMPLES_DIR = "core-examples"
    private val CORE_EXAMPLES_JAVA_DIR = "$CORE_EXAMPLES_DIR/src/main/java/"
    private val CORE_EXAMPLES_JS_DIR = "$CORE_EXAMPLES_DIR/src/main/js/"
    private val CORE_EXAMPLES_GROOVY_DIR = "$CORE_EXAMPLES_DIR/src/main/groovy/"
    private val CORE_EXAMPLES_RUBY_DIR = "$CORE_EXAMPLES_DIR/src/main/ruby/"
    private val CORE_EXAMPLES_KOTLIN_DIR = "$CORE_EXAMPLES_DIR/src/main/kotlin/"

    fun runClusteredExample(clazz: Class<*>) {
        runExample(CORE_EXAMPLES_JAVA_DIR, clazz, VertxOptions().setClustered(true), null)
    }

    fun runClusteredExample(clazz: Class<*>, options: VertxOptions) {
        runExample(CORE_EXAMPLES_JAVA_DIR, clazz, options.setClustered(true), null)
    }

    fun runExample(clazz: Class<*>) {
        runExample(CORE_EXAMPLES_JAVA_DIR, clazz, VertxOptions().setClustered(false), null)
    }

    fun runExample(clazz: Class<*>, options: DeploymentOptions) {
        runExample(CORE_EXAMPLES_JAVA_DIR, clazz, VertxOptions().setClustered(false), options)
    }

    // Kotlin

    fun runKotlinExample(scriptName: String) {
        runScriptExample(CORE_EXAMPLES_KOTLIN_DIR, scriptName, VertxOptions().setClustered(false))
    }

    fun runKotlinExampleClustered(scriptName: String) {
        runScriptExample(CORE_EXAMPLES_KOTLIN_DIR, scriptName, VertxOptions().setClustered(true))
    }

    internal object KotlinHttpSimpleServerRunner {
        @JvmStatic
        fun main(args: Array<String>) {
            Runner.runKotlinExample("io/vertx/example/core/http/simple/server.kt")
        }
    }

    internal object KotlinHttpSimpleClientRunner {
        @JvmStatic
        fun main(args: Array<String>) {
            Runner.runKotlinExample("io/vertx/example/core/http/simple/client.kt")
        }
    }

    fun runExample(exampleDir: String, clazz: Class<*>, options: VertxOptions, deploymentOptions: DeploymentOptions?) {
        runExample(exampleDir + clazz.getPackage().name.replace(".", "/"), clazz.name, options, deploymentOptions)
    }


    fun runScriptExample(prefix: String, scriptName: String, options: VertxOptions) {
        val file = File(scriptName)
        val dirPart = file.parent
        val scriptDir = prefix + dirPart
        runExample(scriptDir, scriptDir + "/" + file.name, options, null)
    }

    fun runExample(
        exampleDir: String,
        verticleID: String,
        options: VertxOptions?,
        deploymentOptions: DeploymentOptions?
    ) {
        var exampleDir = exampleDir
        var options = options
        if (options == null) {
            // Default parameter
            options = VertxOptions()
        }
        // Smart cwd detection

        // Based on the current directory (.) and the desired directory (exampleDir), we try to compute the vertx.cwd
        // directory:
        try {
            // We need to use the canonical file. Without the file name is .
            val current = File(".").canonicalFile
            if (exampleDir.startsWith(current.name) && exampleDir != current.name) {
                exampleDir = exampleDir.substring(current.name.length + 1)
            }
        } catch (e: IOException) {
            // Ignore it.
        }

        System.setProperty("vertx.cwd", exampleDir)
        val runner = { vertx: Vertx ->
            try {
                if (deploymentOptions != null) {
                    vertx.deployVerticle(verticleID, deploymentOptions)
                } else {
                    vertx.deployVerticle(verticleID)
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        if (options.isClustered) {
            Vertx.clusteredVertx(options) { res ->
                if (res.succeeded()) {
                    val vertx = res.result()
                    runner(vertx)
                } else {
                    res.cause().printStackTrace()
                }
            }
        } else {
            val vertx = Vertx.vertx(options)
            runner(vertx)
        }
    }
}