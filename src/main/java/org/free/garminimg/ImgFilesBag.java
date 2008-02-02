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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main entry point of the library. Manages the list of .img mapFiles.
 * <p>Instances of this class are supposed to be fully thread safe.
 */
public class ImgFilesBag
{
    private static final int MAX_OPEN_FILES=10;

    /**
     * We cannot have too many files open at the same time (OS limitation) this list keeps the list of files open.
     * <p>Every usages must be synchronised manually.
     */
    private static List<ImgFileBag> openFiles=new ArrayList<ImgFileBag>();

    private SortedSet<ImgFileBag> mapFiles=Collections.synchronizedSortedSet(new TreeSet<ImgFileBag>(new FileComparator()));

    private SortedSet<ImgFileBag> baseMapFiles=Collections.synchronizedSortedSet(new TreeSet<ImgFileBag>(new FileComparator()));

    private static final Pattern normalMapFilename=Pattern.compile("^\\d+\\.img$", Pattern.CASE_INSENSITIVE);

    private List<ProductFile> products=new ArrayList();

    /**
     * Add a single .img file to the repository.
     */
    public void addFile(File file) throws IOException
    {
        if(file.getName().toLowerCase().endsWith(".tdb"))
        {
            ProductFile product=new ProductFile(file);
            products.add(product);
        }
        else
        {
            ImgFileBag toAdd=new ImgFileBag(file, this);
            Matcher matcher=normalMapFilename.matcher(file.getName());
            if(matcher.matches())
                mapFiles.add(toAdd);
            else
            {
                toAdd.setFamily(ImgFileBag.Family.BASE_MAP);
                baseMapFiles.add(toAdd);
            }
        }
    }

    /**
     * Add every .img mapFiles of the given directory. Does not go recursively in sub-directories.
     */
    public void addDirectory(File directory) throws IOException
    {
        if(!directory.isDirectory())
        {
            System.out.println(directory+" is not a directory");
            return;
        }

        //first the TDB files
        String[] list=directory.list(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".tdb");
            }
        });
        for(int cpt=0; cpt<list.length; ++cpt)
        {
            addFile(new File(directory, list[cpt]));
        }

        //then the IMG files
        list=directory.list(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(".img");
            }
        });
        for(int cpt=0; cpt<list.length; ++cpt)
        {
            addFile(new File(directory, list[cpt]));
        }
    }

    /**
     * Remove every maps.
     */
    public synchronized void clear() throws IOException
    {
        baseMapFiles.clear();
        mapFiles.clear();

        for(ImgFileBag openFile : openFiles)
        {
            openFile.close();
        }
        openFiles.clear();
    }

    /**
     * Read the map for the given coordinates.<br>
     * This method will try to have the objects sorted in an order that is suitable for drawing.
     * @param resolution       The resolution in "Garmin coordinate" units of the details we want. The smaller, the higher the details will be. If negative gets all the details.
     * @param listener         A visitor that will be called for every map objects.
     * @param objectKindFilter Bitset of what kind of object to concider (see ObjectKind for possible values).
     * @see #readMap(int,int,int,int,int,int,BitSet,MapListener)
     */
    public void readMapForDrawing(int minLong, int maxLong, int minLat, int maxLat, int resolution, int objectKindFilter, MapListener listener) throws IOException
    {
        if((objectKindFilter&ObjectKind.BASE_MAP)!=0)
            readMapForDrawing(minLong, maxLong, minLat, maxLat, resolution, listener, baseMapFiles, objectKindFilter);
        if((objectKindFilter&ObjectKind.NORMAL_MAP)!=0 && !wantOnlyBaseMaps(minLong, maxLong, minLat, maxLat, resolution))
        {
            readMapForDrawing(minLong, maxLong, minLat, maxLat, resolution, listener, mapFiles, objectKindFilter);
        }
        listener.finishPainting();
    }

    private void readMapForDrawing(int minLong, int maxLong, int minLat, int maxLat, int resolution, MapListener listener, SortedSet<ImgFileBag> files, int objectKindFilter) throws IOException
    {
        //it's very important to read polygons first, to avoid hiding other objects.
        if((objectKindFilter&ObjectKind.POLYGON)!=0)
        {
            //first, the map background
            readMap(minLong, maxLong, minLat, maxLat, resolution, ObjectKind.POLYGON, getMapBackgroundFilter(), listener, files);

            //then, the city limits
            readMap(minLong, maxLong, minLat, maxLat, resolution, ObjectKind.POLYGON, getMapCityFilter(), listener, files);

            //then, the definition of small zones
            readMap(minLong, maxLong, minLat, maxLat, resolution, ObjectKind.POLYGON, getMapZonesFilter(), listener, files);

            //then, the forests
            readMap(minLong, maxLong, minLat, maxLat, resolution, ObjectKind.POLYGON, getMapForestFilter(), listener, files);

            //finally, the rest
            readMap(minLong, maxLong, minLat, maxLat, resolution, ObjectKind.POLYGON, getMapOthersFilter(), listener, files);
        }

        //lines and points can be read in any order.
        if(objectKindFilter!=ObjectKind.POLYGON)
            readMap(minLong, maxLong, minLat, maxLat, resolution, (ObjectKind.ALL^ObjectKind.POLYGON)&objectKindFilter, null, listener, files);
    }

    /**
     * Read the map for the given coordinates.
     * @param resolution       The resolution in "Garmin coordinate" units of the details we want. The smaller, the higher the details will be. If negative gets all the details.
     * @param listener         A visitor that will be called for every map objects.
     * @param objectKindFilter Bitset of what kind of object to concider (see ObjectKind for possible values).
     * @param objectTypeFilter If not null, allows to filter by type. Must be used with a objectKindFilter allowing only one object kind.
     * @see org.free.garminimg.MapListener
     * @see org.free.garminimg.ObjectKind
     */
    public void readMap(int minLong, int maxLong, int minLat, int maxLat, int resolution, int objectKindFilter, BitSet objectTypeFilter, MapListener listener) throws IOException
    {
        readMap(minLong, maxLong, minLat, maxLat, resolution, objectKindFilter, objectTypeFilter, listener, baseMapFiles);
        if(!wantOnlyBaseMaps(minLong, maxLong, minLat, maxLat, resolution))
        {
            readMap(minLong, maxLong, minLat, maxLat, resolution, objectKindFilter, objectTypeFilter, listener, mapFiles);
        }
    }

    private void readMap(int minLong, int maxLong, int minLat, int maxLat, int resolution, int objectKindFilter, BitSet objectTypeFilter, MapListener listener, SortedSet<ImgFileBag> files) throws IOException
    {
        for(ImgFileBag file : files)
        {
            file.readMap(minLong, maxLong, minLat, maxLat, resolution, objectKindFilter, objectTypeFilter, listener);
        }
    }

    private boolean wantOnlyBaseMaps(int minLong, int maxLong, int minLat, int maxLat, int resolution) throws IOException
    {
        if(baseMapFiles.isEmpty()) return false;
        int nbBits=0;
        for(ImgFileBag file : mapFiles)
        {
            nbBits=Math.max(nbBits, file.guessLowestNbBits(minLong, maxLong, minLat, maxLat));
        }
        int targetBits=24-(int)Math.round(Math.log(resolution)/Math.log(2))-2;
        return nbBits>=targetBits-1;
    }

    public void printDebug(PrintStream out) throws IOException
    {
        for(ImgFileBag file : mapFiles)
        {
            file.printDebug(out);
        }
    }

    public int getMinLongitude() throws IOException
    {
        int result=Integer.MAX_VALUE;
        if(mapFiles.size()>0)
            for(ImgFileBag file : mapFiles)
            {
                result=Math.min(result, file.getWestBoundary());
            }
        else
            for(ImgFileBag file : baseMapFiles)
            {
                result=Math.min(result, file.getWestBoundary());
            }
        return result;
    }

    public int getMaxLongitude() throws IOException
    {
        int result=Integer.MIN_VALUE;
        if(mapFiles.size()>0)
            for(ImgFileBag file : mapFiles)
            {
                result=Math.max(result, file.getEastBoundary());
            }
        else
            for(ImgFileBag file : baseMapFiles)
            {
                result=Math.max(result, file.getEastBoundary());
            }
        return result;
    }

    public int getMinLatitude() throws IOException
    {
        int result=Integer.MAX_VALUE;
        if(mapFiles.size()>0)
            for(ImgFileBag file : mapFiles)
            {
                result=Math.min(result, file.getSouthBoundary());
            }
        else
            for(ImgFileBag file : baseMapFiles)
            {
                result=Math.min(result, file.getSouthBoundary());
            }
        return result;
    }

    public int getMaxLatitude() throws IOException
    {
        int result=Integer.MIN_VALUE;
        if(mapFiles.size()>0)
            for(ImgFileBag file : mapFiles)
            {
                result=Math.max(result, file.getNorthBoundary());
            }
        else
            for(ImgFileBag file : baseMapFiles)
            {
                result=Math.max(result, file.getNorthBoundary());
            }

        return result;
    }

    static synchronized void registerOpenFile(ImgFileBag bag) throws IOException
    {
        int cpt=0;
        while(openFiles.size()>=MAX_OPEN_FILES && cpt<openFiles.size())
        {
            ImgFileBag toClose=openFiles.remove(0);
            if(!toClose.close())
            {   //failed (was in use), retry with another one
                openFiles.add(toClose);
            }
            ++cpt;
        }
        openFiles.add(bag);
    }

    private static BitSet mapBackgroundFilter=null;

    private static BitSet getMapBackgroundFilter()
    {
        if(mapBackgroundFilter==null)
        {
            mapBackgroundFilter=new BitSet(0xB+1);
            mapBackgroundFilter.set(ImgConstants.BACKGROUND);
            mapBackgroundFilter.set(ImgConstants.DEFINITION_AREA);
        }
        return mapBackgroundFilter;
    }

    private static BitSet mapForestFilter=null;

    private static BitSet getMapForestFilter()
    {
        if(mapForestFilter==null)
        {
            mapForestFilter=new BitSet(ImgConstants.FOREST+1);
            mapForestFilter.set(0x0E);

            mapForestFilter.set(0x14);
            mapForestFilter.set(0x15);
            mapForestFilter.set(0x16);
            mapForestFilter.set(0x17);
            mapForestFilter.set(0x18);

            mapForestFilter.set(0x1E);
            mapForestFilter.set(0x1F);
            mapForestFilter.set(0x20);

            mapForestFilter.set(0x50);
            mapForestFilter.set(0x53);
            mapForestFilter.set(ImgConstants.FOREST);
        }
        return mapForestFilter;
    }

    private static BitSet mapCityFilter=null;

    private static BitSet getMapCityFilter()
    {
        if(mapCityFilter==null)
        {
            mapCityFilter=new BitSet(0x03+1);

            mapCityFilter.set(0x01);
            mapCityFilter.set(0x02);
            mapCityFilter.set(0x03);

        }
        return mapCityFilter;
    }

    private static BitSet mapZonesFilter=null;

    private static BitSet getMapZonesFilter()
    {
        if(mapZonesFilter==null)
        {
            mapZonesFilter=new BitSet(ImgConstants.GRAVEL_AREA+1);

            mapZonesFilter.set(0x07);
            mapZonesFilter.set(0x0C);
            mapZonesFilter.set(0x0D);
            mapZonesFilter.set(0x0E);
            mapZonesFilter.set(0x0F);
            mapZonesFilter.set(0x11);
            mapZonesFilter.set(0x19);
            mapZonesFilter.set(0x1A);
            mapZonesFilter.set(0x4E);
            mapZonesFilter.set(0x4F);

            mapZonesFilter.set(ImgConstants.STATION_AREA);
            mapZonesFilter.set(ImgConstants.GRAVEL_AREA);

        }
        return mapZonesFilter;
    }

    private static BitSet mapOthersFilter=null;

    private static BitSet getMapOthersFilter()
    {
        if(mapOthersFilter==null)
        {
            mapOthersFilter=new BitSet(512);
            mapOthersFilter.or(mapBackgroundFilter);
            mapOthersFilter.or(mapForestFilter);
            mapOthersFilter.or(mapCityFilter);
            mapOthersFilter.flip(0, mapOthersFilter.length()-1);

        }
        return mapOthersFilter;
    }

    public ProductFile.MapDesc getMapDescription(ImgFileBag imgFileBag)
    {
        ProductFile.MapDesc result=null;
        for(int i=0; i<products.size() && result==null; i++)
        {
            ProductFile productFile=products.get(i);
            result=productFile.getMapDescription(imgFileBag);
        }
        return result;
    }

    private static class FileComparator implements Comparator<ImgFileBag>
    {
        public int compare(ImgFileBag o1, ImgFileBag o2)
        {
            // biggest surface (less precise) first
            int result=0;
            try
            {
                result=Long.valueOf(o1.getFullSurface()).compareTo(o2.getFullSurface());
            }
            catch(IOException e)
            {
                //ignored
            }
            if(result!=0)
                return -result;

            // then, by filename
            return o1.getFile().compareTo(o2.getFile());
        }
    }
}
