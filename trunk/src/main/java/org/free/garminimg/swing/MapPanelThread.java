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
package org.free.garminimg.swing;

import org.free.garminimg.ImgFilesBag;
import org.free.garminimg.MapListener;
import org.free.garminimg.ObjectKind;
import org.free.garminimg.utils.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.VolatileImage;
import java.io.IOException;

public class MapPanelThread<COORD> extends Thread
{
    private MapConfig targetConfig;

    private MapTransformer<COORD> targetTransformer;

    private MapPanel<COORD> panel;

    private VolatileImage previousImage=null;

    private VolatileImage currentImage=null;

    private MapTransformer<COORD> currentTransformer=null;

    private MapConfig currentConfig=null;

    private boolean exit=false;

    private final Object threadSynchro=new Object();

    private boolean needCompute=false;

    public MapPanelThread(MapPanel<COORD> panel, MapConfig config)
    {
        super("Map");
        this.panel=panel;
        this.targetConfig=config;
        this.targetTransformer=null;
    }

    /**
     * force a reschedule
     */
    public void scheduleComputeMap()
    {
        synchronized(threadSynchro)
        {
            needCompute=true;
            threadSynchro.notify();
        }
    }

    private void computeMap()
    {
        final MapConfig workConfig;
        final MapTransformer<COORD> workTransformer;

        //take a snapshot of the config
        synchronized(this)
        {
            if(targetTransformer==null || !targetTransformer.isSetupDone())
                return;

            workConfig=targetConfig;
            workTransformer=targetTransformer.clone();
        }

        final VolatileImage workPaint;
        if(previousImage!=null && !previousImage.contentsLost() &&
           workTransformer.getWidth()==previousImage.getWidth() && workTransformer.getHeight()==previousImage.getHeight())
        {
            //re-use the previousImage;
            workPaint=previousImage;
            previousImage=null;
        }
        else
        {
            //we have to create a new image
            System.out.println("Create a new VolatileImage");
            if(previousImage!=null)
            {
                previousImage.flush();
                previousImage=null;
            }
            workPaint=panel.createVolatileImage(workTransformer.getWidth(), workTransformer.getHeight());
        }

        //compute
        Graphics2D g2=workPaint.createGraphics();
        setupGraphics(g2, workConfig, workTransformer);
        paintMap(g2, workConfig, workTransformer);
        g2.dispose();

        //save the result
        synchronized(this)
        {
            previousImage=currentImage;
            currentImage=workPaint;
            currentTransformer=workTransformer;
            currentConfig=workConfig;
        }
    }

    private void setupGraphics(Graphics2D g2, MapConfig workConfig, MapTransformer<COORD> workTransformer)
    {
        // set the targetQuality stuff
        if(workConfig.getQuality()==MapConfig.Quality.FINAL)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // paint the background
        g2.setColor(MapPanel.BG_COLOR);
        g2.fillRect(0, 0, workTransformer.getWidth(), workTransformer.getHeight());
    }

    private void paintMap(Graphics2D g2, MapConfig workConfig, MapTransformer<COORD> workTransformer)
    {
        final Font oldFont=g2.getFont();
        final Color oldColor=g2.getColor();
        final Stroke oldStroke=g2.getStroke();
        try
        {
            final long milliStart=System.currentTimeMillis();
            Rectangle bbox=workTransformer.getGarminBoundingBox();
            final int minLon=bbox.x;
            final int maxLon=bbox.x+bbox.width;
            final int minLat=bbox.y;
            final int maxLat=bbox.y+bbox.height;
            int resolution=MapPanel.getResolution(minLon, maxLon, workTransformer.getWidth());

            if(workConfig.getDetailLevel()<0)
                resolution<<=-workConfig.getDetailLevel();
            else
                resolution>>=workConfig.getDetailLevel();

            StatsListener drawer=new StatsListener(panel.createMapDrawer(workConfig, g2, 9.0f, workTransformer));
            //TransformedMapListener drawer=panel.createMapDrawer(workConfig, g2, 9.0f, workTransformer);

            ImgFilesBag map=panel.getMap();
            if(workConfig.wantShading() && workConfig.getQuality()==MapConfig.Quality.FINAL)
            {
                ReliefDrawer<COORD> reliefDrawer=new ReliefDrawer<COORD>(workConfig, g2, workTransformer);
                MapListener drawerConverter=new CoordinateConverterListener<COORD>(workTransformer,
                                                                                   new ClippingMapListener(0, workTransformer.getWidth(), 0, workTransformer.getHeight(),
                                                                                                           drawer));
                MapListener reliefConverter=new CoordinateConverterListener<COORD>(workTransformer,
                                                                                   new ClippingMapListener(0, workTransformer.getWidth(), 0, workTransformer.getHeight(),
                                                                                                           reliefDrawer));

                //draw the full base map
                map.readMapForDrawing(minLon, maxLon, minLat, maxLat, resolution, ObjectKind.BASE_MAP, drawerConverter);

                //draw the polygons of the normal maps
                map.readMapForDrawing(minLon, maxLon, minLat, maxLat, resolution, ObjectKind.POLYGON|ObjectKind.NORMAL_MAP, drawerConverter);

                //put the relief on them
                map.readMap(minLon, maxLon, minLat, maxLat, resolution, ObjectKind.POLYLINE|ObjectKind.NORMAL_MAP, ReliefDrawer.getReliefTypes(), reliefConverter);
                reliefConverter.finishPainting();

                //draw the rest
                map.readMapForDrawing(minLon, maxLon, minLat, maxLat, resolution, (0xFFFF^ObjectKind.POLYGON)^ObjectKind.BASE_MAP, drawerConverter);
            }
            else
            {
                MapListener converter=new CoordinateConverterListener<COORD>(workTransformer,
                                                                             new ClippingMapListener(0, workTransformer.getWidth(), 0, workTransformer.getHeight(),
                                                                                                     drawer));
                map.readMapForDrawing(minLon, maxLon, minLat, maxLat, resolution, 0xFFFF, converter);
            }

            long milliEnd=System.currentTimeMillis();
            System.out.println("Time to compute a {"+workTransformer.getWidth()+"x"+workTransformer.getHeight()+drawer+"} map [ms]: "+(milliEnd-milliStart));
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        g2.setFont(oldFont);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    public synchronized void checkSetup(MapTransformer<COORD> transformer, MapConfig config)
    {
        if(currentImage==null || currentImage.contentsLost() || !transformer.equals(targetTransformer) || !config.equals(targetConfig))
        {
            targetConfig=config.clone();
            targetTransformer=transformer.clone();
            scheduleComputeMap();
        }
    }

    public synchronized PaintInfo getPaintInfo(MapTransformer<COORD> transformer)
    {
        if(currentImage==null || currentImage.contentsLost())
            return null;

        final Point2D.Double prevNW=currentTransformer.getNorthWestWGS84();
        final Point2D.Double prevSE=currentTransformer.getSouthEastWGS84();
        final COORD temp=transformer.createTempCoord();

        final Point2D.Double delta=new Point2D.Double();
        transformer.wgs84ToMap(prevNW.x, prevNW.y, temp, delta);

        final Point2D.Double deltaSize=new Point2D.Double();
        transformer.wgs84ToMap(prevSE.x, prevSE.y, temp, deltaSize);

        deltaSize.x-=delta.x;
        deltaSize.y-=delta.y;

        //for rounding instead of flooring
        deltaSize.x+=0.5;
        deltaSize.y+=0.5;
        delta.x+=0.5;
        delta.y+=0.5;

        return new PaintInfo(currentImage, (int)delta.x, (int)delta.y, (int)deltaSize.x, (int)deltaSize.y);
    }

    public static class PaintInfo
    {
        public final Image image;

        public final int x;

        public final int y;

        public final int w;

        public final int h;

        public PaintInfo(Image image, int x, int y, int w, int h)
        {
            this.image=image;
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
        }

        public String toString()
        {
            return "PaintInfo{x="+x+" y="+y+" w="+w+" h="+h+"}";
        }
    }

    public void run()
    {
        while(!exit)
        {
            boolean doCompute=false;
            synchronized(threadSynchro)
            {
                if(needCompute)
                {
                    doCompute=true;
                    needCompute=false;
                }
                else
                {
                    try
                    {
                        threadSynchro.wait();
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }

            if(doCompute)
            {
                try
                {
                    computeMap();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                panel.repaint();
            }
        }
    }
}
