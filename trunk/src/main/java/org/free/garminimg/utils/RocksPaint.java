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

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

public class RocksPaint implements Paint
{
    private TexturePaint texturePaint;

    private static final int size=4;

    public RocksPaint()
    {
        BufferedImage img=new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2=img.createGraphics();
        g2.setColor(Color.GRAY);
        g2.setStroke(new BasicStroke(0.5f));
        g2.drawLine(0, 0, size, size);
        Rectangle rect=new Rectangle(0, 0, size, size);
        texturePaint=new TexturePaint(img, rect);
    }

    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints)
    {
        return texturePaint.createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    public int getTransparency()
    {
        return texturePaint.getTransparency();
    }

}
