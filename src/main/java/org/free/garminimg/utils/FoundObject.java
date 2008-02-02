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

import org.free.garminimg.Label;

import java.io.IOException;

public abstract class FoundObject
{
    private Label label;

    private int type;

    public FoundObject(int type, Label label)
    {
        this.label=label;
        this.type=type;
    }

    public Label getLabel()
    {
        return label;
    }

    public int getType()
    {
        return type;
    }

    public abstract int getLatitude();

    public abstract int getLongitude();

    public void toDebugHtml(StringBuilder result) throws IOException
    {
        if(label!=null)
            result.append("<table valign=\"top\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td>label=</td><td>").append(label.toDebugHtml()).append("</td></tr></table>");
        result.append("type=0x").append(Integer.toHexString(type)).append("<br>");
    }
}
