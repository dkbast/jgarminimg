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

import org.free.garminimg.utils.ImgConstants;

import java.io.EOFException;
import java.io.IOException;
import java.util.BitSet;

/**
 * A sub-file with .rgn extension. Contains the map points and lines.
 */
class RgnSubFile extends ImgSubFile
{
    private long dataOffset;

    private long dataLength;

    private static final int SEG_POS_POINT=0;

    private static final int SEG_POS_IPOINT=1;

    private static final int SEG_POS_POLYLINE=2;

    private static final int SEG_POS_POLYGON=3;

    public RgnSubFile(String filename, String filetype, int fileSize, int blocSize, ImgFileBag fileBag)
    {
        super(filename, filetype, fileSize, blocSize, fileBag);
    }

    public void init() throws IOException
    {
        FileContext context=new FileContext();
        superInit(context);
        seek(0x15, context);
        dataOffset=readUInt32(context);
        dataLength=readUInt32(context);
    }

    public void parseSubDivision(SubDivision subDivision, LblSubFile lbl, NetSubFile net, MapListener listener,
                                 int targetLevel, int objectKindFilter, BitSet objectTypeFilter, RgnContext rgnContext) throws IOException
    {
        Segment[] segments=getSegments(subDivision, rgnContext.context);
        if(segments==null)
            return;

        if((objectKindFilter&ObjectKind.POLYGON)!=0)
        {
            final Segment segment=segments[SEG_POS_POLYGON];
            if(segment!=null)
            {
                parsePoly(subDivision, net, segment, listener, false, targetLevel, objectTypeFilter, rgnContext);
            }
        }
        if((objectKindFilter&ObjectKind.POLYLINE)!=0)
        {
            final Segment segment=segments[SEG_POS_POLYLINE];
            if(segment!=null)
            {
                parsePoly(subDivision, net, segment, listener, true, targetLevel, objectTypeFilter, rgnContext);
            }
        }
        if((objectKindFilter&ObjectKind.POINT)!=0)
        {
            final Segment segment=segments[SEG_POS_POINT];
            if(segment!=null)
            {
                parsePoints(subDivision, segment, listener, false, targetLevel, objectTypeFilter, rgnContext.context);
            }
        }
        if((objectKindFilter&ObjectKind.INDEXED_POINT)!=0)
        {
            final Segment segment=segments[SEG_POS_IPOINT];
            if(segment!=null)
            {
                parsePoints(subDivision, segment, listener, true, targetLevel, objectTypeFilter, rgnContext.context);
            }
        }
    }

    public Label getIndexPointLabel(SubDivision subDivision, int index) throws IOException
    {
        FileContext context=new FileContext();
        Segment[] segments=getSegments(subDivision, context);
        if(segments==null)
            return null;
        Segment segment=segments[SEG_POS_IPOINT];
        if(segment!=null)
        {
            int cpt=0;
            seek(segment.segmentStart, context);
            while(getNextReadPos(context)<segment.segmentEnd)
            {
                int type=readByte(context);
                int info=readUInt24(context);
                boolean hasSubType=((info&0x800000)!=0); // The doc is wrong
                // in that area...
                boolean isPOI=((info&0x400000)!=0);
                int lblOffset=info&0x3FFFFF;
                int longitudeDelta=readInt16(context);
                int latitudeDelta=readInt16(context);
                int subType=0;
                if(hasSubType)
                {
                    subType=readByte(context);
                }

                if(++cpt==index)
                {
                    Label label=null;
                    if(lblOffset!=0)
                    {
                        int longitude=subDivision.getLongitude(longitudeDelta, 0);
                        int latitude=subDivision.getLongitude(latitudeDelta, 0);
                        if(isPOI)
                        {
                            label=new IndexedPOILabel(fileBag, getFilename(), lblOffset, type, subType, longitude, latitude);
                        }
                        else
                        {
                            label=new IndexedSimpleLabel(fileBag, getFilename(), lblOffset, type, subType, longitude, latitude);
                        }
                    }
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Parse all the poly[gons|lines] of the RGN and send the results to the listener.
     */
    private void parsePoly(SubDivision subDivision, NetSubFile net, Segment segment, MapListener listener, boolean line,
                           int targetLevel, BitSet objectTypeFilter, RgnContext rgnContext) throws IOException
    {
        final FileContext context=rgnContext.context;
        seek(segment.segmentStart, context);
        final BitStreamReader reader=new BitStreamReader();

        while(getNextReadPos(context)<segment.segmentEnd)
        {
            int info=readByte(context);
            int type;
            final boolean direction;
            if(line)
            {
                type=info&0x3F;
                direction=(info&0x40)!=0;
            }
            else
            {
                type=info&0x7F;
                direction=false;
            }
            boolean twoBytesLen=(info&0x80)!=0;

            info=readUInt24(context);
            final int labelOffset=info&0x3FFFFF;
            final boolean extraBit=(info&0x400000)!=0;
            final boolean dataInNet=(info&0x800000)!=0;
            final int longitudeDelta=readInt16(context);
            final int latitudeDelta=readInt16(context);
            final int bitStreamLen;
            if(twoBytesLen)
                bitStreamLen=readUInt16(context);
            else
                bitStreamLen=readByte(context);
            final int bitStreamInfo=readByte(context);

            final int curLevel=subDivision.getLevel();
            final int levelDiff=subDivision.getTre().getMaxPolyLevel(type, line);
            if(curLevel==0 || curLevel-levelDiff<targetLevel)
            {
                reader.reset(bitStreamLen);
                int nbPoints=0;

                int longSign=0;
                if(reader.readNextBits(1, context)!=0)
                { // constant signed
                    longSign=(reader.readNextBits(1, context)==0?+1:-1);
                }

                int latSign=0;
                if(reader.readNextBits(1, context)!=0)
                { // constant signed
                    latSign=(reader.readNextBits(1, context)==0?+1:-1);
                }

                int longExtraBit=0;
                int latExtraBit=0;
                if(extraBit)
                {
                    // I don't know... for some reason, only the longitude gets
                    // the extra bit... weird...
                    longExtraBit=1;
                }

                final int longBits=convertCoordinateLength(bitStreamInfo&0xF, longSign, longExtraBit);
                final int latBits=convertCoordinateLength(bitStreamInfo>>4, latSign, latExtraBit);

                int curLongPos=longitudeDelta;
                int curLatPos=latitudeDelta;

                // I guess, they would be stupid not to use the first point...
                rgnContext.longs[nbPoints]=subDivision.getLongitude(curLongPos, 0);
                rgnContext.lats[nbPoints]=subDivision.getLatitude(curLatPos, 0);
                nbPoints++;

                // increase the precision if needed
                curLongPos<<=longExtraBit;
                curLatPos<<=latExtraBit;

                while(reader.hasNext(longBits+latBits))
                {
                    int longOffset=reader.readCoordOffset(longBits, longSign, longExtraBit, context);
                    int latOffset=reader.readCoordOffset(latBits, latSign, latExtraBit, context);
                    curLongPos+=longOffset;
                    curLatPos+=latOffset;

                    rgnContext.checkCoordsSize(nbPoints);
                    rgnContext.longs[nbPoints]=subDivision.getLongitude(curLongPos, longExtraBit);
                    rgnContext.lats[nbPoints]=subDivision.getLatitude(curLatPos, latExtraBit);
                    nbPoints++;
                }
                reader.finish(context);

                Label label=null;
                if(labelOffset!=0)
                {
                    if(line && dataInNet)
                    {
                        if(net!=null)
                        {
                            label=new LineInNetLabel(fileBag, getFilename(), labelOffset);
                        }
                        else
                        {
                            // I don't know what to do, here...
                            label=new SimpleLabel(fileBag, getFilename(), labelOffset);
                        }
                    }
                    else
                    {
                        label=new SimpleLabel(fileBag, getFilename(), labelOffset);
                    }
                    label=translateLabel(type, label, line);
                }

                type=translateType(type, label, line);
                if(objectTypeFilter==null || objectTypeFilter.get(type))
                    listener.addPoly(type, rgnContext.longs, rgnContext.lats, nbPoints, label, line, direction);
            }
            else
            {
                seek(getNextReadPos(context)+bitStreamLen, context);
            }
        }
        if(getNextReadPos(context)>segment.segmentEnd)
        {
            throw new IOException("Bad poly* subDivision end: expected="+segment.segmentEnd+" actual="+getNextReadPos(context)
                                  +" index="+subDivision.getIndex());
        }
    }

    private Label translateLabel(int type, Label label, boolean line) throws IOException
    {
        if(label!=null && line)
        {
            if(type>=ImgConstants.MINOR_LAND_CONTOUR && type<=ImgConstants.MAJOR_DEPTH_CONTOUR)
            {   //contour lines are in feet
                //TODO: add some way to setup if we want feet or meters. For the moment, it's meters
                double feet=Double.parseDouble(label.getName());
                return new SimpleLabel(Integer.toString((int)Math.round(feet*0.3048/10)*10));
            }
        }
        return label;
    }

    private int translateType(int type, Label label, boolean line) throws IOException
    {
        switch(fileBag.getFamily())
        {
            case SWISS_TOPO:
                if(label==null) return type;
                if(line)
                {
                    if(type<=0x14 && label.getName().equals("TUNNEL"))
                        return type+ImgConstants.TUNNEL_SHIFT;
                    else if(type<=0x06 && label.getName().equals("RUINE"))
                        return ImgConstants.RUINS;
                }
                else
                {
                    if(type==0x13)
                    {
                        String name=label.getName();
                        if(name.equals("GARE") || name.equals("BAHNHOF"))
                            return ImgConstants.STATION_AREA;
                        else if((name.length()==8 && (name.startsWith("GRAVI") ||
                                                      name.startsWith("CARRI")) && name.endsWith("RE")) ||
                                                                                                        name.equals("KIESGRUBE"))
                            return ImgConstants.GRAVEL_AREA;

                    }
                    else
                    if(type==0x15 && ((label.getName().startsWith("FOR") && label.getName().length()==5 && label.getName().endsWith("T")) ||
                                      label.getName().equals("WALD")))
                    {
                        return ImgConstants.FOREST;
                    }
                }
                break;
        }
        return type;
    }

    private int convertCoordinateLength(int i, int sign, int extraBit)
    {
        int additionalLength=0;
        if(sign==0)
            additionalLength++;
        additionalLength+=extraBit;
        if(i<=9)
            return i+2+additionalLength;
        else
            return 2*i-9+2+additionalLength;
    }

    /**
     * Parse all the points of the RGN and send the results to the listener.
     */
    private void parsePoints(SubDivision subDivision, Segment segment, MapListener listener, boolean indexed, int targetLevel, BitSet objectTypeFilter, ImgSubFile.FileContext context) throws IOException
    {
        seek(segment.segmentStart, context);
        while(getNextReadPos(context)<segment.segmentEnd)
        {
            int type=readByte(context);
            int info=readUInt24(context);
            boolean hasSubType=((info&0x800000)!=0); // The doc is wrong
            // in that area...
            boolean isPOI=((info&0x400000)!=0);
            int lblOffset=info&0x3FFFFF;
            int longitudeDelta=readInt16(context);
            int latitudeDelta=readInt16(context);
            int subType=0;
            if(hasSubType)
            {
                subType=readByte(context);
            }

            int curLevel=subDivision.getLevel();
            int levelDiff=subDivision.getTre().getMaxPointLevel(type, subType);
            if((curLevel==0 || curLevel-levelDiff<targetLevel) &&
               (objectTypeFilter==null || objectTypeFilter.get(type)))
            {
                Label label=null;
                if(lblOffset!=0)
                {
                    if(isPOI)
                    {
                        label=new POILabel(fileBag, getFilename(), lblOffset);
                    }
                    else
                    {
                        label=new SimpleLabel(fileBag, getFilename(), lblOffset);
                    }
                }

                listener.addPoint(type, subType, subDivision.getLongitude(longitudeDelta, 0), subDivision.getLatitude(
                        latitudeDelta, 0), label, indexed);
            }
        }
        if(getNextReadPos(context)>segment.segmentEnd)
        {
            System.out.println("Bad segment end: expected="+segment.segmentEnd+" actual="+getNextReadPos(context));
        }
    }

    private Segment[] getSegments(SubDivision subDivision, ImgSubFile.FileContext context) throws IOException
    {
        long offset=subDivision.getDataOffset()+dataOffset;
        if(subDivision.getDataEnd()==0)
            subDivision.setDataEnd(dataLength);
        long end=subDivision.getDataEnd()+dataOffset;
        if(subDivision.getDataOffset()==0)
        {
            return null;
        }
        int nbTypes=subDivision.getNbObjectTypes();
        if(nbTypes==0)
        {
            return null;
        }

        Segment[] result=new Segment[8];
        int objectTypes=subDivision.getObjectTypes();

        if((objectTypes&ObjectKind.POINT)!=0)
        {
            result[SEG_POS_POINT]=new Segment(ObjectKind.POINT);
        }
        if((objectTypes&ObjectKind.INDEXED_POINT)!=0)
        {
            result[SEG_POS_IPOINT]=new Segment(ObjectKind.INDEXED_POINT);
        }
        if((objectTypes&ObjectKind.POLYLINE)!=0)
        {
            result[SEG_POS_POLYLINE]=new Segment(ObjectKind.POLYLINE);
        }
        if((objectTypes&ObjectKind.POLYGON)!=0)
        {
            result[SEG_POS_POLYGON]=new Segment(ObjectKind.POLYGON);
        }
        if((objectTypes&ObjectKind.UNKNOWN1)!=0)
        {
            System.out.println("Unknown object type: "+ObjectKind.UNKNOWN1);
            result[4]=new Segment(ObjectKind.UNKNOWN1);
        }
        if((objectTypes&ObjectKind.UNKNOWN2)!=0)
        {
            System.out.println("Unknown object type: "+ObjectKind.UNKNOWN2);
            result[5]=new Segment(ObjectKind.UNKNOWN2);
        }
        if((objectTypes&ObjectKind.UNKNOWN3)!=0)
        {
            System.out.println("Unknown object type: "+ObjectKind.UNKNOWN3);
            result[6]=new Segment(ObjectKind.UNKNOWN3);
        }
        if((objectTypes&ObjectKind.UNKNOWN4)!=0)
        {
            System.out.println("Unknown object type: "+ObjectKind.UNKNOWN4);
            result[7]=new Segment(ObjectKind.UNKNOWN4);
        }

        seek(offset, context);
        int nbPointers=nbTypes-1;
        long segmentStart=offset+nbPointers*2;
        int curType=0;
        for(int cpt=0; cpt<nbPointers; ++cpt)
        {
            while(result[curType]==null) curType++;
            long segmentEnd=readUInt16(context)+offset;
            if(segmentEnd>end)
            {
                System.out.println("WARNING: invalid segment end (too big)");
                return null;
            }
            if(segmentEnd<=segmentStart)
            {
                System.out.println("WARNING: invalid segment end (too small)");
                return null;
            }
            result[curType].setPosition(segmentStart, segmentEnd);
            segmentStart=segmentEnd;
            curType++;
        }
        while(result[curType]==null) curType++;
        result[curType].setPosition(segmentStart, end);
        return result;
    }

    private static class Segment
    {
        private long segmentStart;

        private long segmentEnd;

        private int type;

        public Segment(int type)
        {
            this.type=type;
        }

        public void setPosition(long segmentStart, long segmentEnd) throws IOException
        {
            if(segmentStart>segmentEnd)
                throw new IOException("A segment's end cannot be before it's start!");
            this.segmentStart=segmentStart;
            this.segmentEnd=segmentEnd;
        }
    }

    /**
     * Reads a series of bits from the RGN file regardless of the Bytes limits.
     */
    private class BitStreamReader
    {
        int length;

        int remainingBits;

        int curByte;

        public final void reset(int length)
        {
            this.length=length;
            remainingBits=0;
        }

        public final void finish(FileContext context) throws IOException
        {
            while(length>0)
            {
                readByte(context);
                --length;
            }

        }

        public final boolean hasNext(int nbBits)
        {
            return length*8+remainingBits>=nbBits;
        }

        public final int readCoordOffset(int nbBits, int sign, int extraBit, FileContext context) throws IOException
        {
            //For when the extrabit is set, I did a lot of experimenting to get to this solution.
            //I don't know if it's 100% correct, but it seems so.

            if(sign==0)
            {
                // variable sign value
                int value=readNextBits(nbBits, context);
                int signMask=1<<(nbBits-1);
                if((value&signMask)!=0)
                {
                    // negative
                    int comp=value^signMask;
                    if(extraBit==0)
                    {
                        if(comp!=0)
                            return comp-signMask;
                        else
                        {
                            //need to get an extra nbBits to know the value
                            int other=readCoordOffset(nbBits, sign, extraBit, context);
                            if(other<0)
                                return 1-value+other;  //negatif
                            else
                                return value-1+other;  //positif
                        }
                    }
                    else
                    {
                        if((comp&0xFFFFFE)!=0)   //the LSB doesn't count when extraBit is set
                        {
                            //simple negatif
                            return (comp&0xFFFFFE)-signMask/*+1-(comp&0x1)*/;
                        }
                        else
                        {
                            int other=readCoordOffset(nbBits-1, sign, 0, context);
                            if(other<0)
                                return 1-signMask+1/*-comp*/+(other<<1);   //negatif
                            else
                                return signMask-1/*+comp*/-1+(other<<1);   //positif
                        }
                    }
                }
                else
                {
                    if(extraBit>0)
                        return (value&0xFFFFFE)/*-1+(value&0x1)*/;
                    else
                        return value;
                }
            }
            else
            {
                // constant sign value
                int val=readNextBits(nbBits, context);
                if(extraBit>0)
                    return (((val>>>1)*sign)<<1)/*+(val&0x1)*/;
                else
                    return val*sign;
            }
        }

        public final int readNextBits(int toGet, FileContext context) throws IOException
        {
            int curPos=0;
            int result=0;
            do
            {
                getSomethingIfNeeded(context);
                final int remainingToGet=toGet-curPos;
                if(remainingToGet>=remainingBits)
                {
                    result|=curByte<<curPos;
                    curPos+=remainingBits;
                    remainingBits=0;
                }
                else
                {
                    int mask=(1<<remainingToGet)-1;
                    result|=(curByte&mask)<<curPos;
                    curByte>>>=remainingToGet;
                    remainingBits-=remainingToGet;
                    return result;
                }
            }
            while(curPos<toGet);
            return result;
        }

        private void getSomethingIfNeeded(FileContext context) throws IOException
        {
            if(remainingBits==0)
            {
                if(length==0)
                    throw new EOFException();
                remainingBits=8;
                length--;
                curByte=readByte(context);
            }
        }
    }
}
