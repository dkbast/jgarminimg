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
package org.free.garminimg;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class SaveMapTest extends TestCase
{
    private static final int NB_RUNS=200;

    public void testPureRead() throws IOException
    {
        ImgFilesBag maps=new ImgFilesBag();
        File file=new File("/home/patrick/gps/maps/00000002.img");
        if(!file.exists()) return;        
        maps.addFile(file);

        final int minLon=maps.getMinLongitude();
        final int maxLon=maps.getMaxLongitude();
        final int minLat=maps.getMinLatitude();
        final int maxLat=maps.getMaxLatitude();

        MapListener listener=new TestListener();

        //warmup
        for(int i=0; i<10; ++i)
        {
            maps.readMapForDrawing(minLon, maxLon, minLat, maxLat, 1, ObjectKind.ALL, listener);
        }

        long sumTime=0;
        for(int i=0; i<NB_RUNS; ++i)
        {
            long start=System.currentTimeMillis();
            maps.readMapForDrawing(minLon, maxLon, minLat, maxLat, 1, ObjectKind.ALL, listener);
            sumTime+=System.currentTimeMillis()-start;
        }

        System.out.println(String.format("Avg time: %fms", (double)sumTime/NB_RUNS));
    }

    public static class TestListener implements MapListener
    {
        public void addPoint(int type, int subType, int longitude, int latitude, Label label, boolean indexed)
        {
        }

        public void addPoly(int type, int[] longitudes, int[] latitudes, int nbPoints, Label label, boolean line, boolean direction)
        {
        }

        public void startMap(ImgFileBag file)
        {
        }

        public void startSubDivision(SubDivision subDivision)
        {
        }

        public void finishPainting()
        {
        }
    }
}
