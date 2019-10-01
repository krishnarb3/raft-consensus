package com.rbkrishna.distributed.raft.api.state

data class VolatileStateOnLeader(
    val nextIndex: List<Int> = listOf(),
    val matchIndex: List<Int> = listOf()
)