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
package org.free.garminimg.swing;

import org.free.garminimg.CoordUtils;
import org.free.garminimg.ImgFilesBag;
import org.free.garminimg.MapListener;
import org.free.garminimg.ObjectKind;
import org.free.garminimg.utils.*;
import org.free.garminimg.utils.MapTransformer.Converter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A swing panel displaying a map.
 * @see org.free.garminimg.swing.MapControlPanel
 */
public class MapPanel<COORD> extends JPanel
{
    private static final long serialVersionUID=-9138170724378387157L;

    public static final Color BG_COLOR=Color.WHITE;

    private final MapTransformer<COORD> transformer;

    private final ImgFilesBag map;

    private MapConfig config=new MapConfig();

    private Popup popup=null;

    private MapPanelThread<COORD> thread;

    public static final Color LABEL_COLOR=Color.BLACK;

    public static final Color LABEL_BACKGROUND=new Color(255, 255, 255, 180);

    /**
     * Create a stand-alone MapPanel, with its own converter and its own maps.
     */
    public MapPanel(Converter<COORD> converter, int margin)
    {
        this(new ImgFilesBag(), new MapTransformer<COORD>(converter, margin));
    }

    /**
     * Create a dependent MapPanel, with its converter and maps provided from outside.
     */
    public MapPanel(ImgFilesBag map, MapTransformer<COORD> transformer)
    {
        this.map=map;
        this.transformer=transformer;
        thread=new MapPanelThread<COORD>(this, config);
        thread.start();

        addComponentListener(new ComponentListener()
        {
            public void componentResized(ComponentEvent e)
            {
                MapPanel.this.componentResized();
            }

            public void componentMoved(ComponentEvent e)
            {
            }

            public void componentShown(ComponentEvent e)
            {
            }

            public void componentHidden(ComponentEvent e)
            {
            }
        });

        transformer.setDimensions(getWidth(), getHeight());
    }

    public void componentResized()
    {
        if(transformer.setDimensions(getWidth(), getHeight()))
        {
            transformerChanged();
        }
    }

    protected void transformerChanged()
    {
        thread.checkSetup(transformer, config);
    }

    /**
     * If an external class changed manually the transformer, it must call this method to make
     * the change effective.
     */
    public void transformerChangedManually()
    {
        thread.checkSetup(transformer, config);
        repaint();
    }

    public void addMapLocation(File location) throws IOException
    {
        if(location.isDirectory())
            map.addDirectory(location);
        else
            map.addFile(location);
        thread.scheduleComputeMap();
    }

    public void clearMaps() throws IOException
    {
        map.clear();
        thread.scheduleComputeMap();
    }

    public void setPosition(double minLon, double maxLon, double minLat, double maxLat)
    {
        transformer.resetAutoScale();
        transformer.adjustAutoScaleFromWgs84(minLon, minLat);
        transformer.adjustAutoScaleFromWgs84(maxLon, maxLat);
        transformer.fixAspectRatio();
        transformerChanged();
        repaint();
    }

    public void movePosition(double deltaX, double deltaY)
    {
        transformer.moveMapPosition(deltaX, deltaY);
        transformerChanged();
        repaint();
    }

    /**
     * If factor>1 zoom in, otherwise zoom out.
     */
    public void zoom(double factor, int x, int y)
    {
        transformer.zoom(factor, x, y);
        transformerChanged();
        repaint();
    }

    public void zoom(double factor)
    {
        transformer.zoom(factor);
        transformerChanged();
        repaint();
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if(transformer.isSetupDone())
        {
            thread.checkSetup(transformer, config);
            Graphics2D g2=((Graphics2D)g.create());

            // paint the background
            g2.setColor(MapPanel.BG_COLOR);
            g2.fillRect(0, 0, getWidth(), getHeight());

            MapPanelThread.PaintInfo paintInfo=thread.getPaintInfo(transformer);
            if(paintInfo!=null)
            {
                /*g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);*/
                g2.drawImage(paintInfo.image, paintInfo.x, paintInfo.y, paintInfo.w, paintInfo.h, this);
            }
        }
    }

    public String getInfo(int x, int y, int maxNbInfo)
    {
        Point2D.Double wgs84=new Point2D.Double();
        transformer.map2wgs84(x, y, wgs84);

        Point2D.Double wgs84Precision=new Point2D.Double();
        transformer.map2wgs84(x+5, y, wgs84Precision);

        int longitude=CoordUtils.fromWGS84Rad(wgs84.x);
        int latitude=CoordUtils.fromWGS84Rad(wgs84.y);

        int precision=Math.max(Math.abs(longitude-CoordUtils.fromWGS84Rad(wgs84Precision.x)),
                               Math.abs(latitude-CoordUtils.fromWGS84Rad(wgs84Precision.y)));

        try
        {
            FindObjectByPositionListener listener=new FindObjectByPositionListener(longitude, latitude, precision);

            int resolution=getResolution(transformer);
            map.readMapForDrawing(longitude, longitude, latitude, latitude, resolution, ObjectKind.ALL, listener);

            List<FoundObject> founds=listener.getFounds();
            if(founds.isEmpty())
                return null;
            StringBuilder buffer=new StringBuilder("<html>\n<ul>\n");
            for(int i=founds.size()-1; i>=Math.max(0, founds.size()-maxNbInfo); i--)
            {
                FoundObject found=founds.get(i);
                buffer.append("  <li>");
                found.toDebugHtml(buffer);
                buffer.append("</li>\n");
            }
            buffer.append("</ul>\n</html>\n");
            return buffer.toString();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public MapTransformer<COORD> getTransformer()
    {
        return transformer;
    }

    /**
     * Zoom and position the map in order to see everything.
     */
    public void showAllMap()
    {
        transformer.resetAutoScale();
        try
        {
            transformer.adjustAutoScaleFromWgs84(CoordUtils.toWGS84Rad(map.getMinLongitude()), CoordUtils.toWGS84Rad(map.getMinLatitude()));
            transformer.adjustAutoScaleFromWgs84(CoordUtils.toWGS84Rad(map.getMaxLongitude()), CoordUtils.toWGS84Rad(map.getMaxLatitude()));
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        transformer.fixAspectRatio();
        transformerChanged();
    }

    /**
     * Does not trigger a repaint...
     */
    public void setQuality(MapConfig.Quality quality)
    {
        config.setQuality(quality);
        thread.checkSetup(transformer, config);
    }

    public void showInfo(int x, int y, int maxNbInfo)
    {
        String info=getInfo(x, y, maxNbInfo);
        if(info!=null)
        {
            showInfoPopup(info, x, y);
        }
    }

    protected void showInfoPopup(String info, int x, int y)
    {
        JPanel content=new JPanel();
        content.add(new JLabel(info));
        content.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        Point location=getLocationOnScreen();
        location.x+=x+5;
        location.y+=y+5;
        popup=PopupFactory.getSharedInstance().getPopup(this, content, location.x, location.y);
        popup.show();
    }

    public void hideInfo()
    {
        if(popup!=null)
            popup.hide();
    }

    public void zoomToGarminGeo(double factor, int longitude, int latitude)
    {
        Point2D.Double mapCoord=new Point2D.Double();
        COORD tempCoord=transformer.createTempCoord();
        transformer.wgs84ToMap(CoordUtils.toWGS84Rad(longitude), CoordUtils.toWGS84Rad(latitude), tempCoord, mapCoord);
        zoom(factor, (int)mapCoord.getX(), (int)mapCoord.getY());
    }

    public ImgFilesBag getMap()
    {
        return map;
    }

    public int getResolution(MapTransformer<COORD> transformer)
    {
        Rectangle bbox=transformer.getGarminBoundingBox();
        int minLon=bbox.x;
        int maxLon=bbox.x+bbox.width;
        return getResolution(minLon, maxLon, transformer.getWidth());
    }

    public int getResolution(int minLon, int maxLon)
    {
        return getResolution(minLon, maxLon, getWidth());
    }

    public static int getResolution(int minLon, int maxLon, int width)
    {
        return (maxLon-minLon)/width;
    }

    /**
     * Create a map drawer. Called each time a new map view must be displayed.
     * <p>Override this if you want a custom drawer.
     */
    public TransformedMapListener createMapDrawer(MapConfig workConfig, Graphics2D g2, float fontSize, MapTransformer<COORD> workTransformer)
    {
        return new MapDrawer(workConfig, g2, this, 9.0f, LABEL_COLOR, LABEL_BACKGROUND);
    }

    public void saveMapAs(final File selectedFile, FileExporter exporter, MapTransformer<COORD> transformer) throws IOException
    {
        exporter.setup(selectedFile, transformer.getWidth(), transformer.getHeight());
        final Graphics2D g2=exporter.getG2();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(MapPanel.BG_COLOR);
        g2.fillRect(0, 0, transformer.getWidth(), transformer.getHeight());

        Rectangle bbox=transformer.getGarminBoundingBox();
        int minLon=bbox.x;
        int maxLon=bbox.x+bbox.width;
        int minLat=bbox.y;
        int maxLat=bbox.y+bbox.height;
        int resolution=getResolution(minLon, maxLon, transformer.getWidth());
        TransformedMapListener drawer=createMapDrawer(new MapConfig(), g2, exporter.getFontSize(), transformer);
        MapListener listener=new CoordinateConverterListener<COORD>(transformer,
                                                                    new ClippingMapListener(0, transformer.getWidth(), 0, transformer.getHeight(),
                                                                                            drawer));

        map.readMapForDrawing(minLon, maxLon, minLat, maxLat, resolution, ObjectKind.ALL, listener);
        exporter.finishSave();
    }

    public void setShowLineLabel(boolean showLineLabel)
    {
        config.setShowLineLabel(showLineLabel);
        thread.checkSetup(transformer, config);
    }

    public void setShowPolygonLabel(boolean showPolygonLabel)
    {
        config.setShowPolygonLabel(showPolygonLabel);
        thread.checkSetup(transformer, config);
    }

    public void setShowPointLabel(boolean showPointLabel)
    {
        config.setShowPointLabel(showPointLabel);
        thread.checkSetup(transformer, config);
    }

    public void setPoiThreshold(int threshold)
    {
        config.setPoiThreshold(threshold);
        thread.checkSetup(transformer, config);
    }

    public void setDetailLevel(int detailLevel)
    {
        config.setDetailLevel(detailLevel);
        thread.checkSetup(transformer, config);
    }

    public void setShading(boolean enabled)
    {
        config.setWantShading(enabled);
        thread.checkSetup(transformer, config);
    }
}
