package org.dromara.project.support;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL ENUM {@code subscription_status} ↔ {@link String}.
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgSubscriptionStatusTypeHandler extends BaseTypeHandler<String> {

    private static final String PG_TYPE = "subscription_status";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
        throws SQLException {
        PGobject value = new PGobject();
        value.setType(PG_TYPE);
        value.setValue(parameter);
        ps.setObject(i, value);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
