package dev.drzepka.wikilinks.app.db.infrastructure

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement

class CloseableDriver(private val delegate: SqlDriver) : SqlDriver {
    private var closed = false

    override fun close() {
        if (closed)
            return

        val wasClosed = nativeClose(delegate)
        if (!wasClosed)
            delegate.close()

        closed = true
    }

    override fun currentTransaction(): Transacter.Transaction? {
        return getDelegate().currentTransaction()
    }

    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?) =
        getDelegate().execute(identifier, sql, parameters, binders)

    override fun executeQuery(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): SqlCursor = getDelegate().executeQuery(identifier, sql, parameters, binders)

    override fun newTransaction(): Transacter.Transaction {
        val thisDelegate = getDelegate()
        val transaction = thisDelegate.newTransaction()

        // For some reason, Jdbc driver doesn't close the connection after completing a transaction,
        // which prevents from moving database file in one of the last generator stages.
        transaction.afterCommit { nativeClose(thisDelegate) }
        transaction.afterRollback { nativeClose(thisDelegate) }

        return transaction
    }

    private fun getDelegate(): SqlDriver {
        if (closed)
            throw IllegalStateException("Driver has already been closed")
        return delegate
    }
}
expect fun nativeClose(driver: SqlDriver): Boolean
