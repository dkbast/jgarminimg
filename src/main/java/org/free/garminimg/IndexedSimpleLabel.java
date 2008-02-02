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
package org.free.garminimg;

import org.free.garminimg.utils.ImgConstants;

import java.io.IOException;

public class IndexedSimpleLabel extends SimpleLabel implements IndexedLabel
{
    private int type;

    private int subType;

    private int latitude;

    private int longitude;

    public IndexedSimpleLabel(ImgFileBag file, int labelOffset, int type, int subType, int longitude, int latitude)
    {
        super(file, labelOffset);
        this.type=type;
        this.subType=subType;
        this.longitude=longitude;
        this.latitude=latitude;
    }

    public int getType()
    {
        return type;
    }

    public int getSubType()
    {
        return subType;
    }

    public int getLatitude()
    {
        return latitude;
    }

    public int getLongitude()
    {
        return longitude;
    }

    public String toDebugHtml() throws IOException
    {
        StringBuilder result=new StringBuilder(super.toDebugHtml());
        result.append("type=").append(ImgConstants.getPointDesc(type, subType)).append("<br>");
        result.append("longitude=").append(CoordUtils.toDMS(longitude)).append("<br>latitude").append(CoordUtils.toDMS(latitude)).append("<br>");
        return result.toString();
    }
}
