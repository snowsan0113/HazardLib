package snowsan0113.hazardlib.manager.drawmap;

import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.style.*;
import org.geotools.api.style.Stroke;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.iso.io.wkt.Coordinate;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.StyleBuilder;
import org.locationtech.jts.geom.Envelope;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MapBuilder {

    //マップ情報
    private DrawMapManager.MapType type;
    private SimpleFeatureSource featureSource;
    private MapContent map;
    private SimpleFeatureCollection map_featureCollection;
    private ReferencedEnvelope envelope;

    //画像情報
    private int width;
    private int height;
    private BufferedImage image;
    private Graphics2D graphics;

    public MapBuilder(File file, int width, int height) {
        try {
            //マップ情報
            this.featureSource = getFileFeatureSource(file);
            this.map = new MapContent();
            this.map_featureCollection = featureSource.getFeatures();
            this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            this.graphics = image.createGraphics();

            //画像情報
            this.width = width;
            this.height = height;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MapBuilder addStroke(Color color, float thickness, Filter filter) {
        if (filter != null) {
            map.addLayer(new FeatureLayer(getMapFeatureCollection(filter), createLineStyle(color, thickness)));
        }
        else {
            map.addLayer(new FeatureLayer(featureSource, createLineStyle(color, thickness)));
        }
        return this;
    }

    public MapBuilder addFill(Color color, float opacity, Filter filter) {
        if (filter != null) {
            map.addLayer(new FeatureLayer(getMapFeatureCollection(filter), createStyle(null, 0f, color, opacity)));
        }
        else {
            map.addLayer(new FeatureLayer(featureSource, createStyle(null, 0f, color, opacity)));
        }
        return this;
    }

    public MapBuilder addImage(BufferedImage add_image, Coordinate coordinate) {
        AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(envelope, new Rectangle(width, height));
        Point2D screenPoint = worldToScreen.transform(new Point2D.Double(coordinate.x, coordinate.y), null);
        int sc_x = (int) (screenPoint.getX() - ((double) add_image.getWidth() / 2));
        int sc_y = (int) (screenPoint.getY() - ((double) add_image.getHeight() / 2));
        graphics.drawImage(add_image, sc_x, sc_y, null);
        System.out.println(sc_x + ":" + sc_y);

        return this;
    }

    public MapBuilder setDrawArea(List<ReferencedEnvelope> drawAreas) {
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE;

        for (ReferencedEnvelope env : drawAreas) {
            if (env.getMinX() < xMin)  xMin = env.getMinX();
            if (env.getMaxX() > xMax) xMax = env.getMaxX();
            if (env.getMinY() < yMin) yMin = env.getMinY();
            if (env.getMaxY() > yMax) yMax = env.getMaxY();
        }

        double marginX = (xMax - xMin) * 0.01;
        double marginY = (yMax - yMin) * 0.01;

        xMin -= marginX;
        xMax += marginX;
        yMin -= marginY;
        yMax += marginY;

        double targetWidth = xMax - xMin;
        double targetHeight = yMax - yMin;
        double mapRatio = (double) width / height;
        double targetRatio = targetWidth / targetHeight;

        // 縦横比を維持するために縦か横を拡張
        if (targetRatio > mapRatio) {
            double centerY = (yMax + yMin) / 2;
            double newHalfHeight = (targetWidth / mapRatio) / 2;
            yMin = centerY - newHalfHeight;
            yMax = centerY + newHalfHeight;
        } else {
            double centerX = (xMax + xMin) / 2;
            double newHalfWidth = (targetHeight * mapRatio) / 2;
            xMin = centerX - newHalfWidth;
            xMax = centerX + newHalfWidth;
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(
                xMin, xMax,
                yMin, yMax,
                DefaultGeographicCRS.WGS84
        );

        setEnvelope(envelope);

        return this;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Graphics2D getGraphics() {
        return graphics;
    }

    public ReferencedEnvelope getEnvelope() {
        return envelope;
    }

    public MapBuilder setEnvelope(ReferencedEnvelope envelope) {
        this.envelope = envelope;
        System.out.println(featureSource.getName() + ":" + envelope);
        return this;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BufferedImage build() {
        if (envelope == null) envelope = map.layers().getFirst().getBounds();
        graphics.setPaint(new Color(255, 255, 255, 0));
        graphics.fillRect(0, 0, width, height);
        //graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);
        renderer.paint(graphics, new Rectangle(width, height), envelope);

        return image;
    }

    public void closeFile() {
        graphics.dispose();
        map.dispose();
    }

    public SimpleFeatureCollection getMapFeatureCollection(Filter filter) {
        if (filter != null) {
            try {
                return featureSource.getFeatures(filter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            return map_featureCollection;
        }
    }

    public MapContent getMap() {
        return map;
    }

    public SimpleFeatureSource getMapFeatureSource() {
        return featureSource;
    }

    public static Style createLineStyle(Color stroke_color, float stroke_thickness) {
        StyleBuilder style_builder = new StyleBuilder();
        LineSymbolizer symbolizer = style_builder.createLineSymbolizer(stroke_color, stroke_thickness);
        Rule rule = style_builder.createRule(symbolizer);
        FeatureTypeStyle featureTypeStyle = style_builder.createFeatureTypeStyle(null, rule);
        Style style = style_builder.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);
        return style;
    }

    public static Style createStyle(Color stroke_color, float stroke_thickness, Color fill_color, float fill_opacity) {
        StyleBuilder style_builder = new StyleBuilder();
        Stroke stroke = style_builder.createStroke(stroke_color, stroke_thickness);
        Fill fill = style_builder.createFill(fill_color, fill_opacity);
        PolygonSymbolizer symbolizer = style_builder.createPolygonSymbolizer(stroke, fill);
        Rule rule = style_builder.createRule(symbolizer);
        FeatureTypeStyle featureTypeStyle = style_builder.createFeatureTypeStyle(null, rule);
        Style style = style_builder.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);
        return style;
    }

    public static SimpleFeatureSource getFileFeatureSource(File file) throws IOException {
        ShapefileDataStore store = (ShapefileDataStore) FileDataStoreFinder.getDataStore(file);
        store.setCharset(StandardCharsets.UTF_8);
        return store.getFeatureSource();
    }

    public static ReferencedEnvelope getAreaEnvelope(SimpleFeatureSource source, Filter filter) throws IOException {
        SimpleFeatureCollection features = source.getFeatures(filter);
        return features.getBounds();
    }
}
