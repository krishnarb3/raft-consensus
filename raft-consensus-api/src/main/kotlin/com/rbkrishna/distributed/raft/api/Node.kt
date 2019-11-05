package com.rbkrishna.distributed.raft.api

import com.rbkrishna.distributed.raft.api.heartbeat.HeartbeatEventClient

interface Node<T> : HeartbeatEventClient {
    fun sendRequestVotes(requestVotesArgs: RequestVotesArgs)
    fun handleRequestVotes(requestVotesArgs: RequestVotesArgs): RequestVotesRes

    fun sendAppendEntries(appendEntriesArgs: AppendEntriesArgs<T>)
    fun handleAppendEntries(appendEntriesArgs: AppendEntriesArgs<T>): AppendEntriesRes

    fun onCommandReceived(command: Command<T>)

    fun handleJoinNotification(sourceNodeId: Int)
    fun handleQuitNotification(sourceNodeId: Int)
}