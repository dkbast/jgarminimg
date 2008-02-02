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
import org.postgis.LinearRing;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.postgis.Polygon;

import java.io.IOException;
import java.sql.SQLException;

public class PolygonSaver extends Saver
{
    public PolygonSaver(MapExporter mapExporter) throws SQLException
    {
        super(mapExporter);
    }

    protected String getQuery()
    {
        return "insert into POLYGON (MAP, LEVEL, TYPE, CONTOUR, NAME) values (?,?,?,?,?)";
    }

    public void addLine(int type, int[] longitudes, int[] latitudes, int nbPoints, Label label)
    {
        try
        {
            stmt.setInt(1, mapExporter.getCurFile().getFamily().ordinal());
            stmt.setInt(2, mapExporter.getCurLevel());
            stmt.setInt(3, type);
            Point[] points=new Point[nbPoints+1];
            for(int i=0; i<nbPoints; i++)
            {
                points[i]=new Point(CoordUtils.toWGS84(longitudes[i]), CoordUtils.toWGS84(latitudes[i]));
            }
            points[nbPoints]=points[0];
            LinearRing[] ring={new LinearRing(points)};
            Polygon line=new Polygon(ring);
            line.setSrid(4326);
            stmt.setObject(4, new PGgeometry(line));

            setString(5, label!=null?label.getName():null);
            checkBatch();
        }
        catch(SQLException e)
        {
            System.err.println("Error while saving a polyline to the DB:");
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
            System.err.println("Error while reading a polyline's label:");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
