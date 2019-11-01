package com.rbkrishna.distributed.impl

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class RaftApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(arrayOf(RaftApplication::class.java), args)
        }
    }
}