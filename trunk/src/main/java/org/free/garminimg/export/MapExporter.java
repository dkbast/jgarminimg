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

import org.free.garminimg.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class MapExporter implements MapListener {
    private final Connection conn;

    private ImgFileBag curFile = null;

    private int curLevel = -1;

    private PointSaver pointSaver;

    private PolylineSaver polylineSaver;

    private PolygonSaver polygonSaver;

    private int nbPoints = 0;

    private int nbPolylines = 0;

    private int nbPolygons = 0;

    public MapExporter(Connection conn) throws SQLException {
        this.conn = conn;

        pointSaver = new PointSaver(this);
        pointSaver.init(conn);

        polylineSaver = new PolylineSaver(this);
        polylineSaver.init(conn);

        polygonSaver = new PolygonSaver(this);
        polygonSaver.init(conn);
    }

    public void exportMaps(ImgFilesBag maps) {
        int coord = (1 << 24);
        try {
            maps.readMap(-coord, coord, -coord, coord, -1, ObjectKind.INDEXED_POINT | ObjectKind.POINT, null, this);
            //maps.readMap(-coord, coord, -coord, coord, -1, ObjectKind.ALL, null, this);
        }
        catch (IOException e) {
            System.err.println("Error while reading the maps:");
            e.printStackTrace();
            System.exit(-1);
        }

        commit();

        System.out.println("Saved nbPoints=" + nbPoints + " nbPolygons=" + nbPolygons + " nbPolylines=" + nbPolylines);
    }

    private void commit() {
        try {
            if (pointSaver != null) pointSaver.finish();
            if (polylineSaver != null) polylineSaver.finish();
            if (polygonSaver != null) polygonSaver.finish();
            conn.commit();
        }
        catch (SQLException e) {
            System.err.println("Error while committing the DB:");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void addPoint(int type, int subType, int longitude, int latitude, Label label, boolean indexed) {
        if (pointSaver != null) {
            pointSaver.addPoint(type, subType, longitude, latitude, label);
            nbPoints++;
        }
    }

    public void addPoly(int type, int[] longitudes, int[] latitudes, int nbPoints, Label label, boolean line, boolean direction) {
        if (line) {
            if (nbPoints >= 2 && polylineSaver != null)   //don't want to insert invalid polylines...
            {
                polylineSaver.addLine(type, longitudes, latitudes, nbPoints, label);
                nbPolylines++;
            }
        } else {
            if (nbPoints >= 3 && polygonSaver != null)   //don't want to insert invalid polygons...
            {
                polygonSaver.addLine(type, longitudes, latitudes, nbPoints, label);
                nbPolygons++;
            }
        }
    }

    public void startMap(ImgFileBag file) {
        //commit();
        curFile = file;
    }

    public void startSubDivision(SubDivision subDivision) {
        curLevel = subDivision.getLevel();
    }

    public void finishPainting() {
    }

    public ImgFileBag getCurFile() {
        return curFile;
    }

    public int getCurLevel() {
        return curLevel;
    }
}
