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

/**
 * A label for a Point Of Interest.
 */
public class POILabel extends Label
{
    private String streetNumber=null;

    private String street=null;

    private String city=null;

    private String zip=null;

    private String phone=null;

    private boolean fullInitDone=false;

    public POILabel(ImgFileBag file, int labelOffset)
    {
        super(file, labelOffset);
    }

    protected void init() throws IOException
    {
        name=file.getLblFile().getPOIName(labelOffset);
    }

    private synchronized void fullInitIfNeeded() throws IOException
    {
        if(!fullInitDone)
        {
            fullInit();
            fullInitDone=true;
            initDone=true;
        }
    }

    private void fullInit() throws IOException
    {
        file.getLblFile().getPOI(labelOffset, this);
    }

    void setName(String name)
    {
        this.name=name;
    }

    public void setStreetNumber(String streetNumber)
    {
        this.streetNumber=streetNumber;
    }

    public void setStreet(String street)
    {
        this.street=street;
    }

    public void setCity(String city)
    {
        this.city=city;
    }

    public void setZip(String zip)
    {
        this.zip=zip;
    }

    public void setPhone(String phone)
    {
        this.phone=phone;
    }

    public String getCity() throws IOException
    {
        fullInitIfNeeded();
        return city;
    }

    public String getPhone() throws IOException
    {
        fullInitIfNeeded();
        return phone;
    }

    public String getStreet() throws IOException
    {
        fullInitIfNeeded();
        return street;
    }

    public String getStreetNumber() throws IOException
    {
        fullInitIfNeeded();
        return streetNumber;
    }

    public String getZip() throws IOException
    {
        fullInitIfNeeded();
        return zip;
    }

    public String toDebugHtml() throws IOException
    {
        fullInitIfNeeded();
        StringBuilder result=new StringBuilder(super.toDebugHtml());
        if(streetNumber!=null || street!=null)
        {
            if(street!=null) result.append(street).append(" ");
            if(streetNumber!=null) result.append(streetNumber);
            result.append("<br>");
        }
        if(zip!=null || city!=null)
        {
            if(zip!=null) result.append(zip).append(" ");
            if(city!=null) result.append(city);
            result.append("<br>");
        }
        if(phone!=null) result.append(phone).append("<br>");
        return result.toString();
    }

    public TreSubFile getTre() throws IOException
    {
        return file.getTreFile();
    }

    public RgnSubFile getRgn() throws IOException
    {
        return file.getRgnFile();
    }
}
