package com.rbkrishna.distributed.raft.api

import com.rbkrishna.distributed.raft.api.heartbeat.HeartbeatEventScheduler
import com.rbkrishna.distributed.raft.api.log.LogEntry
import com.rbkrishna.distributed.raft.api.state.*
import org.slf4j.LoggerFactory
import kotlin.math.min

open class NodeImpl<T>(
    var persistentState: PersistentState<T>,
    var nodeState: NodeState,
    var volatileState: VolatileState,
    var volatileStateOnLeader: VolatileStateOnLeader = VolatileStateOnLeader()
) : Node<T> {

    lateinit var heartbeatEventScheduler: HeartbeatEventScheduler
    var nodeList = mutableListOf<Node<T>>(this)
    var receivedAppendEntriesFromLeader = false

    private lateinit var stateMachine: StateMachine

    private val logger = LoggerFactory.getLogger(NodeImpl::class.java)

    @Synchronized
    override fun sendRequestVotes(requestVotesArgs: RequestVotesArgs) {
        if (nodeState == NodeState.CANDIDATE) {
            val numberOfNodes = nodeList.size
            val numberOfVotes = nodeList
                .map { it.handleRequestVotes(requestVotesArgs) }
                .filter { it.voteGranted }.size

            if (numberOfVotes > numberOfNodes.toDouble() / 2F) {
                logger.info("${persistentState.id} became leader")
                nodeState = NodeState.LEADER
                persistentState = persistentState.copy(currentTerm = persistentState.currentTerm + 1)
            } else {
                nodeState = NodeState.FOLLOWER
            }
        } else {
            throw WrongStateException("Only candidate can call sendRequestVotes, called by $nodeState")
        }
    }

    @Synchronized
    override fun handleRequestVotes(requestVotesArgs: RequestVotesArgs): RequestVotesRes {
        if (nodeState == NodeState.LEADER
            || requestVotesArgs.termNumber < persistentState.currentTerm) {
            return RequestVotesRes(persistentState.currentTerm, false)
        }
        if (requestVotesArgs.candidateId == persistentState.id) {
            persistentState = persistentState.copy(votedFor = requestVotesArgs.candidateId)
            return RequestVotesRes(requestVotesArgs.termNumber, true)
        }
        if (requestVotesArgs.termNumber > persistentState.currentTerm) {
            persistentState = persistentState.copy(
                currentTerm = requestVotesArgs.termNumber,
                votedFor = requestVotesArgs.candidateId
            )
            nodeState = NodeState.FOLLOWER
            logger.info("${persistentState.id} voted for ${requestVotesArgs.candidateId}")
            return RequestVotesRes(requestVotesArgs.termNumber, true)
        }
        if ((persistentState.votedFor == null || persistentState.votedFor == requestVotesArgs.candidateId)
            && logUpToDate(requestVotesArgs)
        ) {
            persistentState = persistentState.copy(
                currentTerm = requestVotesArgs.termNumber,
                votedFor = requestVotesArgs.candidateId
            )
            logger.info("${persistentState.id} voted for ${requestVotesArgs.candidateId}")
            return RequestVotesRes(requestVotesArgs.termNumber, true)
        }
        return RequestVotesRes(requestVotesArgs.termNumber, false)
    }

    @Synchronized
    override fun sendAppendEntries(appendEntriesArgs: AppendEntriesArgs<T>) {
        if (nodeState == NodeState.LEADER) {
            logger.info("${persistentState.id} sendAppendEntries")
            var appendEntriesResults = listOf<AppendEntriesRes>()
            if (appendEntriesArgs.logEntries.isEmpty()) {
                // Send heartbeat to all nodes
                nodeList.filter { it != this }.forEach { node ->
                    appendEntriesResults = appendEntriesResults + node.handleAppendEntries(appendEntriesArgs)
                }
            } else {
                nodeList.filter { it != this }.forEachIndexed { index, node ->
                    val appendEntriesRes = sendAppendEntriesStartingAtNextIndex(
                        appendEntriesArgs,
                        volatileStateOnLeader.nextIndex[index],
                        index,
                        node
                    )
                    if (appendEntriesRes.termNumber > persistentState.currentTerm) {
                        persistentState = persistentState.copy(currentTerm = appendEntriesRes.termNumber)
                        nodeState = NodeState.FOLLOWER
                    } else {
                        for (n in volatileState.commitIndex until persistentState.logEntries.size) {
                            if (persistentState.logEntries[n].termNumber == persistentState.currentTerm
                                && volatileStateOnLeader.matchIndex.isQuorum { it >= n }
                            ) {
                                volatileState = volatileState.copy(commitIndex = n)
                            }
                        }
                    }
                }
            }
            logger.info("${persistentState.id} sendAppendEntries finished")
        } else {
            throw Exception("sendAppendEntries called by non leader")
        }
    }

    @Synchronized
    override fun handleAppendEntries(appendEntriesArgs: AppendEntriesArgs<T>): AppendEntriesRes {
        if (nodeState != NodeState.LEADER) {
            logger.info("${persistentState.id} handleAppendEntries")
            if (appendEntriesArgs.termNumber < persistentState.currentTerm) {
                return AppendEntriesRes(persistentState.currentTerm, false)
            } else if (!isEntryAt(
                    persistentState.logEntries,
                    appendEntriesArgs.prevLogIndex,
                    appendEntriesArgs.prevLogTerm
                )
            ) {
                return AppendEntriesRes(appendEntriesArgs.termNumber, false)
            } else {
                val resultLogs = appendNewEntries(appendEntriesArgs.logEntries)
                if (appendEntriesArgs.leaderCommitIndex > volatileState.commitIndex) {
                    volatileState = if (appendEntriesArgs.leaderCommitIndex > volatileState.lastApplied) {
                        VolatileState(
                            commitIndex = min(appendEntriesArgs.leaderCommitIndex, resultLogs.last().index),
                            lastApplied = volatileState.lastApplied + 1
                        ).also {
                            stateMachine.apply(
                                Command(
                                    appendEntriesArgs.termNumber,
                                    appendEntriesArgs.logEntries[volatileState.lastApplied].command
                                )
                            )
                        }
                    } else {
                        VolatileState(
                            commitIndex = min(appendEntriesArgs.leaderCommitIndex, resultLogs.last().index),
                            lastApplied = volatileState.lastApplied
                        )
                    }
                }
            }
            receivedAppendEntriesFromLeader = true
            heartbeatEventScheduler.refreshHeartbeat()
            persistentState = persistentState.copy(currentTerm = appendEntriesArgs.termNumber)
            return AppendEntriesRes(persistentState.currentTerm, true)
        } else {
            throw WrongStateException("Leader received handleAppendEntries")
        }
    }

    override fun handleHeartbeatEvent() {
        logger.info("${persistentState.id} handleHeartbeat, receivedAppendEntries: $receivedAppendEntriesFromLeader")
        if (nodeState == NodeState.LEADER) {
            val lastLogEntry = persistentState.logEntries.lastOrNull()
            sendAppendEntries(
                AppendEntriesArgs(
                    termNumber = persistentState.currentTerm,
                    leaderId = persistentState.id,
                    prevLogIndex = lastLogEntry?.index ?: 0,
                    prevLogTerm = lastLogEntry?.termNumber ?: 0,
                    logEntries = listOf(),
                    leaderCommitIndex = volatileState.commitIndex
                )
            )
        } else {
            if (!receivedAppendEntriesFromLeader && persistentState.votedFor == null) {
                receivedAppendEntriesFromLeader = false
                startElection()
            } else {
                receivedAppendEntriesFromLeader = false
            }
        }
    }

    @Synchronized
    override fun onCommandReceived(command: Command<T>) {
        if (command.termNumber > persistentState.currentTerm) {
            nodeState = NodeState.FOLLOWER
        } else {
            val logEntries = appendNewEntries(
                listOf(
                    LogEntry(
                        volatileState.commitIndex + 1,
                        persistentState.currentTerm,
                        command.command
                    )
                )
            )
            sendAppendEntries(
                AppendEntriesArgs(
                    persistentState.currentTerm + 1,
                    persistentState.id,
                    persistentState.logEntries.last().index,
                    persistentState.logEntries.last().termNumber,
                    logEntries,
                    volatileState.commitIndex
                )
            )
        }
    }

    @Synchronized
    override fun handleJoinNotification(sourceNode: Node<T>) {
        if (sourceNode != this) {
            nodeList.add(sourceNode)
            volatileStateOnLeader =
                volatileStateOnLeader.copy(
                    nextIndex = volatileStateOnLeader.nextIndex + this.lastIndex(persistentState.logEntries) + 1
                )
        }
    }

    @Synchronized
    override fun handleQuitNotification(sourceNode: Node<T>) {
        nodeList.remove(sourceNode)
    }

    private fun startElection() {
        logger.info("${persistentState.id} started election")
        nodeState = NodeState.CANDIDATE
        sendRequestVotes(
            RequestVotesArgs(
                persistentState.currentTerm + 1,
                persistentState.id,
                persistentState.logEntries.lastOrNull()?.index ?: 0,
                persistentState.logEntries.lastOrNull()?.termNumber ?: 0
            )
        )
    }

    private fun sendAppendEntriesStartingAtNextIndex(
        appendEntriesArgs: AppendEntriesArgs<T>,
        nextIndex: Int,
        nodeIndex: Int,
        node: Node<T>
    ): AppendEntriesRes {
        if (nextIndex >= 0) {
            val newAppendEntriesArgs = appendEntriesArgs.copy(
                logEntries = appendEntriesArgs.logEntries.subList(nextIndex, appendEntriesArgs.logEntries.size)
            )
            val appendEntriesRes = node.handleAppendEntries(newAppendEntriesArgs)
            return if (appendEntriesRes.result) {
                val newNextIndex = volatileStateOnLeader.nextIndex.mapIndexed { index, value ->
                    if (index == nodeIndex) nextIndex
                    else value
                }
                val matchIndex = volatileStateOnLeader.matchIndex.mapIndexed { index, value ->
                    if (index == nodeIndex) appendEntriesArgs.prevLogIndex + 1
                    else value
                }
                volatileStateOnLeader = VolatileStateOnLeader(newNextIndex, matchIndex)
                appendEntriesRes
            } else {
                sendAppendEntriesStartingAtNextIndex(appendEntriesArgs, nextIndex - 1, nodeIndex, node)
            }
        } else {
            throw Exception("nextIndex passed to sendAppendEntries < 0")
        }
    }


    private fun isEntryAt(logEntries: List<LogEntry<T>>, prevLogIndex: Int, prevLogTerm: Int): Boolean {
        // TODO: Update this to a better implementation
        return true
//        return logEntries.size > prevLogIndex && logEntries[prevLogIndex].termNumber == prevLogTerm
    }

    private fun appendNewEntries(logEntries: List<LogEntry<T>>): List<LogEntry<T>> {
        val result = mutableListOf<LogEntry<T>>()
        val len = logEntries.size
        for (i in 0 until len) {
            if (logEntries[i] == persistentState.logEntries[i]) {
                result.add(logEntries[i])
            }
        }
        if (len > 0) {
            for (i in len..persistentState.logEntries.size) {
                result.add(persistentState.logEntries[i])
            }
        }
        return result
    }

    private fun logUpToDate(requestVotesArgs: RequestVotesArgs): Boolean {
        val receiverLogIndex = requestVotesArgs.lastLogIndex
        val candidateLogIndex = lastIndex(persistentState.logEntries)
        if (receiverLogIndex == candidateLogIndex &&
            requestVotesArgs.termNumber == persistentState.currentTerm
        ) {
            return true
        } else if (candidateLogIndex > receiverLogIndex && requestVotesArgs.termNumber < persistentState.currentTerm) {
            return true
        }
        return false
    }

    private fun lastIndex(logEntries: List<LogEntry<T>>) = if (logEntries.isNotEmpty()) {
        logEntries.lastIndex
    } else {
        0
    }

    private fun <U> List<U>.isQuorum(predicate: (U) -> Boolean): Boolean =
        this.filter(predicate).count() > this.size / 2

    override fun toString(): String {
        return "persistentState: $persistentState" +
                ", nodeState: $nodeState" +
                ", volatileState: $volatileState" +
                ", volatileStateOnLeader: $volatileStateOnLeader"
    }
}