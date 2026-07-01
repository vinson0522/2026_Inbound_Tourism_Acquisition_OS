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
import java.util.Map;

/**
 * PostgreSQL JSONB ↔ {@code List<Map<String, Object>>} (e.g. storyboard_json).
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgJsonbListMapTypeHandler extends BaseTypeHandler<List<Map<String, Object>>> {

    private static final TypeReference<List<Map<String, Object>>> TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(
        PreparedStatement ps,
        int i,
        List<Map<String, Object>> parameter,
        JdbcType jdbcType
    ) throws SQLException {
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(JsonUtils.toJsonString(parameter == null ? Collections.emptyList() : parameter));
        ps.setObject(i, json);
    }

    @Override
    public List<Map<String, Object>> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Map<String, Object>> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Map<String, Object>> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<Map<String, Object>> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> parsed = JsonUtils.parseObject(raw, TYPE);
        return parsed != null ? parsed : Collections.emptyList();
    }
}
