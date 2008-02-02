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

import junit.framework.TestCase;
import org.free.garminimg.ImgFileBag;
import org.free.garminimg.ImgSubFile;
import org.free.garminimg.TreSubFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AccessToLevelsTest extends TestCase
{
    public void testSimple() throws IOException
    {
        File file=new File("/home/patrick/gps/maps/00000002.img");
        if(!file.exists()) return;

        ImgFileBag imgFile=new ImgFileBag(file, null);
        TreSubFile tre=imgFile.getTreFile();
        ImgSubFile.FileContext context=new ImgSubFile.FileContext();

        long lockedPos=tre.getAbsolutePosition(tre.getLockedPos());
        int lockedVal=tre.getLocked();
        long levelsPos=tre.getAbsolutePosition(tre.getLevelsPos(context));
        long levelsLength=tre.getLevelsLength(context);
        int xor=imgFile.getXorByte();

        System.out.println("lockedPos="+lockedPos);
        System.out.println("lockedVal="+lockedVal);
        System.out.println("levelsPos="+levelsPos);
        System.out.println("levelsLength="+levelsLength);
        System.out.println("xor="+xor);

        //check that on the file directly

        RandomAccessFile randomAccess=new RandomAccessFile(file, "r");
        randomAccess.seek(lockedPos);
        int actualLockedVal=randomAccess.read()^xor;
        assertEquals(lockedVal, actualLockedVal);

        //just to show how to access the info for re-building the levels section
        boolean[] inheriteds=tre.getInheriteds();
        for(int level=tre.getMinLevel(); level<=tre.getMaxLevel(); ++level)
        {
            System.out.println("Level "+level+":");
            System.out.println("  bits="+tre.getResolution(level));
            System.out.println("  inherited="+inheriteds[level]);
            System.out.println("  nbSubDivisions="+tre.getNbSubDivisions(level));
        }
    }
}
