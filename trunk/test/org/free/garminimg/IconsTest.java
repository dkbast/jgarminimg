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
import org.free.garminimg.utils.ImgConstants;

import java.io.IOException;

public class IconsTest extends TestCase
{
    public void testLoad() throws IOException
    {
        for(ImgConstants.PointDescription pointDescription : ImgConstants.getPointTypes().values())
        {
            if(pointDescription.getIconName()!=null)
                assertNotNull(pointDescription.getIcon());
        }
    }
}
