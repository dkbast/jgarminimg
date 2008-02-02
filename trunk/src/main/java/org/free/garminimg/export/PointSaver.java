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

import org.free.garminimg.CoordUtils;
import org.free.garminimg.Label;
import org.free.garminimg.POILabel;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.io.IOException;
import java.sql.SQLException;

public class PointSaver extends Saver
{
    public PointSaver(MapExporter mapExporter) throws SQLException
    {
        super(mapExporter);
    }

    protected String getQuery()
    {
        return "insert into POI (MAP, LEVEL, TYPE, SUB_TYPE, POSITION, NAME, STREET_NUMBER, STREET, CITY, ZIP, PHONE) values (?,?,?,?,?,?,?,?,?,?,?)";
    }

    public void addPoint(int type, int subType, int longitude, int latitude, Label label)
    {
        try
        {
            stmt.setInt(1, mapExporter.getCurFile().getFamily().ordinal());
            stmt.setInt(2, mapExporter.getCurLevel());
            stmt.setInt(3, type);
            stmt.setInt(4, subType);
            Point point=new Point(CoordUtils.toWGS84(longitude), CoordUtils.toWGS84(latitude));
            point.setSrid(4326);
            stmt.setObject(5, new PGgeometry(point));

            setString(6, label!=null?label.getName():null);
            if(label!=null && label instanceof POILabel)
            {
                POILabel poi=(POILabel)label;
                setString(7, poi.getStreetNumber());
                setString(8, poi.getStreet());
                setString(9, poi.getCity());
                setString(10, poi.getZip());
                setString(11, poi.getPhone());
            }
            else
            {
                setString(7, null);
                setString(8, null);
                setString(9, null);
                setString(10, null);
                setString(11, null);
            }
            checkBatch();
        }
        catch(SQLException e)
        {
            System.err.println("Error while saving a point to the DB:");
            e.printStackTrace();

            SQLException nextException=e.getNextException();
            if(nextException!=null)
            {
                System.err.println();
                System.err.println("Next exception");
                nextException.printStackTrace();
            }

            System.exit(-1);
        }
        catch(IOException e)
        {
            System.err.println("Error while reading a point's label:");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
