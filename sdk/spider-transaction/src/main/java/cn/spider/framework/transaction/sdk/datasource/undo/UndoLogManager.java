/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package cn.spider.framework.transaction.sdk.datasource.undo;

import cn.spider.framework.transaction.sdk.core.exception.TransactionException;
import cn.spider.framework.transaction.sdk.datasource.ConnectionProxy;
import cn.spider.framework.transaction.sdk.datasource.DataSourceProxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;

/**
 * The type Undo log manager.
 *
 * @author DDS
 * @author Geng Zhang
 */
public interface UndoLogManager {

    /**
     * Flush undo logs.
     * @param cp the cp
     * @throws SQLException the sql exception
     */
    void flushUndoLogs(ConnectionProxy cp) throws SQLException;

    /**
     * Undo.
     *
     * @param dataSourceProxy the data source proxy
     * @param xid             the xid
     * @param branchId        the branch id
     * @throws TransactionException the transaction exception
     */
    void undo(DataSourceProxy dataSourceProxy, String xid, long branchId,Connection conn) throws TransactionException;

    /**
     * Delete undo log.
     *
     * @param xid      the xid
     * @param branchId the branch id
     * @param conn     the conn
     * @throws SQLException the sql exception
     */
    void deleteUndoLog(String xid, long branchId, Connection conn) throws SQLException;

    /**
     * batch Delete undo log.
     *
     * @param xids the xid set collections
     * @param branchIds the branch id set collections
     * @param conn the connection
     * @throws SQLException the sql exception
     */
    void batchDeleteUndoLog(Set<String> xids, Set<Long> branchIds, Connection conn) throws SQLException;

    /**
     * delete undolog by created
     * @param logCreated the created time
     * @param limitRows the limit rows
     * @param conn the connection
     * @return the update rows
     * @throws SQLException the sql exception
     */
    int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException;

    BranchUndoLog selectBranchUndoLog(String xid, long branchId, Connection conn) throws SQLException;


    Boolean checkUndoLogExist(String xid, long branchId, Connection conn) throws SQLException;

}
