package com.rbkrishna.distributed.raft.api.state

data class VolatileState(
    val commitIndex: Int = 0,
    val lastApplied: Int = 0
)