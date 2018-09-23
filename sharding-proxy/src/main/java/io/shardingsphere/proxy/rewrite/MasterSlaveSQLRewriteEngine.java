/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.proxy.rewrite;

import io.shardingsphere.core.metadata.ShardingMetaData;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.parsing.parser.token.SQLToken;
import io.shardingsphere.core.parsing.parser.token.SchemaToken;
import io.shardingsphere.core.rewrite.SQLBuilder;
import io.shardingsphere.core.rewrite.placeholder.SchemaPlaceholder;
import io.shardingsphere.core.rule.MasterSlaveRule;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * SQL rewrite engine for master slave rule.
 * 
 * <p>should rewrite schema name.</p>
 * 
 * @author chenqingyang
 */
public final class MasterSlaveSQLRewriteEngine {
    
    private final MasterSlaveRule masterSlaveRule;
    
    private final String originalSQL;
    
    private final List<SQLToken> sqlTokens = new LinkedList<>();
    
    private final SQLStatement sqlStatement;
    
    private final ShardingMetaData metaData;
    
    /**
     * Constructs master slave SQL rewrite engine.
     * 
     * @param masterSlaveRule master slave rule
     * @param originalSQL original SQL
     * @param sqlStatement sql statement
     * @param metaData meta data
     */
    public MasterSlaveSQLRewriteEngine(final MasterSlaveRule masterSlaveRule, final String originalSQL, final SQLStatement sqlStatement, final ShardingMetaData metaData) {
        this.masterSlaveRule = masterSlaveRule;
        this.originalSQL = originalSQL;
        this.sqlStatement = sqlStatement;
        this.metaData = metaData;
        sqlTokens.addAll(sqlStatement.getSqlTokens());
    }
    
    /**
     * Rewrite SQL.
     * 
     * @return SQL
     */
    public String rewrite() {
        if (sqlStatement.getSqlTokens().isEmpty()) {
            return originalSQL;
        }
        SQLBuilder result = new SQLBuilder(null);
        int count = 0;
        sortByBeginPosition();
        for (SQLToken each : sqlStatement.getSqlTokens()) {
            if (0 == count) {
                result.appendLiterals(originalSQL.substring(0, each.getBeginPosition()));
            }
            if (each instanceof SchemaToken) {
                appendSchemaPlaceholder(originalSQL, result, (SchemaToken) each, count, sqlStatement.getSqlTokens());
            }
            count++;
        }
        return result.toSQL(masterSlaveRule, metaData.getDataSource());
    }
    
    private void appendSchemaPlaceholder(final String sql, final SQLBuilder sqlBuilder, final SchemaToken schemaToken, final int count, final List<SQLToken> sqlTokens) {
        sqlBuilder.appendPlaceholder(new SchemaPlaceholder(schemaToken.getSchemaName().toLowerCase(), null));
        int beginPosition = schemaToken.getBeginPosition() + schemaToken.getOriginalLiterals().length();
        int endPosition = sqlTokens.size() - 1 == count ? sql.length() : sqlTokens.get(count + 1).getBeginPosition();
        sqlBuilder.appendLiterals(sql.substring(beginPosition, endPosition));
    }
    
    private void sortByBeginPosition() {
        Collections.sort(sqlTokens, new Comparator<SQLToken>() {
            
            @Override
            public int compare(final SQLToken o1, final SQLToken o2) {
                return o1.getBeginPosition() - o2.getBeginPosition();
            }
        });
    }
    
}
