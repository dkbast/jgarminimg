/*
 * JGarminImgParser - A java library to parse .IMG Garmin map files.
 *
 * Copyright (C) 2006 Patrick Valsecchi
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
package org.free.garminimg.utils;

import org.free.garminimg.ImgFileBag;
import org.free.garminimg.Label;
import org.free.garminimg.MapListener;
import org.free.garminimg.SubDivision;

import java.util.BitSet;

/**
 * A map listener to discover all the object types available. Takes into account only the objects having a label.
 */
public class DiscoverTypesMapListener implements MapListener
{
    private BitSet pointTypes=new BitSet(255);

    private BitSet polylineTypes=new BitSet(511);

    private BitSet polygonTypes=new BitSet(511);

    public void addPoint(int type, int subType, int longitude, int latitude, Label label, boolean indexed)
    {
        if(label==null || ImgConstants.getPointType(type, subType).isIgnoreLabel()) return;
        pointTypes.set(type);
    }

    public void addPoly(int type, int[] longitudes, int[] latitudes, int nbPoints, Label label, boolean line)
    {
        if(label==null) return;
        if(line)
        {
            if(!ImgConstants.getPolylineType(type).isIgnoreLabel())
                polylineTypes.set(type);
        }
        else
        {
            if(!ImgConstants.getPolygonType(type).isIgnoreLabel())
                polygonTypes.set(type);
        }
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

    public BitSet getPointTypes()
    {
        return pointTypes;
    }

    public BitSet getPolylineTypes()
    {
        return polylineTypes;
    }

    public BitSet getPolygonTypes()
    {
        return polygonTypes;
    }
}
