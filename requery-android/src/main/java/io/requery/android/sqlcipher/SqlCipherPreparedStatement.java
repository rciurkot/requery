/*
 * Copyright 2016 requery.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.requery.android.sqlcipher;

import android.database.Cursor;
import io.requery.android.sqlite.BasePreparedStatement;
import io.requery.android.sqlite.CursorResultSet;
import io.requery.android.sqlite.SingleResultSet;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteStatement;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link java.sql.PreparedStatement} implementation using Android's local SQLite database.
 */
class SqlCipherPreparedStatement extends BasePreparedStatement {

    private final SqlCipherConnection connection;
    private final SQLiteStatement statement;
    private Cursor cursor;

    SqlCipherPreparedStatement(SqlCipherConnection connection, String sql,
                               int autoGeneratedKeys) throws SQLException {
        super(connection, sql, autoGeneratedKeys);
        this.connection = connection;
        statement = connection.getDatabase().compileStatement(sql);
    }

    @Override
    protected void bindNullOrString(int index, Object value) {
        if (value == null) {
            statement.bindNull(index);
            if (bindings != null) {
                bindings.add(null);
            }
        } else {
            String string = value.toString();
            statement.bindString(index, string);
            if (bindings != null) {
                bindings.add(string);
            }
        }
    }

    @Override
    protected void bindLong(int index, long value) {
        statement.bindLong(index, value);
        if (bindings != null) {
            bindings.add(value);
        }
    }

    @Override
    protected void bindDouble(int index, double value) {
        statement.bindDouble(index, value);
        if (bindings != null) {
            bindings.add(value);
        }
    }

    @Override
    protected void bindBlob(int index, byte[] value) {
        if (value == null) {
            statement.bindNull(index);
            if (bindings != null) {
                bindings.add(null);
            }
        } else {
            statement.bindBlob(index, value);
            if (bindings != null) {
                bindBlobLiteral(index, value);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        clearParameters();
        statement.close();
        if (cursor != null) {
            cursor.close();
        }
        super.close();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearParameters() throws SQLException {
        statement.clearBindings();
        if (bindings != null) {
            bindings.clear();
        }
    }

    @Override
    public boolean execute() throws SQLException {
        try {
            statement.execute();
        } catch (SQLiteException e) {
            SqlCipherConnection.throwSQLException(e);
        }
        return false;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        try {
            String[] args = bindingsToArray();
            cursor = connection.getDatabase().rawQuery(getSql(), args);
            return queryResult = new CursorResultSet(this, cursor, false);
        } catch (SQLiteException e) {
            SqlCipherConnection.throwSQLException(e);
        }
        return null;
    }

    @Override
    public int executeUpdate() throws SQLException {
        if (autoGeneratedKeys == RETURN_GENERATED_KEYS) {
            try {
                long rowId = statement.executeInsert();
                insertResult = new SingleResultSet(this, rowId);
                updateCount = 1;
            } catch (SQLiteException e) {
                SqlCipherConnection.throwSQLException(e);
            }
        } else {
            try {
                updateCount = statement.executeUpdateDelete();
            } catch (SQLiteException e) {
                SqlCipherConnection.throwSQLException(e);
            }
        }
        return updateCount;
    }
}
