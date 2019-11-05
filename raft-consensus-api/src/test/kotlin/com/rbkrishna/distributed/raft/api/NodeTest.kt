package com.rbkrishna.distributed.raft.api

import com.rbkrishna.distributed.raft.api.heartbeat.HeartbeatEventSchedulerTimerImpl
import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import com.rbkrishna.distributed.raft.api.state.VolatileStateOnLeader
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class NodeTest {
    @Test
    fun testNode() {
        val node = BaseNode<String>(
            PersistentState(1, 0, 0, listOf()),
            NodeState.FOLLOWER,
            VolatileState(0, 0),
            VolatileStateOnLeader(listOf(), listOf())
        )
        val heartbeatEventScheduler = HeartbeatEventSchedulerTimerImpl(1000)
        heartbeatEventScheduler.attachClient(node)
        assertNotNull(node)
    }
}