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
package org.free.garminimg.utils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class ImgConstants
{
    /**
     * Points have the highest priority
     */
    public static final int POINT_BASE_PRIORITY=1<<8;

    /**
     * Polygons have a middle priority
     */
    private static final int POLYGON_BASE_PRIORITY=3<<8;

    /**
     * Polylines have the lowest priority
     */
    private static final int POLYLINE_BASE_PRIORITY=5<<8;

    public static PointDescription getPointType(int type, int subType)
    {
        PointDescription result=POINT_TYPES.get(type<<8|subType);
        if(result==null)
            result=POINT_TYPES.get(type<<8);
        if(result==null)
        {
            result=new PointDescription(type<<8|subType, type+"/"+subType, false, null, true, 255);
            POINT_TYPES.put(type<<8|subType, result);
        }
        return result;
    }

    public static String getPointDesc(int type, int subType)
    {
        return getPointType(type, subType).getDescription();
    }

    public static Description getPolyType(int type, boolean line)
    {
        if(line)
            return getPolylineType(type);
        else
            return getPolygonType(type);
    }

    public static PolylineDescription getPolylineType(int type)
    {
        PolylineDescription result=POLYLINE_TYPES.get(type);
        if(result==null)
        {
            result=new PolylineDescription(type, Integer.toString(type), false, Color.ORANGE, new BasicStroke(1.0f), 255);
            POLYLINE_TYPES.put(type, result);
        }
        return result;
    }

    public static PolygonDescription getPolygonType(int type)
    {
        PolygonDescription result=POLYGON_TYPES.get(type);
        if(result==null)
            result=new PolygonDescription(type, Integer.toString(type), false, Color.ORANGE, 255);
        return result;
    }

    public static String getPolyDesc(int type, boolean line)
    {
        if(line)
            return getPolylineType(type).getDescription();
        else
            return getPolygonType(type).getDescription();
    }

    private static final String NO_POINT="no point";

    private static final Object POINT_TYPES_T[][]={
            //format: type<<8|subType, name, iconName, prio=type
            {0x0100, "City (pop. over 8M)", "large_city"},
            {0x0200, "City (pop. 4-8M)", "large_city"},
            {0x0300, "City (pop. 2-4M)", "large_city"},
            {0x0400, "City (pop. 1-2M)", "large_city"},
            {0x0500, "City (pop. 0.5-1M)", "medium_city"},
            {0x0600, "City (pop. 200-500K)", "medium_city"},
            {0x0700, "City (pop. 100-200K)", "medium_city"},
            {0x0800, "City (pop. 50-100K)", "medium_city"},
            {0x0900, "City (pop. 20-50K)", "medium_city"},
            {0x0a00, "Town (pop. 10-20K)", "medium_city"},
            {0x0b00, "Town (pop. 4-10K)", "small_city"},
            {0x0c00, "Town (pop. 2-4K)", "small_city"},
            {0x0d00, "Town (pop. 1-2K)", "small_city"},
            {0x0e00, "Town (pop. 0.5-1K)", "small_city"},
            {0x0f00, "Town (pop. 200-500)", "small_city"},
            {0x1000, "Town (pop. 100-200)", "small_city"},
            {0x1100, "Town (pop. under 100)", "small_city"},
            {0x1300, "Town", "small_city"},
            {0x1400, "Region, large", NO_POINT, -2},
            {0x1500, "Region, large", NO_POINT, -2},

            {0x1600, "Navaid"},
            {0x1601, "Forg horn", "white_horn"},
            {0x1602, "Radio beacon", "radio_beacon"},
            {0x1603, "Racon", "radio_beacon"},
            {0x1604, "Daybeacon (red triangle)", "radio_beacon"},
            {0x1605, "Daybeacon (green square)", "radio_beacon"},
            {0x1606, "Unlit navaid (white diamond)"},
            {0x1607, "Unlit navaid (white)"},
            {0x1608, "Unlit navaid (red)"},
            {0x1609, "Unlit navaid (green)"},
            {0x160a, "Unlit navaid (black)"},
            {0x160b, "Unlit navaid (yellow/amber)"},
            {0x160c, "Unlit navaid (orange)"},
            {0x160d, "Unlit navaid (multi-colored)"},
            {0x160e, "Navaid"},
            {0x160f, "Navaid (white)"},
            {0x1610, "Navaid (red)"},
            {0x1611, "Navaid (green)"},
            {0x1612, "Navaid (yellow/amber)"},
            {0x1613, "Navaid (orange)"},
            {0x1614, "Navaid (violet)"},
            {0x1615, "Navaid (blue)"},
            {0x1616, "Navaid (multi-colored)"},

            {0x1c00, "Obstruction"},
            {0x1c01, "Wreck", "white_wreck"},
            {0x1c02, "Submerged wreck, dangerous", "white_wreck"},
            {0x1c03, "Submerged wreck, non-dangerous", "white_wreck"},
            {0x1c04, "Wreck, cleared by wire-drag", "white_wreck"},
            {0x1c05, "Obstruction, visible at high water"},
            {0x1c06, "Obstruction, awash"},
            {0x1c07, "Obstruction, submerged"},
            {0x1c08, "Obstruction, cleared by wire-drag"},
            {0x1c09, "Rock, awash"},
            {0x1c0a, "Rock, submerged at low water"},
            {0x1c0b, "Sounding"},

            {0x1d00, "Tide"},
            {0x1d01, "Tide prediction"},
            {0x1d02, "Tide prediction"},

            {0x1e00, "Region, medium", NO_POINT, -1},
            {0x1f00, "Region, medium", NO_POINT, -1},
            {0x2000, "Exit"},
            {0x2100, "Exit, with facilities"},
            {0x210f, "Exit, service"},
            {0x2200, "Exit, restroom"},
            {0x2300, "Exit, convenience store"},
            {0x2400, "Exit, weigh station"},
            {0x2500, "Exit, toll booth"},
            {0x2600, "Exit, information"},
            {0x2700, "Exit"},
            {0x2800, "Region, small", NO_POINT, 0},
            {0x2900, "Region", null, 0},

            {0x2a00, "Food & Drink", "white_knife_fork"},
            {0x2a01, "Food & Drink, American", "fast_food"},
            {0x2a02, "Food & Drink, Asian", "white_knife_fork"},
            {0x2a03, "Food & Drink, Barbeque", "white_knife_fork"},
            {0x2a04, "Food & Drink, Chinese", "white_knife_fork"},
            {0x2a05, "Food & Drink, Deli/Bakery", "white_knife_fork"},
            {0x2a06, "Food & Drink, International", "white_knife_fork"},
            {0x2a07, "Food & Drink, Fast Food", "fast_food"},
            {0x2a08, "Food & Drink, Italian", "white_knife_fork"},
            {0x2a09, "Food & Drink, Mexican", "white_knife_fork"},
            {0x2a0a, "Food & Drink, Pizza", "white_knife_fork"},
            {0x2a0b, "Food & Drink, Seafood", "white_knife_fork"},
            {0x2a0c, "Food & Drink, Steak/Grill", "white_knife_fork"},
            {0x2a0d, "Food & Drink, Bagel/Doughnut", "white_knife_fork"},
            {0x2a0e, "Food & Drink, Cafe/Diner", "white_knife_fork"},
            {0x2a0f, "Food & Drink, French", "white_knife_fork"},
            {0x2a10, "Food & Drink, German", "white_mug"},
            {0x2a11, "Food & Drink, British Isles", "white_knife_fork"},

            {0x2b00, "Lodging", "lodging"},
            {0x2b01, "Lodging, Hotel/Motel", "lodging"},
            {0x2b02, "Lodging, Bed & Breakfast/Inn", "lodging"},
            {0x2b03, "Lodging, Campground/RV Park", "lodging"},
            {0x2b04, "Lodging, Resort", "lodging"},

            {0x2c00, "Attraction", "museum"},
            {0x2c01, "Recreation, Amusement/Theme Park", "amusement_park"},
            {0x2c02, "Attraction, Museum/Historical", "museum"},
            {0x2c03, "Community, Library", "museum"},
            {0x2c04, "Attraction, Landmark", "amber_map_buoy"},
            {0x2c05, "Community, School", "school"},
            {0x2c06, "Attraction, Park/Garden", "park"},
            {0x2c07, "Attraction, Zoo/Aquarium", "zoo"},
            {0x2c08, "Recreation, Arena/Track", "stadium"},
            {0x2c09, "Attraction, Hall/Auditorium", "movie"},
            {0x2c0a, "Attraction, Winery"},
            {0x2c0b, "Community, Place of Worship", "church"},
            {0x2c0c, "Attraction, Hot Spring", "drinking_water"},

            {0x2d00, "Entertainment", "movie"},
            {0x2d01, "Entertainment, Live Theater", "live_theater"},
            {0x2d02, "Entertainment, Bar/Nightclub", "white_mug"},
            {0x2d03, "Entertainment, Cinema", "movie"},
            {0x2d04, "Entertainment, Casino", "white_dollar"},
            {0x2d05, "Entertainment, Golf Course", "golf"},
            {0x2d06, "Recreation, Skiing Center/Resort", "skiing"},
            {0x2d07, "Entertainment, Bowling", "bowling"},
            {0x2d08, "Entertainment, Ice Skating"},
            {0x2d09, "Entertainment, Swimming Pool", "swimming"},
            {0x2d0a, "Entertainment, Sports/Fitness Center", "fitness"},
            {0x2d0b, "Entertainment, Sport Airport", "airport"},

            {0x2e00, "Shopping", "shopping_cart"},
            {0x2e01, "Shopping, Department Store", "dept_store"},
            {0x2e02, "Shopping, Grocery Store", "shopping_cart"},
            {0x2e03, "Shopping, General Merchandise", "shopping_cart"},
            {0x2e04, "Shopping Center", "shopping_cart"},
            {0x2e05, "Shopping, Pharmacy", "pharmacy"},
            {0x2e06, "Shopping, Convenience Store", "shopping_cart"},
            {0x2e07, "Shopping, Apparel", "shopping_cart"},
            {0x2e08, "Shopping, Home and Garden", "shopping_cart"},
            {0x2e09, "Shopping, Home Furnishings", "shopping_cart"},
            {0x2e0a, "Shopping, Specialty Retail", "shopping_cart"},
            {0x2e0b, "Shopping, Computer/Software", "shopping_cart"},
            {0x2e0c, "Shopping, Other", "shopping_cart"},

            {0x2f00, "Service", "convenience_store"},
            {0x2f01, "Service, Auto Fuel", "convenience_store"},
            {0x2f02, "Service, Auto Rental", "car_rental"},
            {0x2f03, "Service, Auto Repair", "car_repair"},
            {0x2f04, "Service, Air Transportation", "airport"},
            {0x2f05, "Service, Post Office", "post_office"},
            {0x2f06, "Service, Bank/ATM", "white_dollar"},
            {0x2f07, "Service, Dealer/Auto Parts", "car_repair"},
            {0x2f08, "Service, Ground Transportation", "car"},
            {0x2f09, "Service, Marina/Boat Repair", "boat_ramp"},
            {0x2f0a, "Service, Wrecker"},
            {0x2f0b, "Service, Parking", "parking"},
            {0x2f0c, "Service, Rest Area/Information", "restrooms"},
            {0x2f0d, "Service, Auto Club", "car"},
            {0x2f0e, "Service, Car Wash", "car"},
            {0x2f0f, "Service, Garmin Dealer"},
            {0x2f10, "Service, Personal"},
            {0x2f11, "Service, Business"},
            {0x2f12, "Service, Communication"},
            {0x2f13, "Service, Repair"},
            {0x2f14, "Service, Social"},
            {0x2f15, "Service, Public Utility"},

            {0x3000, "Emergency/Government", "white_house"},
            {0x3005, "City Hall", "flag"},
            {0x3010, "Community, Police Station", "flag"},
            {0x3020, "Hospital", "first_aid"},
            {0x3030, "Community, City Hall", "flag"},
            {0x3040, "Community, Court House", "inspection_weigh_station"},
            {0x3050, "Community, Community Center", "flag"},
            {0x3060, "Community, Border Crossing", "crossing"},

            {0x4000, "Golf", "golf"},
            {0x4100, "Fishing", "white_fish"},
            {0x4200, "Wreck", "white_wreck"},
            {0x4300, "Marina", "white_anchor"},
            {0x4400, "Gas Station", "white_fuel"},
            {0x4500, "Food & Drink", "white_knife_fork"},
            {0x4600, "Bar", "white_mug"},
            {0x4700, "Boat Ramp", "boat_ramp"},
            {0x4800, "Camping", "campground"},
            {0x4900, "Park", "park"},
            {0x4a00, "Picnic Area", "picnic"},
            {0x4b00, "First Aid", "first_aid"},
            {0x4c00, "Information", "information"},
            {0x4d00, "Parking", "parking"},
            {0x4e00, "Restroom", "restrooms"},
            {0x4f00, "Shower", "shower"},
            {0x5000, "Drinking Water", "drinking_water"},
            {0x5100, "Telephone", "telephone"},
            {0x5200, "Scenic Area", "orange_map_buoy"},
            {0x5300, "Skiing", "skiing"},
            {0x5400, "Swimming", "swimming"},
            {0x5500, "Dam", "dam"},
            {0x5700, "Danger", "danger"},
            {0x5800, "Restrcited Area", "restricted_area"},

            {0x5900, "Airport", "airport"},
            {0x5901, "Airport, Large", "airport"},
            {0x5902, "Airport, Medium", "airport"},
            {0x5903, "Airport, Small", "airport"},
            {0x5904, "Heliport", "heliport"},
            {0x5905, "Airport", "airport"},

            {0x5d00, "Daymark, green square"},
            {0x5e00, "Daymark, red triangle"},

            {0x6100, "Place"},
            {0x6200, "Depth"},
            {0x6300, "Elevation", "summit"},

            {0x6400, "Man-made Feature", "building"},
            {0x6401, "Bridge", "bridge"},
            {0x6402, "Building", "building"},
            {0x6403, "Cemetary", "cemetery"},
            {0x6404, "Church", "church"},
            {0x6405, "Civil Building", "civil_location"},
            {0x6406, "Crossing", "crossing"},
            {0x6407, "Dam", "dam"},
            {0x6408, "Hospital", "first_aid"},
            {0x6409, "Levee"},
            {0x640a, "Locale"},
            {0x640b, "Military", "military_location"},
            {0x640c, "Mine", "mine"},
            {0x640d, "Oil Field", "oil_field"},
            {0x640e, "Park", "park"},
            {0x640f, "Post Office", "post_office"},
            {0x6410, "School", "school"},
            {0x6411, "Tower", "tall_tower"},
            {0x6412, "Trail", "trail_head"},
            {0x6413, "Tunnel", "tunnel"},
            {0x6414, "Well", "drinking_water"},
            {0x6415, "Ghost Town", "historical_town"},
            {0x6416, "Subdivision"},

            {0x6500, "Water Feature"},
            {0x6501, "Arroyo"},
            {0x6502, "Sand Bar"},
            {0x6503, "Bay"},
            {0x6504, "Bend"},
            {0x6505, "Canal"},
            {0x6506, "Channel"},
            {0x6507, "Cove"},
            {0x6508, "Falls"},
            {0x6509, "Geyser"},
            {0x650a, "Glacier"},
            {0x650b, "Harbor"},
            {0x650c, "Island"},
            {0x650d, "Lake"},
            {0x650e, "Rapids"},
            {0x650f, "Resevoir"},
            {0x6510, "Sea"},
            {0x6511, "Spring"},
            {0x6512, "Stream"},
            {0x6513, "Swamp"},

            {0x6600, "Land Feature"},
            {0x6601, "Arch"},
            {0x6602, "Area"},
            {0x6603, "Basin"},
            {0x6604, "Beach"},
            {0x6605, "Bench"},
            {0x6606, "Cape"},
            {0x6607, "Cliff"},
            {0x6608, "Crater"},
            {0x6609, "Flat"},
            {0x660a, "Forest"},
            {0x660b, "Gap"},
            {0x660c, "Gut"},
            {0x660d, "Isthmus"},
            {0x660e, "Lava"},
            {0x660f, "Pillar"},
            {0x6610, "Plain"},
            {0x6611, "Range"},
            {0x6612, "Reserve"},
            {0x6613, "Ridge"},
            {0x6614, "Rock"},
            {0x6615, "Slope"},
            {0x6616, "Summit", "summit"},
            {0x6617, "Valley"},
            {0x6618, "Woods", "forest"}};

    private static final Map<Integer, PointDescription> POINT_TYPES=convertPointType(POINT_TYPES_T);

    private static final float tinyDash[]={5.0f, 2.0f};

    private static final float smallDash[]={5.0f, 5.0f};

    private static final float pointDash[]={15.0f, 3.0f, 3.0f, 3.0f};

    private static final Stroke trailStroke=new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                                            10.0f, smallDash, 0.0f);

    private static final Stroke powerLineStroke=new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                                                10.0f, pointDash, 0.0f);

    private static final Color water=new Color(74, 216, 255);

    public static final int MINOR_LAND_CONTOUR;

    public static final int INTERMEDIATE_LAND_CONTOUR;

    public static final int MAJOR_LAND_CONTOUR;

    public static final int MINOR_DEPTH_CONTOUR;

    public static final int INTERMEDIATE_DEPTH_CONTOUR;

    public static final int MAJOR_DEPTH_CONTOUR;

    public static final int TUNNEL_SHIFT=0x100;

    public static final int RUINS=0x150;

    private static final Object POLYLINE_TYPES_T[][]={
            //format: type, name, color, paint, ignoreLabel=false
            {0x01, "Major highway", Color.BLACK, new BasicStroke(2.0f)},
            {0x02, "Principal highway", Color.BLACK, new BasicStroke(2.0f)},
            {0x03, "Other highway", Color.BLACK, new BasicStroke(2.0f)},
            {0x04, "Arterial road", Color.BLACK, new BasicStroke(2.0f)},
            {0x05, "Collector road", Color.BLACK, new BasicStroke(1.0f)},
            {0x06, "Residential street", Color.BLACK, new BasicStroke(1.0f)},
            {0x07, "Alley/Private road", Color.BLACK, new BasicStroke(0.5f)},
            {0x08, "Highway ramp, low speed", Color.BLACK, new BasicStroke(1.0f)},
            {0x09, "Highway ramp, high speed", Color.BLACK, new BasicStroke(1.0f)},
            {0x0a, "Unnpaved road", Color.BLACK, new BasicStroke(0.5f)},
            {0x0b, "Major highway connector", Color.BLACK, new BasicStroke(1.0f)},
            {0x0c, "Roundabout", Color.BLACK, new BasicStroke(1.0f)},
            {0x14, "Railroad", Color.BLACK, new RailStroke(7f, 5f, 1f)},
            {0x15, "Shoreline", new Color(128, 64, 0), new BasicStroke(1.0f)},
            {0x16, "Trail", Color.BLACK, trailStroke},
            {0x18, "Stream", water, new BasicStroke(1.0f)},
            {0x19, "Time zone", Color.BLACK, new BasicStroke(1.0f)},
            {0x1a, "Ferry", Color.BLACK, new BasicStroke(1.0f)},
            {0x1b, "Ferry", Color.BLACK, new BasicStroke(1.0f)},
            {0x1c, "State/province border", Color.BLACK, new BasicStroke(1.0f)},
            {0x1d, "County/parish border", Color.BLACK, new BasicStroke(1.0f)},
            {0x1e, "International border", Color.BLACK, new InternationalBorderStroke(10f, 6f, 1.5f)},
            {0x1f, "River", water, new BasicStroke(2.0f)},
            {MINOR_LAND_CONTOUR=0x20, "Minor land contour", new Color(128, 64, 0), new BasicStroke(0.2f), false},
            {INTERMEDIATE_LAND_CONTOUR=0x21, "Intermediate land contour", new Color(128, 64, 0), new BasicStroke(0.2f), false},
            {MAJOR_LAND_CONTOUR=0x22, "Major land contour", new Color(128, 64, 0), new BasicStroke(0.5f), false},
            {MINOR_DEPTH_CONTOUR=0x23, "Minor deph contour", Color.BLUE, new BasicStroke(0.2f), false},
            {INTERMEDIATE_DEPTH_CONTOUR=0x24, "Intermediate depth contour", Color.BLUE, new BasicStroke(0.2f), false},
            {MAJOR_DEPTH_CONTOUR=0x25, "Major depth contour", Color.BLUE, new BasicStroke(0.5f), false},
            {0x26, "Intermittent stream", water, new BasicStroke(1.0f)},
            {0x27, "Airport runway", Color.BLACK, new BasicStroke(6.0f)},
            {0x28, "Pipeline", Color.BLUE, new BasicStroke(1.0f)},
            {0x29, "Powerline", Color.BLACK, powerLineStroke},
            {0x2a, "Marine boundary", Color.BLUE, new BasicStroke(1.0f)},
            {0x2b, "Hazard boundary", Color.RED, new BasicStroke(1.0f)},

            //roads or rail in tunnel for Swiss Topo 50
            {TUNNEL_SHIFT+0x01, "Major highway (tunnel)", Color.BLACK, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x02, "Principal highway (tunnel)", Color.BLACK, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x03, "Other highway (tunnel)", Color.BLACK, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x04, "Arterial road (tunnel)", Color.BLACK, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x05, "Collector road (tunnel)", Color.BLACK, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x06, "Residential street (tunnel)", Color.BLACK, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x07, "Alley/Private road (tunnel)", Color.BLACK, new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x08, "Highway ramp, low speed (tunnel)", Color.BLACK, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x09, "Highway ramp, high speed (tunnel)", Color.BLACK, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x0a, "Unpaved road (tunnel)", Color.BLACK, new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x0b, "Major highway connector (tunnel)", Color.BLACK, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x0c, "Roundabout (tunnel)", Color.BLACK, new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, tinyDash, 0.0f), true},
            {TUNNEL_SHIFT+0x14, "Railroad (tunnel)", Color.BLACK, new RailStroke(7f, 5f, 0.3f), true},
            {RUINS, "Ruins", Color.DARK_GRAY, new BasicStroke(4.0f), true},
    };

    private static final Map<Integer, PolylineDescription> POLYLINE_TYPES=convertLineType(POLYLINE_TYPES_T);

    private static final Color urbanArea=new Color(220, 220, 220);

    private static final Paint bushPaint=new BushPaint(Color.GREEN);

    private static final Paint swampPaint=new BushPaint(water);

    public static final int LAKE=0x00;

    public static final int SMALL_URBAN_AREA;

    public static final int RURAL_HOUSING_AREA;

    public static final int MILITARY_BASE;

    public static final int AIRPORT;

    public static final int SHOPPING_CENTER;

    public static final int DEFINITION_AREA;

    public static final int BACKGROUND;

    private static final Paint rocksPaint=new RocksPaint();

    public static final int FOREST;

    public static final int STATION_AREA;

    public static final int GRAVEL_AREA;

    private static final Color BG_COLOR=new Color(255, 255, 240);

    private static final Object POLYGON_TYPES_T[][]={
            //format: type, name, color, ignoreLabel=false
            {0x01, "Large urban area (>200K)", urbanArea},
            {SMALL_URBAN_AREA=0x02, "Small urban area (<200K)", urbanArea},
            {RURAL_HOUSING_AREA=0x03, "Rural housing area", urbanArea},
            {MILITARY_BASE=0x04, "Military base", Color.BLACK},
            {0x05, "Parking lot", Color.GRAY},
            {0x06, "Parking garage", Color.GRAY},
            {AIRPORT=0x07, "Airport", Color.GRAY},
            {SHOPPING_CENTER=0x08, "Shopping center", Color.GRAY},
            {0x09, "Marina", Color.GRAY},
            {0x0a, "University/college", Color.GRAY},
            {0x0b, "Hospital", Color.GRAY},
            {0x0c, "Industrial complex", Color.GRAY},
            {0x0d, "Reservation", Color.GREEN},
            {0x0e, "Airport runnway", Color.GRAY},
            {0x13, "Man-made area", Color.DARK_GRAY}, // houses on swiss topo maps
            {0x14, "National park", new Color(180, 255, 180)},
            {0x15, "National park", new Color(180, 255, 180)},
            {0x16, "National park", new Color(180, 255, 180)},
            {0x17, "City park", Color.GREEN},
            {0x18, "Golf course", Color.GREEN},
            {0x19, "Sports complex", Color.GRAY},
            {0x1a, "Cemetary", Color.GRAY},
            {0x1e, "State park", new Color(180, 255, 180)},
            {0x1f, "State park", new Color(180, 255, 180)},
            {0x20, "State park", new Color(180, 255, 180)},
            {0x28, "Ocean", water},
            {0x29, "Blue (unknown)", water},
            {0x32, "Sea", water},
            {0x3b, "Blue (unknown)", water},
            {0x3c, "Large lake", water},
            {0x3d, "Large lake", water},
            {0x3e, "Medium lake", water},
            {0x3f, "Medium lake", water},
            {0x40, "Small lake", water},
            {0x41, "Small lake", water},
            {0x42, "Major lake", water},
            {0x43, "Major lake", water},
            {0x44, "Large lake", water},
            {0x45, "Blue (unknown)", water},
            {0x46, "Major River", water},
            {0x47, "Large River", water},
            {0x48, "Medium River", water},
            {0x49, "Small River", water},
            {DEFINITION_AREA=0x4a, "Definition area", BG_COLOR, true},
            {BACKGROUND=0x4b, "Background", BG_COLOR, true},
            {0x4c, "Intermittent water", Color.BLUE},
            {0x4d, "Glacier", new Color(220, 220, 255)},
            {0x4e, "Orchard/Plantation", new Color(210, 255, 210)},
            {0x4f, "Scrub", bushPaint},
            {0x50, "Forest", new Color(180, 255, 180)},
            {0x51, "Wetland/Swamp", swampPaint},
            {0x52, "Tundra", Color.ORANGE},
            {0x53, "Flat", rocksPaint, true},

            //artifical types for Swiss Topo 50
            {FOREST=0x101, "Forest", new Color(180, 255, 180), true},
            {STATION_AREA=0x102, "Station area", Color.GRAY, false},
            {GRAVEL_AREA=0x103, "Gravel area", Color.GRAY, true},
    };

    private static final Map<Integer, PolygonDescription> POLYGON_TYPES=convertPolygonType(POLYGON_TYPES_T);

    private static Map<Integer, PointDescription> convertPointType(Object[][] flat)
    {
        Map<Integer, PointDescription> result=new HashMap<Integer, PointDescription>(flat.length);
        for(int cpt=0; cpt<flat.length; cpt++)
        {
            Object[] cur=flat[cpt];
            Integer fullType=(Integer)cur[0];
            String iconName=null;
            boolean point=true;
            if(cur.length>=3)
            {
                iconName=(String)cur[2];
                if(iconName==NO_POINT)
                {
                    point=false;
                    iconName=null;
                }
            }
            int priority=fullType>>8;
            if(cur.length>=4 && cur[3]!=null)
            {
                priority=(Integer)cur[3];
            }
            result.put(fullType, new PointDescription(fullType, (String)cur[1], false, iconName, point, priority));
        }
        return result;
    }

    private static Map<Integer, PolygonDescription> convertPolygonType(Object[][] flat)
    {
        Map<Integer, PolygonDescription> result=new HashMap<Integer, PolygonDescription>(flat.length*2);
        for(int cpt=0; cpt<flat.length; ++cpt)
        {
            Object[] cur=flat[cpt];
            Integer type=(Integer)cur[0];
            boolean ignoreLabel=false;
            if(cur.length>3)
                ignoreLabel=(Boolean)cur[3];
            int priority=type;  //TODO: Maybe need something smarter
            PolygonDescription desc=new PolygonDescription(type, (String)cur[1], ignoreLabel, (Paint)cur[2], priority);
            result.put(type, desc);
        }
        return result;
    }

    private static Map<Integer, PolylineDescription> convertLineType(Object[][] flat)
    {
        Map<Integer, PolylineDescription> result=new HashMap<Integer, PolylineDescription>(flat.length*2);
        for(int cpt=0; cpt<flat.length; ++cpt)
        {
            Object[] cur=flat[cpt];
            Integer type=(Integer)cur[0];
            boolean ignoreLabel=false;
            if(cur.length>4)
                ignoreLabel=(Boolean)cur[4];
            int priority=type;  //TODO: Maybe need something smarter
            PolylineDescription desc=new PolylineDescription(type, (String)cur[1], ignoreLabel, (Color)cur[2],
                                                             (Stroke)cur[3], priority);
            result.put(type, desc);
        }
        return result;
    }

    public static Map<Integer, PointDescription> getPointTypes()
    {
        return POINT_TYPES;
    }

    public static Map<Integer, PolylineDescription> getPolylineTypes()
    {
        return POLYLINE_TYPES;
    }

    public static Map<Integer, PolygonDescription> getPolygonTypes()
    {
        return POLYGON_TYPES;
    }

    public static class Description
    {
        private final int type;

        private final String description;

        private boolean ignoreLabel;

        private int priority;

        public Description(int type, String description, boolean ignoreLabel, int priority)
        {
            this.priority=priority;
            this.type=type;
            this.description=description;
            this.ignoreLabel=ignoreLabel;
        }

        public String getDescription()
        {
            return description;
        }

        public int getType()
        {
            return type;
        }

        public boolean isIgnoreLabel()
        {
            return ignoreLabel;
        }

        public int getPriority()
        {
            return priority;
        }
    }

    public static class PolylineDescription extends Description
    {
        private final Color color;

        private final Stroke stroke;

        public PolylineDescription(int type, String description, boolean ignoreLabel, Color color, Stroke stroke, int priority)
        {
            super(type, description, ignoreLabel, priority+POLYLINE_BASE_PRIORITY);
            // TODO Auto-generated constructor stub
            this.color=color;
            this.stroke=stroke;
        }

        public Color getColor()
        {
            return color;
        }

        public Stroke getStroke()
        {
            return stroke;
        }
    }

    public static class PolygonDescription extends Description
    {
        private final Paint paint;

        public PolygonDescription(int type, String description, boolean ignoreLabel, Paint paint, int priority)
        {
            super(type, description, ignoreLabel, priority+POLYGON_BASE_PRIORITY);
            this.paint=paint;
        }

        public Paint getPaint()
        {
            return paint;
        }
    }

    public static class PointDescription extends Description
    {
        private int subType;

        private String iconName;

        private ImageIcon icon;

        private boolean point;

        public PointDescription(int fullType, String description, boolean ignoreLabel, String iconName, boolean point, int priority)
        {
            super(fullType>>8, description, ignoreLabel, priority+POINT_BASE_PRIORITY);
            subType=fullType&0xFF;
            this.iconName=iconName;
            this.icon=null;
            this.point=point;
        }

        public int getSubType()
        {
            return subType;
        }

        public String getIconName()
        {
            return iconName;
        }

        public ImageIcon getIcon() throws IOException
        {
            if(icon==null && iconName!=null)
            {
                String name=String.format("icons/icon_%s.png", iconName);
                URL url=ImgConstants.class.getResource(name);
                if(url==null)
                {
                    iconName=null;
                    throw new IOException("Cannot load icon: "+name);
                }
                icon=new ImageIcon(url);
            }
            return icon;
        }

        public boolean hasPoint()
        {
            return point;
        }
    }
}
