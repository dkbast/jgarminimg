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

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * A sub-file with .tre extension. Contains the information about the sub-divisions
 * and the boundary of the map.
 */
public class TreSubFile extends ImgSubFile
{
    private static final int SMALL_DIVISION_RECORD=14;

    private static final int FULL_DIVISION_RECORD=16;

    private int northBoundary;

    private int eastBoundary;

    private int southBoundary;

    private int westBoundary;

    private List<SubDivision> subDivisions=new ArrayList<SubDivision>();

    private int lastBigIndex=0;

    private int maxLevel;

    private int maxLevelWithData=0;

    private int minLevel;

    private static int MAX_RESOLUTION=24;

    private int bitsPerCoords[]=
            {
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION,
                    MAX_RESOLUTION};

    private boolean inheriteds[]=new boolean[16];

    private Map<Integer/* type */, Integer/* max level */> polylineTypes=new HashMap<Integer, Integer>();

    private Map<Integer/* type */, Integer/* max level */> polygonTypes=new HashMap<Integer, Integer>();

    private Map<Integer/* type/subType */, Integer/* max level */> pointTypes=new HashMap<Integer, Integer>();

    private ArrayList<SubDivision> subDivisionsByIndex=new ArrayList<SubDivision>();

    private boolean guessLevels;

    private boolean initDone=false;

    private long fullSurface;

    public TreSubFile(String filename, String filetype, int fileSize, int blocSize, ImgFileBag fileBag)
    {
        super(filename, filetype, fileSize, blocSize, fileBag);
    }

    public void init() throws IOException
    {
        FileContext context=new FileContext();
        superInit(context);
        seek(0x15, context);
        northBoundary=readInt24(context);
        eastBoundary=readInt24(context);
        southBoundary=readInt24(context);
        westBoundary=readInt24(context);
        fullSurface=((long)northBoundary-southBoundary)*((long)eastBoundary-westBoundary);
    }

    private void fullInit() throws IOException
    {
        FileContext context=new FileContext();
        parseLevels(context);
        parseSubDivisions(context);
        parsePolies(0x4A, polylineTypes, context);
        parsePolies(0x58, polygonTypes, context);
        parsePoints(0x66, context);
        if(headerLength>=0x7C+4+4+2)
        {
            parseTRE7(0x7C, context);
        }

        System.out.println(fileBag.getDescription()+": max="+maxLevel+" maxData="+maxLevelWithData);
    }

    private void parseTRE7(int infoOffset, FileContext context) throws IOException
    {
        seek(infoOffset, context);
        long offset=readUInt32(context);
        long length=readUInt32(context);
        long size=readUInt16(context);

        //I don't know what's in it...
    }

    /**
     * Check if the subDivisions needs to be read and read them if yes.
     */
    private synchronized void initIfNeeded() throws IOException
    {
        if(!initDone)
        {
            fullInit();
            initDone=true;
        }
    }

    private void parsePolies(int infoOffset, Map<Integer, Integer> target, FileContext context) throws IOException
    {
        seek(infoOffset, context);
        long offset=readUInt32(context);
        long length=readUInt32(context);
        long size=readUInt16(context);

        int read=0;
        while(read+size<=length)
        {
            seek(offset+read, context);
            int type=readByte(context);
            int maxLevel=readByte(context);
            target.put(type, maxLevel);
            read+=size;
        }
    }

    private void parsePoints(int infoOffset, FileContext context) throws IOException
    {
        seek(infoOffset, context);
        long offset=readUInt32(context);
        long length=readUInt32(context);
        long size=readUInt16(context);

        int read=0;
        while(read+size<=length)
        {
            seek(offset+read, context);
            int type=readByte(context);
            int maxLevel=readByte(context);
            int subType=readByte(context);
            pointTypes.put(type<<8|subType, maxLevel);
            read+=size;
        }
    }

    public long getLevelsPos(FileContext context) throws IOException
    {
        seek(0x21, context);
        return readUInt32(context);
    }

    public long getLevelsLength(FileContext context) throws IOException
    {
        seek(0x25, context);
        return readUInt32(context);
    }

    private void parseLevels(FileContext context) throws IOException
    {
        seek(0x21, context);
        long levelsOffset=readUInt32(context);
        long levelsLength=readUInt32(context);

        if(getLocked()==0)
        {
            seek(levelsOffset, context);
            maxLevel=0;
            minLevel=16;
            while(getNextReadPos(context)<levelsOffset+levelsLength)
            {
                int zoom=readByte(context);
                int bitsPerCoord=readByte(context);
                int subDivisions=readUInt16(context);  //don't care
                int level=zoom&0xF;
                boolean inherited=((zoom&0x80)!=0);
                if(maxLevel<level)
                    maxLevel=level;
                if(minLevel>level)
                    minLevel=level;
                bitsPerCoords[level]=bitsPerCoord;
                inheriteds[level]=inherited;
            }
            guessLevels=false;
        }
        else
        {
            final boolean allInherited;
            minLevel=0;
            maxLevel=(int)(levelsLength/4)-1;
            switch(fileBag.getFamily())
            {
                case SWISS_TOPO:
                    bitsPerCoords[0]=24;
                    bitsPerCoords[1]=22;
                    bitsPerCoords[2]=20;
                    bitsPerCoords[3]=18;
                    bitsPerCoords[4]=16;
                    bitsPerCoords[5]=15;

                    if(levelsLength==24)
                    {
                        maxLevel=5;
                        guessLevels=false;
                    }
                    else if(levelsLength==28)
                    {
                        maxLevel=6;
                        bitsPerCoords[6]=14;
                        guessLevels=false;
                    }
                    else if(levelsLength==32)
                    {
                        maxLevel=7;
                        bitsPerCoords[6]=14;
                        bitsPerCoords[7]=13;
                        guessLevels=false;
                    }
                    else
                    {
                        throw new IOException("Don't know any swiss maps with levelsLength="+levelsLength);
                    }
                    allInherited=false;
                    break;

                case C_GPS_MAPPER:
                    guessLevels=true;
                    allInherited=false;
                    break;

                default:
                    guessLevels=true;
                    allInherited=false;   //better safe than sorry ;-)
            }

            for(int cpt=minLevel; cpt<=maxLevel; ++cpt)
            {
                inheriteds[cpt]=allInherited;
            }
        }
    }

    private void parseSubDivisions(FileContext context) throws IOException
    {
        seek(0x29, context);
        long subDivisionOffset=readUInt32(context);
        long subDivisionLength=readUInt32(context);

        parseSubDivision(subDivisionOffset, subDivisionLength, subDivisions, maxLevel, 1, false, context);
        parseSubDivision(subDivisionOffset, subDivisionLength, subDivisions, maxLevel, 1, true, context);

        adjustLevels();
        adjustSubDivisions();
    }

    /**
     * Try to guess the resolutions of the different levels using the assumption that
     * sub-levels must fit within their englobing level.
     * @throws IOException
     */
    private void adjustLevels() throws IOException
    {
        if(guessLevels)
        {
            System.out.println("Guessing the levels (locked map)");

            guessMaxResolution();

            do
            {
                boolean success=true;
                for(int cpt=0; cpt<subDivisions.size() && success; ++cpt)
                {
                    SubDivision cur=subDivisions.get(cpt);
                    success=cur.guessResolutions();
                }
                if(success)
                    return;
            }
            while(bitsPerCoords[maxLevel]>1);
            throw new IOException("Locked file and unable to guess the levels' resolutions (inconsistent levels)");
        }
        else
        {
            for(int cpt=0; cpt<subDivisions.size(); ++cpt)
            {
                SubDivision cur=subDivisions.get(cpt);
                if(!cur.checkResolutions())
                {
                    System.out.println("WARNING: Bad boundaries");
                }
            }
        }
    }

    /**
     * Guess what is the maximum resolution of the lowest level (highest details).
     * @throws IOException If it failed
     */
    private void guessMaxResolution() throws IOException
    {
        for(int maxResolution=10; maxResolution<=24; ++maxResolution)
        {
            for(int cpt=minLevel; cpt<=maxLevel; ++cpt)
            {
                bitsPerCoords[cpt]=maxResolution;
            }

            boolean ok=true;
            for(int cpt=1; cpt<subDivisionsByIndex.size() && ok; ++cpt)
            {
                SubDivision cur=subDivisionsByIndex.get(cpt);
                if(cur.getLevel()==minLevel)
                {
                    int tolerance=cur.getLongitudeWidth()/2;  //there are some weird imprecision in the sub-divisions boundaries
                    ok=cur.includedInCoordinates(westBoundary-tolerance, eastBoundary+tolerance, southBoundary-tolerance, northBoundary+tolerance);
                }
            }
            if(ok) return;
        }
        System.out.println("Locked map and cannot guess maximum resolution, fallback to 24 bits");
    }

    public int convertMapUnits(int level, int value, int additionalAccuracy)
    {
        int shift=24-getResolution(level)-additionalAccuracy;
        if(shift>=0)
            return value<<shift;
        else
            return value>>-shift;
    }

    private void adjustSubDivisions() throws IOException
    {
        for(int cpt=1; cpt<subDivisionsByIndex.size()-1; ++cpt)
        {
            if(subDivisionsByIndex.get(cpt+1)==null)
            {
                throw new IOException("Empty region #"+(cpt+1));
            }
            SubDivision curRegion=subDivisionsByIndex.get(cpt);
            curRegion.setDataEnd(subDivisionsByIndex.get(cpt+1).getDataOffset());
        }
        subDivisionsByIndex.get(subDivisionsByIndex.size()-1).setDataEnd(0);
    }

    private void parseSubDivision(long subDivisionOffset, long subDivisionLength, List<SubDivision> curSubDivisions,
                                  int level, int index, boolean onlyLast, FileContext context) throws IOException
    {
        if(index==0)
        {
            return;
        }
        final int recordSize;
        long pos;
        if(level>minLevel)
        {
            recordSize=FULL_DIVISION_RECORD;
            pos=subDivisionOffset+(index-1)*FULL_DIVISION_RECORD;
        }
        else
        {
            if(!onlyLast)
                return;
            recordSize=SMALL_DIVISION_RECORD;
            pos=subDivisionOffset+(lastBigIndex)*FULL_DIVISION_RECORD+(index-lastBigIndex-1)*SMALL_DIVISION_RECORD;
        }

        if((!onlyLast && level>minLevel) || (onlyLast && level==minLevel))
        {
            seek(pos, context);
            while(getNextReadPos(context)<subDivisionOffset+subDivisionLength)
            {
                SubDivision cur=new SubDivision(index, level, this);
                boolean last=cur.parse(this, context, recordSize);
                curSubDivisions.add(cur);
                if(cur.getDataOffset()!=0)
                    maxLevelWithData=Math.max(maxLevelWithData, level);
                if(last)
                {
                    break;
                }
                index++;
            }

            if(recordSize==FULL_DIVISION_RECORD && lastBigIndex<index)
            {
                lastBigIndex=index;
            }
        }

        for(int cpt=0; cpt<curSubDivisions.size(); ++cpt)
        {
            SubDivision cur=curSubDivisions.get(cpt);
            parseSubDivision(subDivisionOffset, subDivisionLength, cur.getSubDivisions(), level-1, cur.getNextLevel(),
                             onlyLast, context);
        }
    }

    public void printDebug(PrintStream out) throws IOException
    {
        super.printDebug(out);
        initIfNeeded();
        out.println("  northBoundary="+northBoundary);
        out.println("  eastBoundary="+eastBoundary);
        out.println("  southBoundary="+southBoundary);
        out.println("  westBoundary="+westBoundary);

        for(int cpt=0; cpt<subDivisions.size(); ++cpt)
        {
            SubDivision cur=subDivisions.get(cpt);
            cur.printDebug(out);
        }

        out.println("  Level bitsPerCoords:");
        for(int cpt=maxLevel; cpt>=minLevel; --cpt)
        {
            out.println("    "+cpt+" res="+getResolution(cpt));
        }

        out.println("  Polyline types:");
        for(Integer type : polylineTypes.keySet())
        {
            out.println("    "+type+": maxLevel="+polylineTypes.get(type));
        }

        out.println("  Polygon types:");
        for(Integer type : polygonTypes.keySet())
        {
            out.println("    "+type+": maxLevel="+polygonTypes.get(type));
        }

        out.println("  Point Types:");
        for(Integer type : pointTypes.keySet())
        {
            out.println("    "+(type>>8)+"/"+(type&0xFF)+": maxLevel="+pointTypes.get(type));
        }
    }

    public int getResolution(int level)
    {
        return bitsPerCoords[level];
    }

    public void halveResolution(int level) throws IOException
    {
        bitsPerCoords[level]-=1;
        if(bitsPerCoords[level]<=0)
        {
            throw new IOException("Cannot guess resolution for level "+level+": "+getResolutionsDesc());
        }
    }

    private String getResolutionsDesc()
    {
        StringBuffer buffer=new StringBuffer();
        for(int cpt=maxLevel; cpt>=minLevel; --cpt)
        {
            if(cpt<maxLevel)
                buffer.append(", ");
            buffer.append(cpt).append(':').append(bitsPerCoords[cpt]);
        }
        return buffer.toString();
    }

    public int getMaxLevel() throws IOException
    {
        initIfNeeded();
        return maxLevel;
    }

    public int getMinLevel() throws IOException
    {
        initIfNeeded();
        return minLevel;
    }

    public void registerRegionByIndex(int index, SubDivision division)
    {
        while(subDivisionsByIndex.size()<=index)
            subDivisionsByIndex.add(null);
        subDivisionsByIndex.set(index, division);
    }

    public SubDivision getSubDivision(int i) throws IOException
    {
        initIfNeeded();
        return subDivisionsByIndex.get(i);
    }

    public boolean matchesCoordinates(int minLong, int maxLong, int minLat, int maxLat)
    {
        return CoordUtils.matchesCoordinates(westBoundary, eastBoundary, southBoundary, northBoundary, minLong,
                                             maxLong, minLat, maxLat);
    }

    public boolean matchesCoordinate(int longitude, int latitude)
    {
        return CoordUtils.includedInCoordinates(longitude, latitude, westBoundary, eastBoundary, southBoundary, northBoundary);
    }

    public void readMap(int minLong, int maxLong, int minLat, int maxLat, int resolution, int objectKindFilter, BitSet objectTypeFilter, RgnSubFile rgn, LblSubFile lbl, NetSubFile net, MapListener listener) throws IOException
    {
        initIfNeeded();
        int targetMinLevel;
        int targetMaxLevel;
        if(resolution>=0)
        {
            targetMinLevel=guessLevel(resolution);
            targetMaxLevel=findMaxToDisplay(targetMinLevel);
        }
        else
        {
            targetMinLevel=minLevel;
            targetMaxLevel=maxLevel;
        }
        RgnContext rgnContext=new RgnContext();
        for(int level=targetMaxLevel; level>=targetMinLevel; --level)
        {
            for(int cpt=0; cpt<subDivisions.size(); ++cpt)
            {
                SubDivision cur=subDivisions.get(cpt);
                cur.readMap(minLong, maxLong, minLat, maxLat, level, objectKindFilter, objectTypeFilter, rgn, lbl, net, listener, rgnContext);
            }
        }
    }

    private int findMaxToDisplay(int targetMinLevel)
    {
        for(int cpt=targetMinLevel; cpt<=maxLevelWithData; cpt++)
        {
            if(!inheriteds[cpt])
                return cpt;
        }
        return maxLevel;
    }

    private int guessLevel(long resolution)
    {
        int targetBits=24-(int)Math.round(Math.log(resolution)/Math.log(2));

        int minDist=Integer.MAX_VALUE;
        int result=0;
        for(int cpt=minLevel; cpt<=maxLevelWithData; ++cpt)
        {
            int dist=Math.abs(bitsPerCoords[cpt]-targetBits);
            if(dist<minDist)
            {
                minDist=dist;
                result=cpt;
            }
        }
        return result;
    }

    private int getMaxPolylineLevel(int type)
    {
        Integer result=polylineTypes.get(type);
        if(result!=null)
            return result;
        else
            return 0;
    }

    private int getMaxPolygonLevel(int type)
    {
        Integer result=polygonTypes.get(type);
        if(result!=null)
            return result;
        else
            return 0;
    }

    public int getMaxPointLevel(int type, int subType)
    {
        Integer result=pointTypes.get((type<<8)|subType);
        if(result!=null)
            return result;
        else
            return 24;
    }

    public int getMaxPolyLevel(int type, boolean line)
    {
        if(line)
        {
            return getMaxPolylineLevel(type);
        }
        else
        {
            return getMaxPolygonLevel(type);
        }

    }

    public long getFullSurface()
    {
        return fullSurface;
    }

    public int getEastBoundary()
    {
        return eastBoundary;
    }

    public int getNorthBoundary()
    {
        return northBoundary;
    }

    public int getSouthBoundary()
    {
        return southBoundary;
    }

    public int getWestBoundary()
    {
        return westBoundary;
    }

    public int guessLowestNbBits()
    {
        if(initDone)
            return bitsPerCoords[maxLevel];
        else
            //guess work to avoid doing a fullInit on every files.
            return 24-(int)(Math.log((eastBoundary-westBoundary)/15.0)/Math.log(2.0));
    }

    public boolean[] getInheriteds() throws IOException
    {
        initIfNeeded();
        return inheriteds;
    }

    /**
     * @param level
     * @return The number of sub-divisions for the given level.
     */
    public int getNbSubDivisions(int level)
    {
        int result=0;
        for(int i=0; i<subDivisionsByIndex.size(); i++)
        {
            SubDivision subDivision=subDivisionsByIndex.get(i);
            if(subDivision!=null && subDivision.getLevel()==level)
                result++;
        }
        return result;
    }
}
