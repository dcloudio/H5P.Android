package io.dcloud.feature.weex_amap.adapter;



public class Constant {

    public interface Value {
        int SCROLLGESTURE = 0x1;
        int ZOOMGESTURE = 0x1 << 1;
        int TILTGESTURE = 0x1 << 2;
        int ROTATEGESTURE = 0x1 << 3;
        String RIGHT_CENTER = "center";
        String RIGHT_BOTTOM = "bottom";
    }

    public interface JSONKEY {
        String ID = "id";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String TITLE = "title";
        String ZINDEX = "zIndex";
        String ICONPATH = "iconPath";
        String ROTATE = "rotate";
        String ALPHE = "alpha";
        String WIDTH = "width";
        String HEIGHT = "height";
        String CALLOUT = "callout";
        String LABEL = "label";
        String ANCHOR = "anchor";
        String ARIA_LABEL = "aria-label";

    }

    public interface Name {

        // mapview
        String SCALE = "scale";
        String ENABLE3D = "enable3D";
        String SHOW_COMPASS = "showCompass";
        String ENABLE_ZOOM = "enableZoom";
        String ENABLE_SCROLL = "enableScroll";
        String ENABLE_ROTATE = "enableRotate";
        String ENABLE_OVERLOOKING = "enableOverlooking";
        String KEYS = "subkey";
        String MARKERS = "markers";
        String POLYLINE = "polyline";
        String POLYGONS = "polygons";
        String CIRCLES = "circles";
        String CONTROLS = "controls";
        String SHOW_LOCATION = "showLocation";
        String INCLUDE_POINTS = "includePoints";
        String ROTATE = "rotate";
        String SKEW = "skew";
        String ENABLE_SATELLITE = "enableSatellite";
        String ENABLE_TRAFFIC = "enableTraffic";
        String SHOW_SCALE = "showScale";


        // marker
        String MARKER = "marker";
        String POSITION = "position";
        String ICON = "icon";
        String TITLE = "title";
        String HIDE_CALL_OUT = "hideCallout";


        String GEOLOCATION = "geolocation";
        String GESTURE = "gesture";
        String GESTURES = "gestures";
        String INDOORSWITCH = "indoorswitch";

        String ZOOM_POSITION = "zoomPosition";
        String MY_LOCATION_ENABLED = "myLocationEnabled";
        String SHOW_MY_LOCATION = "showMyLocation";
        String CUSTOM_STYLE_PATH = "customStylePath";
        String CUSTOM_ENABLED = "customEnabled";




        // polyline
        String PATH = "path";
        String STROKE_COLOR = "strokeColor";
        String STROKE_WIDTH = "strokeWidth";
        String STROKE_OPACITY = "strokeOpacity";
        String STROKE_STYLE = "strokeStyle";

        // circle
        String RADIUS = "radius";
        String FILL_COLOR = "fillColor";

        // offset
        String OFFSET = "offset";
        String OPEN = "open";
    }


    public static interface EVENT {
        String ZOOM_CHANGE = "zoomchange";
        String DRAG_CHANGE = "dragend";
        String BINDTAP = "tap";
        String UPDATED = "updated";
        String BINDREGION_CHANGE = "regionchange";
        String BIND_MARKER_TAP = "markertap";
        String BIND_CALLOUT_TAP = "callouttap";
        String BIND_POI_TAP = "poitap";
        String BIND_CONTROL_TAP = "controltap";
    }
}
