package com.rbkrishna.distributed.raft.api

import com.rbkrishna.distributed.raft.api.log.LogEntry
import kotlinx.serialization.Serializable

interface Args
interface Res

@Serializable
data class RequestVotesArgs(
    val termNumber: Int,
    val candidateId: Int,
    val lastLogIndex: Int,
    val lastLogTerm: Int
) : Args

@Serializable
data class RequestVotesRes(
    val termNumber: Int,
    val voteGranted: Boolean
) : Res

@Serializable
data class AppendEntriesArgs<T>(
    val termNumber: Int,
    val leaderId: Int,
    val prevLogIndex: Int,
    val prevLogTerm: Int,
    val logEntries: List<LogEntry<T>>,
    val leaderCommitIndex: Int
) : Args

@Serializable
data class AppendEntriesRes(
    val termNumber: Int,
    val result: Boolean
) : Res

@Serializable
data class Command<T>(
    val termNumber: Int,
    val command: T
)