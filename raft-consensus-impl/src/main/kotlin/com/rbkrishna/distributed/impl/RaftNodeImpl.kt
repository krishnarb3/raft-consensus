package com.rbkrishna.distributed.impl

import com.rbkrishna.distributed.raft.api.NodeImpl
import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import com.rbkrishna.distributed.raft.api.state.VolatileStateOnLeader

class RaftNodeImpl(
    persistentState: PersistentState<String>,
    nodeState: NodeState,
    volatileState: VolatileState,
    volatileStateOnLeader: VolatileStateOnLeader = VolatileStateOnLeader()
) : NodeImpl<String>(persistentState, nodeState, volatileState, volatileStateOnLeader) {


}