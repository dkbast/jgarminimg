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
package org.free.garminimg.swing;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class DefaultMouseGestures<COORD> implements MouseGestures
{
    int startMoveX=-1;

    int startMoveY=-1;

    int curMoveX=-1;

    int curMoveY=-1;

    protected MapPanel<COORD> mapPanel;

    public DefaultMouseGestures(MapPanel<COORD> mapPanel)
    {
        this.mapPanel=mapPanel;
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        if(e.getButton()==MouseEvent.BUTTON1 || e.getButton()==MouseEvent.BUTTON2)
        {
            startMoveX=e.getX();
            startMoveY=e.getY();
            curMoveX=e.getX();
            curMoveY=e.getY();
        }
        mapPanel.hideInfo();
    }

    public void mouseReleased(MouseEvent e)
    {
        switch(e.getButton())
        {
            case MouseEvent.BUTTON1:
            case MouseEvent.BUTTON2:
                if(startMoveX!=-1)
                {
                    mapPanel.setQuality(MapConfig.Quality.FINAL);
                    mapPanel.movePosition(-e.getX()+curMoveX, -e.getY()+curMoveY);
                }
                startMoveX=-1;
                break;

            case MouseEvent.BUTTON3:
                mapPanel.showInfo(e.getX(), e.getY(), 5);
                break;
        }
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mouseWheelMoved(MouseWheelEvent e)
    {
        mapPanel.hideInfo();
        mapPanel.zoom(Math.pow(1.5, e.getWheelRotation()), e.getX(), e.getY());

        // move the move so that it is centered on the window
        try
        {
            Robot robot=new Robot();
            Point currentLocation=MouseInfo.getPointerInfo().getLocation();
            robot.mouseMove(currentLocation.x-(e.getX()-mapPanel.getWidth()/2), currentLocation.y
                                                                                -(e.getY()-mapPanel.getHeight()/2));
        }
        catch(AWTException e1)
        {
            e1.printStackTrace();
        }
    }

    public void mouseDragged(MouseEvent e)
    {
        if(startMoveX!=-1)
        {
            mapPanel.setQuality(MapConfig.Quality.TEMP);
            mapPanel.movePosition(-e.getX()+curMoveX, -e.getY()+curMoveY);
            curMoveX=e.getX();
            curMoveY=e.getY();
        }
    }

    public void mouseMoved(MouseEvent e)
    {
        mapPanel.hideInfo();
    }
}
