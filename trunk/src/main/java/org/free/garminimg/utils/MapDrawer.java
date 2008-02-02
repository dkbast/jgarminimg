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
import org.free.garminimg.SubDivision;
import org.free.garminimg.swing.MapConfig;
import org.free.garminimg.utils.ImgConstants.PolygonDescription;
import org.free.garminimg.utils.ImgConstants.PolylineDescription;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.io.IOException;

public class MapDrawer implements TransformedMapListener
{
    private ImageObserver mapPanel;

    protected Graphics2D g2;

    private LabelDeClutteringFilter labelFilter;

    private MapConfig config;

    public MapDrawer(MapConfig config, Graphics2D g2, ImageObserver mapPanel, float fontSize, Paint labelColor, Paint labelBackgroundColor)
    {
        this.mapPanel=mapPanel;
        this.config=config;
        this.g2=g2;
        this.labelFilter=new LabelDeClutteringFilter(g2, fontSize, labelColor, labelBackgroundColor);
    }

    public void addPoint(int type, int subType, int x, int y, Label label, boolean indexed)
    {
        ImgConstants.PointDescription setup=ImgConstants.getPointType(type, subType);
        ImageIcon icon=null;
        try
        {
            icon=setup.getIcon();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        g2.setColor(Color.RED);
        int width=0;
        int height=0;
        if(icon!=null)
        {
            width=icon.getIconWidth();
            height=icon.getIconHeight();
            g2.drawImage(icon.getImage(), x-width/2, y-height/2, mapPanel);
        }
        else
        {
            if(setup.hasPoint())
            {
                width=5;
                height=5;
                g2.drawOval(x-width/2, y-height/2, width, height);
            }
        }

        if(config.isShowPointLabel() && !setup.isIgnoreLabel() && label!=null &&
           config.getQuality()==MapConfig.Quality.FINAL &&
           config.getPoiThreshold()+ImgConstants.POINT_BASE_PRIORITY>=setup.getPriority())
            labelFilter.addPointLabel(x, y, width, height, label, setup.getPriority());
    }

    public void addPoly(int type, int[] xPoints, int[] yPoints, int nbPoints, Label label, boolean line)
    {
        g2.setColor(Color.BLACK);
        ImgConstants.Description setup;
        if(line)
        {
            setup=setLineStyle(type);
            g2.drawPolyline(xPoints, yPoints, nbPoints);
            if(config.isShowLineLabel() && !setup.isIgnoreLabel() && label!=null && config.getQuality()==MapConfig.Quality.FINAL)
                labelFilter.addLineLabel(xPoints, yPoints, nbPoints, label, setup.getPriority());
        }
        else
        {
            setup=setPolygonStyle(type);
            g2.fillPolygon(xPoints, yPoints, nbPoints);
            if(config.isShowPolygonLabel() && !setup.isIgnoreLabel() && label!=null && config.getQuality()==MapConfig.Quality.FINAL)
                labelFilter.addSurfaceLabel(xPoints, yPoints, nbPoints, label, setup.getPriority());
        }
    }

    public void startMap(ImgFileBag file)
    {
    }

    public void startSubDivision(SubDivision subDivision)
    {
    }

    protected PolygonDescription setPolygonStyle(int type)
    {
        PolygonDescription setup=ImgConstants.getPolygonType(type);
        g2.setPaint(setup.getPaint());
        if(setup.getPaint() instanceof Color)
        {
            g2.setColor((Color)setup.getPaint());
        }
        return setup;
    }

    protected PolylineDescription setLineStyle(int type)
    {
        PolylineDescription setup=ImgConstants.getPolylineType(type);
        g2.setColor(setup.getColor());
        g2.setStroke(setup.getStroke());
        return setup;
    }

    public void finishPainting()
    {
        labelFilter.paint();
    }
}
