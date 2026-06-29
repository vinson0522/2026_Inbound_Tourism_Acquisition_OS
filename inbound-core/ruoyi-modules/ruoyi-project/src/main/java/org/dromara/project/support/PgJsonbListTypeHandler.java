package org.dromara.project.support;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.dromara.common.json.utils.JsonUtils;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * PostgreSQL JSONB ↔ {@code List<String>} for customer_project JSON columns.
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgJsonbListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final TypeReference<List<String>> TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
        throws SQLException {
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(JsonUtils.toJsonString(parameter));
        ps.setObject(i, json);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return JsonUtils.parseObject(raw, TYPE);
    }
}
