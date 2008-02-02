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
package org.free.garminimg;

import junit.framework.TestCase;
import org.free.garminimg.ImgFileBag;
import org.free.garminimg.Label;
import org.free.garminimg.SubDivision;
import org.free.garminimg.utils.ClippingMapListener;
import org.free.garminimg.utils.TransformedMapListener;

public class ClippingMapListenerTest extends TestCase
{
    public void testLosangeLine()
    {
        int expected[][][]=
                {{{6, 8}, {8, 6}},
                 {{8, 2}, {6, 0}},
                 {{2, 0}, {0, 2}},
                 {{0, 6}, {2, 8}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={4, 10, 4, -2};
        int lats[]={10, 4, -2, 4};
        clipper.addPoly(0, lons, lats, lons.length, null, true);
    }

    public void testTangentLosangeLine()
    {
        int expected[][][]=
                {{{8, 8}},
                 {{8, 0}},
                 {{0, 0}},
                 {{0, 8}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={4, 12, 4, -4};
        int lats[]={12, 4, -4, 4};
        clipper.addPoly(0, lons, lats, lons.length, null, true);
    }

    public void testLosangePolygon()
    {
        int expected[][][]=
                {{{6, 8}, {8, 6},
                  {8, 2}, {6, 0},
                  {2, 0}, {0, 2},
                  {0, 6}, {2, 8}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={4, 10, 4, -2};
        int lats[]={10, 4, -2, 4};
        clipper.addPoly(0, lons, lats, lons.length, null, false);
    }

    public void testTangentLosangePolygon()
    {
        int expected[][][]=
                {{{8, 8},
                  {8, 0},
                  {0, 0},
                  {0, 8}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={4, 12, 4, -4};
        int lats[]={12, 4, -4, 4};
        clipper.addPoly(0, lons, lats, lons.length, null, false);
    }

    public void testTetrisPolygon()
    {
        int expected[][][]=
                {{{7, 2},
                  {8, 2},
                  {8, 3},
                  {7, 3}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={7, 10, 10, 9, 9, 7};
        int lats[]={2, 2, 5, 5, 3, 3};
        clipper.addPoly(0, lons, lats, lons.length, null, false);
    }

    public void testCornerPolygon()
    {
        int expected[][][]=
                {{{2, 4},
                  {4, 2},
                  {2, 0},
                  {0, 0},
                  {0, 2}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={2, 4, 0, -2};
        int lats[]={4, 2, -2, 0};
        clipper.addPoly(0, lons, lats, lons.length, null, false);
    }

    public void testCornerPolygonWithTwist()
    {
        int expected[][][]=
                {{{2, 4},
                  {4, 2},
                  {2, 0},
                  {4, 0},
                  {4, 1},
                  {5, 1},
                  {5, 0},
                  {0, 0},
                  {0, 2}}
                };
        TestMapListener listener=new TestMapListener(expected);
        ClippingMapListener clipper=new ClippingMapListener(0, 8, 0, 8, listener);

        int lons[]={2, 4, 0, 4, 4, 5, 5, 0, -2};
        int lats[]={4, 2, -2, -2, 1, 1, -3, -3, 0};
        clipper.addPoly(0, lons, lats, lons.length, null, false);
    }

    private class TestMapListener implements TransformedMapListener
    {
        int expected[][][];

        int curPoly=0;

        public TestMapListener(int[][][] expected)
        {
            this.expected=expected;
        }

        public void addPoint(int type, int subType, int x, int y, Label label, boolean indexed)
        {
        }

        public void addPoly(int type, int[] xPoints, int[] yPoint, int nbPoints, Label label, boolean line)
        {
            assertTrue(curPoly<expected.length);
            boolean matching=expected[curPoly].length==nbPoints;
            for(int cpt=0; cpt<nbPoints && matching; ++cpt)
            {
                if(expected[curPoly][cpt][0]!=xPoints[cpt] ||
                   expected[curPoly][cpt][1]!=yPoint[cpt])
                {
                    matching=false;
                }
            }

            if(!matching)
            {
                System.out.println("Polygon #"+(curPoly+1)+" is not matching:");
                System.out.print("  Expected=");
                for(int cpt=0; cpt<expected[curPoly].length; ++cpt)
                {
                    System.out.print("{"+expected[curPoly][cpt][0]+","+expected[curPoly][cpt][1]+"},");
                }
                System.out.println();
                System.out.print("  Actual=");
                for(int cpt=0; cpt<nbPoints; ++cpt)
                {
                    System.out.print("{"+xPoints[cpt]+","+yPoint[cpt]+"},");
                }
                System.out.println();
                assertTrue(false);
            }
            curPoly++;
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
