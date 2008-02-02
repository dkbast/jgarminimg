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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One .img file.
 */
public class ImgFileBag
{
    private File file;

    private int blocSize;

    private LblSubFile lbl;

    private NetSubFile net;

    private RgnSubFile rgn;

    private TreSubFile tre;

    private Map<String, ImgSubFile> otherFiles=new HashMap<String, ImgSubFile>();

    private String description;

    private static int FAT_BLOC_SIZE=512;

    private ImgFileInputStream inputPrivate=null;

    private boolean inputLocked=false;

    private ImgFilesBag parent;

    private Family family=null;

    private boolean initDone=false;

    private boolean initBoundariesDone=false;

    /**
     * Keep a cache of used blocs. Simple implementation, there is room for improvement...
     */
    private HashMap<Long, SoftReference<byte[]>> blocCache=new HashMap();

    private ProductFile.MapDesc mapDescription=null;

    private int northBoundary;

    private int southBoundary;

    private int eastBoundary;

    private int westBoundary;

    public ImgFileBag(File file, ImgFilesBag parent) throws IOException
    {
        this.file=file;
        this.parent=parent;
    }

    private void init() throws IOException
    {
        if(!initDone)
        {
            System.out.println("reading file "+file.getName());
            ImgFileInputStream input=getInput();

            input.seek(0x49);
            description=input.readString(20);

            input.seek(0x61);
            int e1=input.readByte();
            int e2=input.readByte();
            blocSize=1<<(e1+e2);

            parseFat(input);
            if(family==null)
                family=guessFamily();
            releaseInput();
            initDone=true;
        }
    }

    private void parseFat(ImgFileInputStream input) throws IOException
    {
        input.seek(0x1FE);
        int endOfPartitions=input.readUInt16();
        if(endOfPartitions!=0xAA55)
        {
            throw new IOException("Bad end of partition table: 0x"+Integer.toHexString(endOfPartitions));
        }

        //jump over some un-interesting blocs (usually only one, but can be more)
        int interestingFatStart=0x200;
        while(true)
        {
            input.seek(interestingFatStart);
            int firstByte=input.readByte();
            if(firstByte==1)
            {
                break;
            }
            else
            {
                interestingFatStart+=FAT_BLOC_SIZE;
            }
        }

        //the first block with startByte==1 contains the length and other unknown stuff
        input.seek(interestingFatStart+0xc);
        int fatLength=input.readInt32()-interestingFatStart;
        if(fatLength<0)
        {
            throw new IOException("Invalid FAT length: "+fatLength);
        }
        interestingFatStart+=FAT_BLOC_SIZE;

        //the rest of the blocs contains the information to find the other files
        input.seek(interestingFatStart);
        final int nbBlocks=fatLength/FAT_BLOC_SIZE;
        for(int fatBlock=0; fatBlock<nbBlocks; fatBlock++)
        {
            input.seek(interestingFatStart+fatBlock*FAT_BLOC_SIZE);
            int firstByte=input.readByte();
            if(firstByte==0x1)
            {
                String filename=input.readString(8);
                String filetype=input.readString(3);
                String fullFilename=filename+"."+filetype;
                int fileSize=input.readInt32();
                int partNumber=input.readUInt16();

                ImgSubFile subFile;
                if(partNumber==0)
                {
                    subFile=ImgSubFile.create(filename, filetype, fileSize, blocSize, this);
                    addFile(filetype, subFile);
                }
                else
                {
                    subFile=getFile(filetype);
                    if(subFile==null)
                    {
                        throw new IOException("Unknown sub-file '"+fullFilename+"' with partNumber="+partNumber);
                    }
                }

                // read the list of blocs
                input.seek(interestingFatStart+fatBlock*FAT_BLOC_SIZE+0x20);
                int count=0;
                int lastBloc;
                do
                {
                    lastBloc=input.readUInt16();
                    if(lastBloc!=0xFFFF)
                        subFile.addBloc(lastBloc);
                }
                while((++count)<240 && lastBloc!=0xFFFF);
            }
            else
            {
                /*if(fatBlock+1!=nbBlocks)
                    System.out.println("WARNING: Premature end of FAT table: "+fatBlock+"+1!="+nbBlocks);
                break;*/
            }
        }

        foreachFile(new FileVisitor()
        {
            public void file(ImgSubFile file) throws IOException
            {
                file.init();
            }
        });
    }

    public int guessLowestNbBits(int minLong, int maxLong, int minLat, int maxLat) throws IOException
    {
        initBoundaries();
        if(tre==null)
        {
            if(!containsCoordinates(minLong, maxLong, minLat, maxLat))
                return 0;
            else
                //guess work to avoid doing a fullInit on every files.
                return 24-(int)(Math.log((eastBoundary-westBoundary)/15.0)/Math.log(2.0));
        }
        else
        {
            return tre.guessLowestNbBits();
        }
    }

    public String getDescription()
    {
        return description;
    }

    public synchronized byte[] getBloc(long pos, long blocSize) throws IOException
    {
        SoftReference<byte[]> reference=blocCache.get(pos);
        byte[] result;
        if(reference==null || (result=reference.get())==null)
        {
            ImgFileInputStream input=getInput();
            input.seek(pos);
            result=new byte[(int)blocSize];
            input.readBloc(result);
            blocCache.put(pos, new SoftReference<byte[]>(result));
            releaseInput();
        }
        return result;
    }

    public long getFullSurface() throws IOException
    {
        initBoundaries();
        if(tre!=null)
            return tre.getFullSurface();
        else
            return ((long)northBoundary-southBoundary)*((long)eastBoundary-westBoundary);
    }

    private void initBoundaries() throws IOException
    {
        if(!initBoundariesDone)
        {
            mapDescription=parent!=null?parent.getMapDescription(this):null;
            if(mapDescription!=null)
            {
                northBoundary=mapDescription.getCoordN();
                southBoundary=mapDescription.getCoordS();
                eastBoundary=mapDescription.getCoordE();
                westBoundary=mapDescription.getCoordW();
            }
            else
            {
                init();
            }
            initBoundariesDone=true;
        }
    }

    private interface FileVisitor
    {
        void file(ImgSubFile file) throws IOException;
    }

    private void foreachFile(FileVisitor visitor) throws IOException
    {
        if(lbl!=null)
            visitor.file(lbl);
        if(rgn!=null)
            visitor.file(rgn);
        if(net!=null)
            visitor.file(net);
        if(tre!=null)
            visitor.file(tre);

        for(String subFileName : otherFiles.keySet())
        {
            ImgSubFile subFile=otherFiles.get(subFileName);
            visitor.file(subFile);
        }
    }

    public void printDebug(final PrintStream out) throws IOException
    {
        init();
        System.out.println("Description: "+description);
        System.out.println("Bloc size: "+blocSize);

        foreachFile(new FileVisitor()
        {
            public void file(ImgSubFile file) throws IOException
            {
                file.printDebug(out);
                out.println();
            }
        });
    }

    private void addFile(String filetype, ImgSubFile subFile)
    {
        if("RGN".equals(filetype))
            rgn=(RgnSubFile)subFile;
        else if("TRE".equals(filetype))
            tre=(TreSubFile)subFile;
        else if("LBL".equals(filetype))
            lbl=(LblSubFile)subFile;
        else if("NET".equals(filetype))
            net=(NetSubFile)subFile;
        else
            otherFiles.put(filetype, subFile);
    }

    private ImgSubFile getFile(String filetype)
    {
        if("RGN".equals(filetype))
            return rgn;
        else if("TRE".equals(filetype))
            return tre;
        else if("LBL".equals(filetype))
            return lbl;
        else if("NET".equals(filetype))
            return net;
        else
            return otherFiles.get(filetype);
    }

    public RgnSubFile getRgnFile() throws IOException
    {
        init();
        return rgn;
    }

    public TreSubFile getTreFile() throws IOException
    {
        init();
        return tre;
    }

    public LblSubFile getLblFile() throws IOException
    {
        init();
        return lbl;
    }

    public NetSubFile getNetFile() throws IOException
    {
        init();
        return net;
    }

    private boolean containsCoordinates(int minLong, int maxLong, int minLat, int maxLat) throws IOException
    {
        initBoundaries();
        if(tre!=null)
            return tre.matchesCoordinates(minLong, maxLong, minLat, maxLat);
        else
            return CoordUtils.matchesCoordinates(westBoundary, eastBoundary, southBoundary, northBoundary, minLong,
                                                 maxLong, minLat, maxLat);
    }

    public boolean containsCoordinate(int longitude, int latitude) throws IOException
    {
        init();
        return tre!=null && tre.matchesCoordinate(longitude, latitude);
    }

    public void readMap(int minLong, int maxLong, int minLat, int maxLat, int resolution, int objectKindFilter, BitSet objectTypeFilter, MapListener listener) throws IOException
    {
        if(containsCoordinates(minLong, maxLong, minLat, maxLat))
        {
            init();
            listener.startMap(this);
            if(tre!=null)
            {
                tre.readMap(minLong, maxLong, minLat, maxLat, resolution, objectKindFilter, objectTypeFilter, rgn, lbl, net, listener);
            }
        }
    }

    public File getFile()
    {
        return file;
    }

    private synchronized ImgFileInputStream getInput() throws IOException
    {
        if(inputPrivate==null)
        {
            if(parent!=null)
                ImgFilesBag.registerOpenFile(this);
            inputPrivate=new ImgFileInputStream(file);
        }
        inputLocked=true;
        return inputPrivate;
    }

    private synchronized void releaseInput()
    {
        inputLocked=false;
    }

    public synchronized boolean close() throws IOException
    {
        if(!inputLocked)
        {
            inputPrivate.close();
            inputPrivate=null;
            return true;
        }
        return false;
    }

    public enum Family
    {
        SWISS_TOPO,
        EURO_METRO_GUIDE7,
        EURO_METRO_GUIDE8,
        BASE_MAP,
        C_GPS_MAPPER,
        WORLD_MAP,
        UNKNOWN
    }

    public Family getFamily()
    {
        return family;
    }

    public void setFamily(Family family)
    {
        this.family=family;
    }

    private static final Pattern familyPattern=Pattern.compile("^(I?)([\\dabcdef]+)$", Pattern.CASE_INSENSITIVE);

    private Family guessFamily()
    {
        if(tre!=null)
        {
            Matcher matcher=familyPattern.matcher(tre.getFilename());
            if(matcher.matches())
            {
                if(matcher.group(1).equals("I"))
                {
                    int number=Integer.valueOf(matcher.group(2), 16);
                    if(number>=0x0665800 && number<=0x06658F7)
                        return Family.SWISS_TOPO;
                    else if(number>=0x04BF27C && number<=0x04BFBC4)
                        return Family.EURO_METRO_GUIDE7;
                    else if(number>=0x04DCC41 && number<=0x04DD727)
                        return Family.EURO_METRO_GUIDE8;
                    else if((number>=0x0002584 && number<=0x00026E7) ||
                            (number>=0x000296F && number<=0x0002BCE) ||
                            (number>=0x0004280 && number<=0x00042E6) ||
                            (number>=0x00045ED && number<=0x0004757) ||
                            (number>=0x0004B77 && number<=0x0004D3C) ||
                            (number>=0x0008A1B && number<=0x0009214) ||
                            (number>=0x020105A && number<=0x02010AA))
                        return Family.WORLD_MAP;
                }
                else
                {
                    //seems to be stuff generated with cGPSMapper
                    return Family.C_GPS_MAPPER;
                }
            }
        }
        System.out.println("Unknown map familly for: "+tre.getFilename());
        return Family.UNKNOWN;
    }

    public int getNorthBoundary() throws IOException
    {
        initBoundaries();
        if(tre!=null) return tre.getNorthBoundary();
        return northBoundary;
    }

    public int getSouthBoundary() throws IOException
    {
        initBoundaries();
        if(tre!=null) return tre.getSouthBoundary();
        return southBoundary;
    }

    public int getEastBoundary() throws IOException
    {
        initBoundaries();
        if(tre!=null) return tre.getEastBoundary();
        return eastBoundary;
    }

    public int getWestBoundary() throws IOException
    {
        initBoundaries();
        if(tre!=null) return tre.getWestBoundary();
        return westBoundary;
    }

    public int getXorByte() throws IOException
    {
        int result=getInput().getXor();
        releaseInput();
        return result;
    }
}
