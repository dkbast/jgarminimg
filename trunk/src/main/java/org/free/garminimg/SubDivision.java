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

import org.pvalsecc.misc.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * A sub division. Represents a part of a map for a given zoom level.
 */
public class SubDivision
{
    private int level;

    private long dataOffset;

    private long dataEnd;

    private int objectTypes;

    private int longitudeCenter;

    private int latitudeCenter;

    private int width;

    private int height;

    private int nextLevel;

    private List<SubDivision> subDivisions=new ArrayList<SubDivision>();

    private int index;

    private TreSubFile subFile;

    public SubDivision(int index, int level, TreSubFile subFile)
    {
        this.index=index;
        this.level=level;
        this.subFile=subFile;
    }

    public boolean parse(TreSubFile file, ImgSubFile.FileContext context, int recordSize) throws IOException
    {
        dataOffset=file.readUInt24(context);
        objectTypes=file.readByte(context);
        longitudeCenter=file.readInt24(context);
        latitudeCenter=file.readInt24(context);
        width=file.readUInt16(context);
        boolean last=false;
        if((width&0x8000)!=0)
        {
            width&=0x7FFF;
            last=true;
        }
        height=file.readUInt16(context);
        if(recordSize>=16)
        {
            nextLevel=file.readUInt16(context);
        }
        subFile.registerRegionByIndex(index, this);
        return last;
    }

    public long getDataOffset()
    {
        return dataOffset;
    }

    public long getDataEnd()
    {
        return dataEnd;
    }

    public void setDataEnd(long dataEnd) throws IOException
    {
        if(dataEnd<dataOffset && dataEnd!=0)
            throw new IOException("A segment's end cannot be before it's start!");
        this.dataEnd=dataEnd;
    }

    private int getLatitudeHeight()
    {
        return convertMapUnits(height, 0);
    }

    public int getLongitudeWidth()
    {
        return convertMapUnits(width, 0);
    }

    public int getLatitudeCenter()
    {
        return latitudeCenter;
    }

    public int getLongitudeCenter()
    {
        return longitudeCenter;
    }

    public int getNextLevel()
    {
        return nextLevel;
    }

    public int getObjectTypes()
    {
        return objectTypes;
    }

    public int getNbObjectTypes()
    {
        int count=0;
        int cur=1;
        for(int cpt=0; cpt<8; ++cpt)
        {
            if((objectTypes&cur)!=0)
                ++count;
            cur=cur<<1;
        }
        return count;
    }

    private int convertMapUnits(int value, int additionalAccuracy)
    {
        return subFile.convertMapUnits(level, value, additionalAccuracy);
    }

    public List<SubDivision> getSubDivisions()
    {
        return subDivisions;
    }

    public boolean guessResolutions() throws IOException
    {
        if(level>0)
        {
            int minLat=latitudeCenter-getLatitudeHeight();
            int maxLat=latitudeCenter+getLatitudeHeight();
            int minLon=longitudeCenter-getLongitudeWidth();
            int maxLon=longitudeCenter+getLongitudeWidth();
            for(int cpt=0; cpt<subDivisions.size(); ++cpt)
            {
                SubDivision cur=subDivisions.get(cpt);
                if(!cur.guessResolutions())
                {
                    return false;
                }
                if(!cur.includedInCoordinates(minLon, maxLon, minLat, maxLat))
                {
                    subFile.halveResolution(level);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean checkResolutions()
    {
        if(level>0)
        {
            int minLat=latitudeCenter-getLatitudeHeight();
            int maxLat=latitudeCenter+getLatitudeHeight();
            int minLon=longitudeCenter-getLongitudeWidth();
            int maxLon=longitudeCenter+getLongitudeWidth();
            for(int cpt=0; cpt<subDivisions.size(); ++cpt)
            {
                SubDivision cur=subDivisions.get(cpt);
                if(!cur.checkResolutions())
                {
                    return false;
                }
                if(!cur.includedInCoordinates(minLon, maxLon, minLat, maxLat))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public void printDebug(PrintStream out) throws IOException
    {
        out.print("  "+level+": ");
        for(int cpt=subFile.getMinLevel(); cpt<subFile.getMaxLevel()-level; cpt++)
        {
            out.print("  ");
        }
        out.println("index="+index+" data="+dataOffset+" lat="+CoordUtils.toWGS84(latitudeCenter)+" lon="+CoordUtils.toWGS84(longitudeCenter)
                +" height(lat)=2*"+CoordUtils.toWGS84(getLatitudeHeight())+" width(long)=2*"+CoordUtils.toWGS84(getLongitudeWidth())
                +" types="+objectTypes+" next="+nextLevel);

        if(true || level>subFile.getMaxLevel()-2)
        {
            for(int cpt=0; cpt<subDivisions.size(); ++cpt)
            {
                subDivisions.get(cpt).printDebug(out);
            }
        }
    }

    public int getLongitude(int longitudeDelta, int additionalAccuracy)
    {
        return longitudeCenter+convertMapUnits(longitudeDelta, additionalAccuracy);
    }

    public int getLatitude(int latitudeDelta, int additionalAccuracy)
    {
        return latitudeCenter+convertMapUnits(latitudeDelta, additionalAccuracy);
    }

    private boolean matchesCoordinates(int minLong, int maxLong, int minLat, int maxLat)
    {
        return CoordUtils.matchesCoordinates(longitudeCenter-getLongitudeWidth(), longitudeCenter+getLongitudeWidth(),
                                             latitudeCenter-getLatitudeHeight(), latitudeCenter+getLatitudeHeight(), minLong, maxLong, minLat,
                                             maxLat);
    }

    public boolean includedInCoordinates(int minLong, int maxLong, int minLat, int maxLat)
    {
        return CoordUtils.includedInCoordinates(longitudeCenter-getLongitudeWidth(), longitudeCenter+getLongitudeWidth(),
                                                latitudeCenter-getLatitudeHeight(), latitudeCenter+getLatitudeHeight(),
                                                minLong, maxLong, minLat, maxLat);
    }

    public void readMap(int minLong, int maxLong, int minLat, int maxLat, int targetLevel, int objectKindFilter, BitSet objectTypeFilter, RgnSubFile rgn, LblSubFile lbl, NetSubFile net, MapListener listener, RgnContext rgnContext) throws IOException
    {
        if(level<targetLevel)
            return;
        if(!matchesCoordinates(minLong, maxLong, minLat, maxLat))
            return;

        if(dataOffset!=0 && level==targetLevel)
        {
            listener.startSubDivision(this);
            rgn.parseSubDivision(this, lbl, net, listener, targetLevel, objectKindFilter, objectTypeFilter, rgnContext);
            return;
        }

        for(int cpt=0; cpt<subDivisions.size(); ++cpt)
        {
            subDivisions.get(cpt).readMap(minLong, maxLong, minLat, maxLat, targetLevel, objectKindFilter,
                                          objectTypeFilter, rgn, lbl, net, listener, rgnContext);
        }
    }

    public int getResolution()
    {
        return subFile.getResolution(level);
    }

    public int getLevel()
    {
        return level;
    }

    public TreSubFile getTre()
    {
        return subFile;
    }

    public int getIndex()
    {
        return index;
    }
}
