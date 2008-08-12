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
import org.pvalsecc.opts.Option;
import org.pvalsecc.opts.GetOptions;
import org.pvalsecc.opts.InvalidOption;
import org.pvalsecc.jdbc.ConnectionFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgisExporter extends BaseExporter
{
    @Option(desc="Map file(s) location", mandatory = true)
    private String mapLocation=null;

    public PostgisExporter(String[] argv) throws IllegalAccessException {
        getOptions(argv);
    }

    public void run()
    {
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

    public static void main(String[] args) throws IllegalAccessException {
        PostgisExporter exporter=new PostgisExporter(args);
        exporter.run();
    }
}
