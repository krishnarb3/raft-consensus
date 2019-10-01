package com.rbkrishna.distributed.raft.api.state

import com.rbkrishna.distributed.raft.api.log.LogEntry

data class PersistentState<T>(
    val id: Int,
    val currentTerm: Int = 0,
    val votedFor: Int? = null,
    val logEntries: List<LogEntry<T>> = listOf()
)