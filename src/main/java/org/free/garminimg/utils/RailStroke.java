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
import java.awt.geom.GeneralPath;

/**
 * Used to draw train rails.
 */
public class RailStroke implements Stroke
{
    BasicStroke lineStroke;

    ShapeStroke shapeStroke;

    public RailStroke(float delta, float railWidth, float lineWidth)
    {
        lineStroke=new BasicStroke(lineWidth);
        GeneralPath line=new GeneralPath();
        line.moveTo(lineWidth/2, railWidth/2);
        line.lineTo(lineWidth/2, -railWidth/2);
        line.lineTo(-lineWidth/2, -railWidth/2);
        line.lineTo(-lineWidth/2, railWidth/2);
        shapeStroke=new ShapeStroke(line, delta);
    }

    public Shape createStrokedShape(Shape p)
    {
        GeneralPath path=new GeneralPath(shapeStroke.createStrokedShape(p));
        path.append(lineStroke.createStrokedShape(p), false);
        return path;
    }

}
