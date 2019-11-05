package com.rbkrishna.distributed.raft.api.log

import kotlinx.serialization.Serializable

@Serializable
data class LogEntry<T>(
    val index: Int,
    val termNumber: Int,
    val command: T?
)