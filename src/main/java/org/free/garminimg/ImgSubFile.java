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

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * One sub-file within a .img file.
 */
public abstract class ImgSubFile
{
    private String filename;

    private String filetype;

    private int fileSize;

    private long blocSize;

    protected int headerLength;

    private int locked;

    private List<Long> blocs=new ArrayList<Long>();

    protected ImgFileBag fileBag;

    public static ImgSubFile create(String filename, String filetype, int fileSize, int blocSize, ImgFileBag fileBag)
    {
        if(filetype.equalsIgnoreCase("LBL"))
            return new LblSubFile(filename, filetype, fileSize, blocSize, fileBag);
        else if(filetype.equalsIgnoreCase("TRE"))
            return new TreSubFile(filename, filetype, fileSize, blocSize, fileBag);
        else if(filetype.equalsIgnoreCase("RGN"))
            return new RgnSubFile(filename, filetype, fileSize, blocSize, fileBag);
        else if(filetype.equalsIgnoreCase("NET"))
            return new NetSubFile(filename, filetype, fileSize, blocSize, fileBag);
        else
            return new UnknownSubFile(filename, filetype, fileSize, blocSize, fileBag);
    }

    public ImgSubFile(String filename, String filetype, int fileSize, int blocSize, ImgFileBag fileBag)
    {
        this.filename=filename;
        this.filetype=filetype;
        this.fileSize=fileSize;
        this.blocSize=blocSize;
        this.fileBag=fileBag;
    }

    public abstract void init() throws IOException;

    protected void superInit(FileContext context) throws IOException
    {
        seek(0, context);
        headerLength=readUInt16(context);

        seek(getLockedPos(), context);
        locked=readByte(context);
    }

    /**
     * @return Position of the locked byte relative to the start of the sub-file.
     */
    public long getLockedPos()
    {
        return 0xD;
    }

    public void addBloc(long bloc)
    {
        blocs.add(bloc);
    }

    public String getFilename()
    {
        return filename;
    }

    public int getFileSize()
    {
        return fileSize;
    }

    public String getFiletype()
    {
        return filetype;
    }

    protected int getHeaderLength()
    {
        return headerLength;
    }

    public int getLocked()
    {
        return locked;
    }

    public static class FileContext
    {
        private byte[] curBlocContent=null;

        private int curPosInBloc=-1;

        private long curPos=-1;

        private int curBloc=-1;
    }

    /**
     * @param relative The position relative to the start of the sub-file
     * @return The position relative to the start of the IMG file.
     * @throws EOFException
     */
    public long getAbsolutePosition(long relative) throws EOFException
    {
        int bloc=(int)(relative/blocSize);
        long blocOffset=relative%blocSize;
        if(bloc>=blocs.size())
            throw new EOFException("offset="+relative+" bloc="+bloc+">="+blocs.size());
        return blocs.get(bloc)*blocSize+blocOffset;
    }

    public void seek(long pos, FileContext context) throws IOException
    {
        context.curPos=pos;
        int newBloc=(int)(pos/blocSize);
        if(context.curBloc!=newBloc)
        {
            context.curBloc=newBloc;
            if(newBloc>=blocs.size())
                throw new EOFException("offset="+pos+" bloc="+newBloc+">="+blocs.size());
            context.curBlocContent=fileBag.getBloc(blocs.get(newBloc)*blocSize, blocSize);
        }
        context.curPosInBloc=(int)(pos%blocSize);
    }

    public long getNextReadPos(FileContext context)
    {
        return context.curPos;
    }

    public int readByte(FileContext context) throws IOException
    {
        int result=context.curBlocContent[context.curPosInBloc++]&0xFF;
        context.curPos++;
        if(context.curPosInBloc>=blocSize)
        {
            seek(context.curPos, context);
        }
        return result;
    }

    public String readString(int len, FileContext context) throws IOException
    {
        StringBuffer result=new StringBuffer(len);
        for(int cpt=0; cpt<len; ++cpt)
            result.append(readByte(context));
        return result.toString();
    }

    public int readUInt16(FileContext context) throws IOException
    {
        return readByte(context)|readByte(context)<<8;
    }

    public int readUInt24(FileContext context) throws IOException
    {
        return readByte(context)|readByte(context)<<8|readByte(context)<<16;
    }

    public long readUInt32(FileContext context) throws IOException
    {
        return readByte(context)|readByte(context)<<8|readByte(context)<<16|readByte(context)<<24;
    }

    public int readInt16(FileContext context) throws IOException
    {
        int result=readUInt16(context);
        if(result>0x7FFF)
        {
            result=(result&0x7FFF)-0x8000;
        }
        return result;
    }

    public int readInt24(FileContext context) throws IOException
    {
        int result=readUInt24(context);
        if(result>0x7FFFFF)
        {
            result=(result&0x7FFFFF)-0x800000;
        }
        return result;
    }

    public void printDebug(PrintStream out) throws IOException
    {
        // TODO Auto-generated method stub
        out.println(filename+"."+filetype+" size="+fileSize+" lock="+locked+" headerLength="+headerLength);
    }

    public void debugPrintHex(FileContext context, long startOffset, long length) throws IOException
    {
        seek(startOffset, context);
        System.out.print("        ");
        for(int cpt=0; cpt<16; ++cpt)
        {
            System.out.format(" %X ", (cpt+startOffset)%16);
            if(cpt%16==7)
                System.out.print("  ");
        }
        System.out.println();
        for(int i=0; i<length; i+=16)
        {
            long realPos=i+startOffset;
            System.out.format("0x%04X: ", realPos);
            StringBuilder chars=new StringBuilder("    ");
            for(int j=i; j<i+16 && j<length; ++j)
            {
                int cur=readByte(context);
                System.out.format("%02X ", cur);
                if(j-i==7)
                    System.out.print("  ");
                if(cur>='!' && cur<='~')
                    chars.append((char)cur);
                else
                    chars.append('.');
                realPos++;
            }
            System.out.println(chars.toString());
        }
    }
}
