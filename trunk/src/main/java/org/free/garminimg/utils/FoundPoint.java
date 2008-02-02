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

import org.free.garminimg.CoordUtils;
import org.free.garminimg.Label;

import java.io.IOException;

public class FoundPoint extends FoundObject
{
    private int subType;

    private int longitude;

    private int latitude;

    private boolean indexed;

    public FoundPoint(int type, int subType, int longitude, int latitude, Label label, boolean indexed)
    {
        super(type, label);
        this.indexed=indexed;
        this.latitude=latitude;
        this.longitude=longitude;
        this.subType=subType;
    }

    public boolean isIndexed()
    {
        return indexed;
    }

    public int getLatitude()
    {
        return latitude;
    }

    public int getLongitude()
    {
        return longitude;
    }

    public int getSubType()
    {
        return subType;
    }

    public void toDebugHtml(StringBuilder result) throws IOException
    {
        if(indexed)
            result.append("<b>Indexed point</b><br>\n");
        else
            result.append("<b>Point</b><br>\n");
        ImgConstants.PointDescription setup=ImgConstants.getPointType(getType(), subType);
        result.append("what=").append(setup.getDescription()).append(" (").append(setup.getPriority()).append(")").append("<br>");
        super.toDebugHtml(result);
        if(subType!=0)
            result.append("subType=0x").append(Integer.toHexString(subType)).append("<br>");
        result.append("latitude=").append(CoordUtils.toDMS(latitude)).append("<br>");
        result.append("longitude=").append(CoordUtils.toDMS(longitude)).append("<br>");
    }
}
