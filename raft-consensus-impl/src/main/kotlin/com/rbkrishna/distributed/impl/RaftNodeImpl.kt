package com.rbkrishna.distributed.impl

import com.rbkrishna.distributed.raft.api.*
import com.rbkrishna.distributed.raft.api.state.NodeState
import com.rbkrishna.distributed.raft.api.state.PersistentState
import com.rbkrishna.distributed.raft.api.state.VolatileState
import com.rbkrishna.distributed.raft.api.state.VolatileStateOnLeader

class RaftNodeImpl(
    persistentState: PersistentState<String>,
    nodeState: NodeState,
    volatileState: VolatileState,
    volatileStateOnLeader: VolatileStateOnLeader = VolatileStateOnLeader(),
    val raftCluster: RaftCluster
) : BaseNode<String>(persistentState, nodeState, volatileState, volatileStateOnLeader) {

    override fun sendHandleRequestVotes(nodeId: Int, requestVotesArgs: RequestVotesArgs): RequestVotesRes {
        val node = raftCluster.nodeMap[nodeId]
        return node?.handleRequestVotes(requestVotesArgs) ?: RequestVotesRes(requestVotesArgs.termNumber, false)
    }

    override fun sendHandleAppendEntries(nodeId: Int, appendEntriesArgs: AppendEntriesArgs<String>): AppendEntriesRes {
        val node = raftCluster.nodeMap[nodeId]
        return node?.handleAppendEntries(appendEntriesArgs) ?: AppendEntriesRes(appendEntriesArgs.termNumber, false)
    }

    override fun sendJoinNotification(sourceNodeId: Int) {
        val nodes = raftCluster.nodeMap.values
        nodes.forEach { node -> node.handleJoinNotification(sourceNodeId) }
    }

    override fun sendQuitNotification(sourceNodeId: Int) {
        val nodes = raftCluster.nodeMap.values
        nodes.forEach { node -> node.handleQuitNotification(sourceNodeId) }
    }
}