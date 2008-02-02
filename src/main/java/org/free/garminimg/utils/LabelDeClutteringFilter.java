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

import org.free.garminimg.Label;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Sort the labels by importance for latter drawing.
 */
public class LabelDeClutteringFilter
{
    private Graphics2D g2;

    private Font font;

    private Paint frontColor;

    private Paint backgroundColor;

    private SortedSet<LabelInfo> everyLabels=new TreeSet<LabelInfo>();

    private SortedSet<String> knownNames=new TreeSet<String>();

    private ArrayList<LabelInfo> toPaintLabels=new ArrayList<LabelInfo>();

    private static final int MARGIN_X=5;

    private static final int MARGIN_Y=2;

    private static final int LINE_LENGTH=2;

    private static final int LINE_MARGIN=2;

    private final FontRenderContext fontRenderContext;

    public LabelDeClutteringFilter(Graphics2D g2, float fontSize, Paint frontColor, Paint backgroundColor)
    {
        this.g2=g2;
        font=g2.getFont().deriveFont(fontSize);
        this.frontColor=frontColor;
        this.backgroundColor=backgroundColor;
        fontRenderContext=g2.getFontRenderContext();
    }

    /**
     * Will try to put the label horizontally around the point
     */
    public void addPointLabel(int x, int y, int width, int height, Label label, int priority)
    {
        if(width!=0 && height!=0)
            addLabel(x+width/2+3, y, label, Placement.MIDDLE_LEFT, priority);
        else
            addLabel(x, y, label, Placement.MIDDLE_CENTER, priority);

    }

    /**
     * Will try to put the label attached to one of the segments of the given line
     */
    public void addLineLabel(int[] xPoints, int[] yPoints, int nbPoints, Label label, int priority)
    {

        int middle=nbPoints/2;
        if(middle==nbPoints-1) middle--;

        final Placement placement;
        int x;
        int y;
        int dx=0;
        int dy=0;
        if(middle>=0)
        {
            int x1=xPoints[middle];
            int y1=yPoints[middle];
            int x2=xPoints[middle+1];
            int y2=yPoints[middle+1];
            x=(x1+x2)/2;
            y=(y1+y2)/2;

            if(Math.abs(x1-x2)<Math.abs(y1-y2))
            {
                //mainly horizontal
                placement=Placement.MIDDLE_LEFT;
                dx=3;
            }
            else
            {
                //mainly vertical
                placement=Placement.TOP_CENTER;
                dy=3;
            }

        }
        else
        {
            x=xPoints[0];
            y=yPoints[0];
            placement=Placement.MIDDLE_LEFT;
            dx=3;
        }

        addLabel(x+dx, y+dy, label, placement, priority);
    }

    /**
     * will try to put the label horizontaly within the given surface
     */
    public void addSurfaceLabel(int[] xPoints, int[] yPoints, int nbPoints, Label label, int priority)
    {
        int minX=Integer.MAX_VALUE;
        int minY=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE;
        int maxY=Integer.MIN_VALUE;
        for(int cpt=0; cpt<nbPoints; ++cpt)
        {
            minX=Math.min(minX, xPoints[cpt]);
            minY=Math.min(minY, yPoints[cpt]);
            maxX=Math.max(maxX, xPoints[cpt]);
            maxY=Math.max(maxY, yPoints[cpt]);
        }
        if(maxX-minX<30)
            addLabel(maxX, (minY+maxY)/2, label, Placement.MIDDLE_LEFT, priority);
        else
            addLabel((minX+maxX)/2, (minY+maxY)/2, label, Placement.MIDDLE_CENTER, priority);
    }

    private enum Placement
    {
        MIDDLE_LEFT,
        TOP_CENTER,
        MIDDLE_CENTER
    }

    private static class LabelInfo implements Comparable<LabelInfo>
    {
        private String name;

        private Rectangle2D bounds;

        private int x;

        private int y;

        private Placement placement;

        private boolean firstInstance;

        private int priority;

        public LabelInfo(String name, Rectangle2D boundings, int x, int y, int priority, boolean firstInstance, Placement placement)
        {
            this.firstInstance=firstInstance;
            this.priority=priority;
            this.name=name;
            this.bounds=boundings;
            this.x=x;
            this.y=y;
            this.placement=placement;
        }

        public void paint(Graphics2D g2, Paint frontColor, Paint backColor)
        {
            if(backColor!=null)
            {
                g2.setPaint(backColor);
                int height=(int)bounds.getHeight();
                g2.fillRect((int)bounds.getMinX()+MARGIN_X-1, (int)bounds.getMinY()+MARGIN_Y-1, (int)bounds.getWidth()-2*MARGIN_X+3, height-2*MARGIN_Y+3);
            }
            g2.setPaint(frontColor);
            g2.drawString(name, x, y);
            switch(placement)
            {
                case MIDDLE_CENTER:
                    break;
                case MIDDLE_LEFT:
                    int cy=(int)(bounds.getCenterY()+0.5);
                    g2.drawLine(x-LINE_MARGIN, cy, x-LINE_LENGTH-LINE_MARGIN, cy);
                    break;
                case TOP_CENTER:
                    int cx=(int)(bounds.getCenterX()+0.5);
                    int ty=y-(int)(bounds.getHeight()+0.5)+2*MARGIN_Y;
                    g2.drawLine(cx, ty-LINE_MARGIN, cx, ty-LINE_LENGTH-LINE_MARGIN);
                    break;
            }
        }

        public int compareTo(LabelInfo o)
        {
            if(firstInstance!=o.firstInstance)
                return firstInstance?-1:1;
            if(priority!=o.priority)
                return priority-o.priority;
            int nameCompare=name.compareTo(o.name);
            if(nameCompare!=0)
            {
                return nameCompare;
            }
            if(x!=o.x)
                return x-o.x;
            return y-o.y;
        }
    }

    public void paint()
    {
        //compute what label has to be painted
        for(LabelInfo labelInfo : everyLabels)
        {
            testAddLabelToPaint(labelInfo);
        }

        //paint them
        Font oldFont=g2.getFont();
        g2.setPaint(frontColor);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(font);
        for(int i=0; i<toPaintLabels.size(); i++)
        {
            LabelInfo info=toPaintLabels.get(i);
            info.paint(g2, frontColor, backgroundColor);
        }
        g2.setFont(oldFont);
    }

    /**
     * Check if a label can be painted or not (overlapping with another one).
     * Add it to {@link #toPaintLabels} if it can be painted.
     */
    private void testAddLabelToPaint(LabelInfo labelInfo)
    {
        for(int i=0; i<toPaintLabels.size(); i++)
        {
            LabelInfo info=toPaintLabels.get(i);
            if(info.bounds.intersects(labelInfo.bounds))
                return;
        }
        toPaintLabels.add(labelInfo);
    }

    private void addLabel(int x, int y, Label label, Placement placement, int priority)
    {
        String name;
        try
        {
            name=label.getName();
            if(name.length()>0)
            {
                TextLayout layout=new TextLayout(name, font, fontRenderContext);
                Rectangle2D bounds=layout.getBounds();
                switch(placement)
                {
                    case MIDDLE_CENTER:
                        x-=bounds.getCenterX();
                        y-=bounds.getCenterY();
                        break;
                    case MIDDLE_LEFT:
                        y-=bounds.getCenterY();
                        x+=LINE_LENGTH+LINE_MARGIN;
                        break;
                    case TOP_CENTER:
                        x-=bounds.getCenterX();
                        y+=bounds.getHeight()+LINE_LENGTH+LINE_MARGIN;
                        break;
                }

                //translate the bounds
                bounds.setRect(x+bounds.getX()-MARGIN_X, y+bounds.getY()-MARGIN_Y, bounds.getWidth()+2*MARGIN_X, bounds.getHeight()+2*MARGIN_Y);

                boolean firstInstance=knownNames.add(name);
                everyLabels.add(new LabelInfo(name, bounds, x, y, priority, firstInstance, placement));
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
