package com.rbkrishna.distributed.raft.api.log

import com.rbkrishna.distributed.raft.api.AppendEntriesArgs

interface AppendEntriesLogger {
    fun <T>logAppendEntries(appendEntries: AppendEntriesArgs<T>)
}