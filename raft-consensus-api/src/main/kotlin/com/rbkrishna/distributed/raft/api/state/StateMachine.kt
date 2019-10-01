package com.rbkrishna.distributed.raft.api.state

import com.rbkrishna.distributed.raft.api.Command

interface StateMachine {
    fun <T> apply(command: Command<T>)
}