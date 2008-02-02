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
import org.free.garminimg.swing.MapConfig;
import visad.Delaunay;
import visad.VisADException;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;

public class ReliefDrawer<COORD> implements TransformedMapListener
{
    private ArrayList<ContourPoint> contourPoints=new ArrayList();

    private MapConfig workConfig;

    private double minAlt=Double.MAX_VALUE;

    private double maxAlt=-Double.MAX_VALUE;

    private Graphics2D g2;

    private double lx=1/Math.sqrt(3);

    private double ly=-1/Math.sqrt(3);

    private double lz=-1/Math.sqrt(3);

    /**
     * rough estimate of how many pixels per meters.
     */
    private double altFactor;

    private int[] polyX=new int[3];

    private int[] polyY=new int[3];

    public static BitSet reliefTypes;

    public ReliefDrawer(MapConfig workConfig, Graphics2D g2, MapTransformer<COORD> workTransformer)
    {
        this.workConfig=workConfig;
        this.g2=g2;

        altFactor=workTransformer.getPixelsPerMeter();


    }

    public void addPoint(int type, int subType, int x, int y, Label label, boolean indexed)
    {
    }

    public void addPoly(int type, int[] xPoints, int[] yPoints, int nbPoints, Label label, boolean line)
    {
        if(line && type>=ImgConstants.MINOR_LAND_CONTOUR && type<=ImgConstants.MAJOR_DEPTH_CONTOUR)
        {
            try
            {
                Float alt=Float.valueOf(label.getName());
                for(int i=0; i<nbPoints; ++i)
                {
                    contourPoints.add(new ContourPoint(xPoints[i], yPoints[i], alt));
                    minAlt=Math.min(minAlt, alt);
                    maxAlt=Math.max(maxAlt, alt);
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
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
        if(contourPoints.isEmpty()) return;

        float delaunayPoints[][]={new float[contourPoints.size()], new float[contourPoints.size()]};
        for(int i=0; i<contourPoints.size(); i++)
        {
            ContourPoint contourPoint=contourPoints.get(i);
            delaunayPoints[0][i]=contourPoint.x;
            delaunayPoints[1][i]=contourPoint.y;
        }

        try
        {
            Object previousHints=g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            //TODO: Doesn't work perfectly, it should take at least 2 points from the same altitude in each triangle...
            //      For that, you need Constrained Delaunay, but no working Java implementation was found on the web.
            //      Jun for Java was looking promising, but it's going in infinite recursion most of the time. 
            Delaunay delaunay=Delaunay.factory(delaunayPoints, false);

            for(int i=0; i<delaunay.Tri.length; i++)
            {
                int[] tri=delaunay.Tri[i];
                ContourPoint p1=contourPoints.get(tri[0]);
                ContourPoint p2=contourPoints.get(tri[1]);
                ContourPoint p3=contourPoints.get(tri[2]);

                //first vector
                double ax=p1.x-p2.x;
                double ay=p1.y-p2.y;
                double az=(p1.alt-p2.alt)*altFactor;

                //second vector
                double bx=p1.x-p3.x;
                double by=p1.y-p3.y;
                double bz=(p1.alt-p3.alt)*altFactor;

                //compute the normal
                double px=ay*bz-az*by;
                double py=az*bx-ax*bz;
                double pz=ax*by-ay*bx;

                //normalize it
                double pl=Math.sqrt(px*px+py*py+pz*pz);
                px/=pl;
                py/=pl;
                pz/=pl;
                if(pz>0)
                {
                    px*=-1;
                    py*=-1;
                    pz*=-1;
                }

                //scalar product with the light source vector (number between -1 and +1)
                double scalar=lx*px+ly*py+lz*pz;
                float light=(float)((scalar+1)/2);

                //altitude dependent color
                float red=(float)(((p1.alt+p2.alt+p3.alt)/3-minAlt)/(maxAlt-minAlt));
                g2.setColor(new Color(light*red, light*(1-red), light*(1-red), 0.5f));

                polyX[0]=p1.x;
                polyY[0]=p1.y;
                polyX[1]=p2.x;
                polyY[1]=p2.y;
                polyX[2]=p3.x;
                polyY[2]=p3.y;
                g2.fillPolygon(polyX, polyY, 3);
            }

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousHints);
            g2.setColor(Color.BLACK);
        }
        catch(VisADException e)
        {
            e.printStackTrace();
        }
    }

    public static synchronized BitSet getReliefTypes()
    {
        if(reliefTypes==null)
        {
            reliefTypes=new BitSet(ImgConstants.MAJOR_DEPTH_CONTOUR+1);
            reliefTypes.set(ImgConstants.MINOR_LAND_CONTOUR);
            reliefTypes.set(ImgConstants.INTERMEDIATE_LAND_CONTOUR);
            reliefTypes.set(ImgConstants.MAJOR_LAND_CONTOUR);
            reliefTypes.set(ImgConstants.MINOR_DEPTH_CONTOUR);
            reliefTypes.set(ImgConstants.INTERMEDIATE_DEPTH_CONTOUR);
            reliefTypes.set(ImgConstants.MAJOR_DEPTH_CONTOUR);
        }
        return reliefTypes;
    }

    private static class ContourPoint
    {
        int x;

        int y;

        float alt;

        public ContourPoint(int x, int y, float alt)
        {
            this.x=x;
            this.y=y;
            this.alt=alt;
        }
    }
}
