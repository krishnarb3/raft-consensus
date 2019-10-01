package com.rbkrishna.distributed.raft.api.log

data class LogEntry<T>(
    val index: Int,
    val termNumber: Int,
    val command: T?
)