package dev.drzepka.wikilinks.app.db

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement

/**
 * Allows the driver to be reused after closing the connection. Closing behavior differs between platforms:
 * * under JVM (JdbcSqliteDriver), closing the connection has no effect, as connection is established on per-connection basis,
 * * under Native, closing the connection works as expected.
 */
class ReusableDriver(private val factory: () -> SqlDriver) : SqlDriver {
    private var delegate: SqlDriver? = null

    override fun close() {
        delegate?.close()
        delegate = null
    }

    override fun currentTransaction(): Transacter.Transaction? {
        return getOrCreateDelegate().currentTransaction()
    }

    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?) =
        getOrCreateDelegate().execute(identifier, sql, parameters, binders)

    override fun executeQuery(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): SqlCursor = getOrCreateDelegate().executeQuery(identifier, sql, parameters, binders)

    override fun newTransaction(): Transacter.Transaction = getOrCreateDelegate().newTransaction()

    private fun getOrCreateDelegate(): SqlDriver {
        if (delegate == null)
            delegate = factory()
        return delegate!!
    }
}
