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
package org.free.garminimg.swing;

public class MapConfig implements Cloneable
{
    private Quality quality=Quality.FINAL;

    private boolean showLineLabel=true;

    private boolean showPolygonLabel=true;

    private boolean showPointLabel=true;

    private int poiThreshold=0x15;

    private int detailLevel=0;

    private boolean wantShading=false;

    public void setQuality(Quality quality)
    {
        this.quality=quality;
    }

    public Quality getQuality()
    {
        return quality;
    }

    public boolean equals(Object obj)
    {
        if(obj instanceof MapConfig)
        {
            MapConfig other=(MapConfig)obj;
            return quality==other.quality &&
                   showLineLabel==other.showLineLabel &&
                   showPolygonLabel==other.showPolygonLabel &&
                   showPointLabel==other.showPointLabel &&
                   poiThreshold==other.poiThreshold &&
                   detailLevel==other.detailLevel &&
                   wantShading==other.wantShading;
        }
        return false;
    }

    public void setShowLineLabel(boolean showLineLabel)
    {
        this.showLineLabel=showLineLabel;
    }

    public boolean isShowPolygonLabel()
    {
        return showPolygonLabel;
    }

    public void setShowPolygonLabel(boolean showPolygonLabel)
    {
        this.showPolygonLabel=showPolygonLabel;
    }

    public boolean isShowPointLabel()
    {
        return showPointLabel;
    }

    public void setShowPointLabel(boolean showPointLabel)
    {
        this.showPointLabel=showPointLabel;
    }

    public boolean isShowLineLabel()
    {
        return showLineLabel;
    }

    public int getPoiThreshold()
    {
        return poiThreshold;
    }

    public void setPoiThreshold(int poiThreshold)
    {
        this.poiThreshold=poiThreshold;
    }

    public void setDetailLevel(int detailLevel)
    {
        this.detailLevel=detailLevel;
    }

    public int getDetailLevel()
    {
        return detailLevel;
    }

    public boolean wantShading()
    {
        return wantShading;
    }

    public enum Quality
    {
        TEMP,       //no label, no aliasing
        DRAFT,      //no aliasing
        FINAL       //all the fancy stuff
    }

    protected MapConfig clone()
    {
        try
        {
            return (MapConfig)super.clone();
        }
        catch(CloneNotSupportedException e)
        {
            e.printStackTrace();
            return this;
        }
    }

    public void setWantShading(boolean wantRelief)
    {
        this.wantShading=wantRelief;
    }

    public String toString()
    {
        return "q="+quality+" lineL="+showLineLabel+" ptL="+showPointLabel+" polL="+showPolygonLabel+" ptT="+poiThreshold+" detail="+detailLevel+" shading="+wantShading;
    }
}
