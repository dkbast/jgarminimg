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
package org.free.garminimg.utils;

import org.free.garminimg.ImgFileBag;
import org.free.garminimg.Label;
import org.free.garminimg.SubDivision;

/**
 * Collect some statistics about what is being drawn and forward everything to the next listener.
 */
public class StatsListener implements TransformedMapListener
{
    private TransformedMapListener drawer;

    private int nbPoints=0;

    private int nbPolylines=0;

    private int nbPolylinePoints=0;

    private int nbPolygons=0;

    private int nbPolygonPoints=0;

    public StatsListener(TransformedMapListener drawer)
    {
        this.drawer=drawer;
    }

    public void addPoint(int type, int subType, int x, int y, Label label, boolean indexed)
    {
        drawer.addPoint(type, subType, x, y, label, indexed);
        nbPoints++;
    }

    public void addPoly(int type, int[] xPoints, int[] yPoints, int nbPoints, Label label, boolean line)
    {
        drawer.addPoly(type, xPoints, yPoints, nbPoints, label, line);
        if(line)
        {
            nbPolylines++;
            nbPolylinePoints+=nbPoints;
        }
        else
        {
            nbPolygons++;
            nbPolygonPoints+=nbPoints;
        }
    }

    public void startMap(ImgFileBag file)
    {
        drawer.startMap(file);
    }

    public void startSubDivision(SubDivision subDivision)
    {
        drawer.startSubDivision(subDivision);
    }

    public void finishPainting()
    {
        drawer.finishPainting();
    }

    public String toString()
    {
        StringBuilder result=new StringBuilder();
        if(nbPoints>0) result.append(" nbPoints=").append(nbPoints);
        if(nbPolygons>0) result.append(" nbPolygons=").append(nbPolygons);
        if(nbPolygonPoints>0) result.append(" nbPolygonPoints=").append(nbPolygonPoints);
        if(nbPolylines>0) result.append(" nbPolylines=").append(nbPolylines);
        if(nbPolylinePoints>0) result.append(" nbPolylinePoints=").append(nbPolylinePoints);
        return result.toString();
    }
}
