package com.rbkrishna.distributed.raft.api.state

data class WrongStateException(
    override val message: String
) : Exception(message)