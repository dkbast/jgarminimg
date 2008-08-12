package org.free.garminimg.export;

import org.free.garminimg.utils.ImgConstants;
import org.pvalsecc.jdbc.JdbcUtilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class StyleExporter extends BaseExporter {
    public StyleExporter(String[] argv) throws IllegalAccessException {
        getOptions(argv);
    }

    public static void main(String[] args) throws IllegalAccessException, SQLException {
        StyleExporter exporter = new StyleExporter(args);
        exporter.run();
    }

    private void run() throws SQLException {
        Connection con = getConnection();

        insertPointTypes(con);

        con.commit();
        con.close();
    }

    private void insertPointTypes(Connection con) throws SQLException {
        Map<Integer, ImgConstants.PointDescription> pointTypes = ImgConstants.getPointTypes();
        JdbcUtilities.runInsertQuery("Inserting point types",
                "INSERT INTO POI_TYPE (type, sub_type, description, icon) VALUES (?,?,?,?)",
                con, pointTypes.entrySet(), 500,
            new JdbcUtilities.InsertTask<Map.Entry<Integer, ImgConstants.PointDescription>>() {
                public boolean marshall(PreparedStatement stmt, Map.Entry<Integer, ImgConstants.PointDescription> item) throws SQLException {
                    int type = item.getKey() >> 8;
                    int subType = item.getKey() & 0xFF;
                    ImgConstants.PointDescription info = item.getValue();
                    stmt.setInt(1, type);
                    stmt.setInt(2, subType);
                    stmt.setString(3, info.getDescription());
                    stmt.setString(4, info.getIconName());

                    return true;
                }
            });
    }
}
