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

import org.free.garminimg.ImgFilesBag;
import org.postgis.PGgeometry;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgisExporter
{
    public static Connection getConnection()
    {
        String dbConnection=System.getProperty("db");
        if(dbConnection==null)
        {
            help("Missing 'db' property.");
        }

        Connection conn;
        try
        {
            Class.forName("org.postgresql.Driver");
            conn=DriverManager.getConnection(dbConnection, System.getProperty("dbUser"), System.getProperty("dbPassword"));
            ((org.postgresql.PGConnection)conn).addDataType("geometry", PGgeometry.class);
            conn.setAutoCommit(false);
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
        return conn;
    }

    public static void main(String[] args)
    {
        String mapLocation=System.getProperty("map");
        if(mapLocation==null)
        {
            help("Missing 'map' property.");
        }

        Connection conn=getConnection();

        try
        {
            MapExporter exporter=new MapExporter(conn);

            ImgFilesBag maps=new ImgFilesBag();
            File mapLocFile=new File(mapLocation);
            try
            {
                if(mapLocFile.isDirectory())
                    maps.addDirectory(mapLocFile);
                else
                    maps.addFile(mapLocFile);
            }
            catch(IOException e)
            {
                System.out.println("Cannot open map location: "+e.getMessage());
                System.exit(-1);
            }

            exporter.exportMaps(maps);

            conn.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    private static void help(String message)
    {
        if(message!=null)
        {
            System.out.println(message);
            System.out.println();
        }

        System.out.println("Garmin IMG file(s) exporter for PostGIS database.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java ... -Dmap={location} PostgisExporter");
        System.out.println();
        System.out.println("Properties:");
        System.out.println("  map={location}      IMG file or directory to export");
        System.out.println("  db={name}              name of the Postgres DB to connect");
        System.out.println("  dbUser={user}          username");
        System.out.println("  dbPassword={password}  password");

        System.exit(-1);
    }
}
