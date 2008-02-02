/*
 * JGarminImgParser - A java library to parse .IMG Garmin map files.
 *
 * Copyright (C) 2007 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.free.garminimg.export;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public abstract class Saver
{
    protected MapExporter mapExporter;

    protected PreparedStatement stmt=null;

    private int curBatchSize=0;

    private int total=0;

    public Saver(MapExporter mapExporter)
    {
        this.mapExporter=mapExporter;
    }

    public void init(Connection conn) throws SQLException
    {
        stmt=conn.prepareStatement(getQuery());
    }

    protected void checkBatch() throws SQLException
    {
        stmt.addBatch();

        ++total;
        if(++curBatchSize>=500)
        {
            System.out.println(getClass().getName()+": "+total);
            stmt.executeBatch();
            curBatchSize=0;
        }
    }

    public void finish() throws SQLException
    {
        if(curBatchSize>0)
        {
            stmt.executeBatch();
        }
    }

    protected void setString(int pos, String value) throws SQLException
    {
        if(value!=null)
            stmt.setString(pos, value);
        else
            stmt.setNull(pos, Types.VARCHAR);
    }

    protected abstract String getQuery();

}
