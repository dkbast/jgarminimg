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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PngFileExporter extends SimpleFileExporter
{
    private BufferedImage image;

    private File selectedFile;

    public String getExtension()
    {
        return "png";
    }

    public void setup(File selectedFile, int width, int height)
    {
        this.selectedFile=selectedFile;
        image=new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public Graphics2D getG2()
    {
        return image.createGraphics();
    }

    public void finishSave() throws IOException
    {
        ImageIO.write(image, "png", selectedFile);
    }

    public String toString()
    {
        return "PNG";
    }
}
