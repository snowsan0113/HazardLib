package snowsan0113.hazardlib.manager.drawmap;

import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.iso.io.wkt.Coordinate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.renderer.lite.RendererUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import snowsan0113.hazardlib.api.tsunami.TsunamiType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DrawMapManager {

    public static BufferedImage getQuakeCityMap(List<Filter> draw_areaList, Map<Color, List<Filter>> draw_colormap) throws IOException {
        //府県予報区
        File eew = new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\AreaForecastLocalM_prefecture_GIS\\府県予報区等.shp");
        MapBuilder eew_map = new MapBuilder(eew, 1280, 1080)
                .addStroke(Color.BLACK, 3f, null);
        List<ReferencedEnvelope> envelope_list = new ArrayList<>();
        for (Filter filter : draw_areaList) {
            ReferencedEnvelope area = MapBuilder.getAreaEnvelope(eew_map.getMapFeatureSource(), filter); //都道府県ごとのReferencedEnvelopeを取得する
            envelope_list.add(area); //都道府県ごとのReferencedEnvelopeを追加
        }
        eew_map.setDrawArea(envelope_list); //描画範囲を決定する
        ImageIO.write(eew_map.build(), "png", new File("aaaa.png"));

        //地震_細分
        File file = new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\AreaInformationCity_quake_GIS\\市町村等（地震津波関係）.shp");
        MapBuilder builder = new MapBuilder(file, 1280, 1080)
                .addFill(Color.LIGHT_GRAY, 1f, null);
        builder.setDrawArea(envelope_list);
        for (Map.Entry<Color, List<Filter>> entry : draw_colormap.entrySet()) {
            Color color = entry.getKey();
            for (Filter filter : entry.getValue()) {
                builder.addFill(color, 1f, filter);
            }
        }

        builder.getMap().addLayer(eew_map.getMap().layers().getFirst());

        return builder.build();
    }

    public static BufferedImage getQuakeLocalAreaMap(List<Filter> draw_areaList, Map<Color, List<Filter>> draw_colormap, Coordinate coordinate) throws IOException {
        //府県予報区
        File eew = new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\AreaForecastLocalM_prefecture_GIS\\府県予報区等.shp");
        MapBuilder eew_map = new MapBuilder(eew, 1280, 1080);
        List<ReferencedEnvelope> envelope_list = new ArrayList<>();
        for (Filter filter : draw_areaList) {
            ReferencedEnvelope area = MapBuilder.getAreaEnvelope(eew_map.getMapFeatureSource(), filter); //都道府県ごとのReferencedEnvelopeを取得する
            envelope_list.add(area); //都道府県ごとのReferencedEnvelopeを追加
        }
        eew_map.setDrawArea(envelope_list); //描画範囲を決定する

        //地震_細分
        File file = new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\AreaForecastLocalE_GIS\\地震情報／細分区域.shp");
        MapBuilder builder = new MapBuilder(file, 1280, 1080)
                .addFill(Color.LIGHT_GRAY, 1f, null);
        builder.setDrawArea(envelope_list);
        for (Map.Entry<Color, List<Filter>> entry : draw_colormap.entrySet()) {
            Color color = entry.getKey();
            for (Filter filter : entry.getValue()) {
                builder.addFill(color, 1f, filter);
            }
        }

        if (coordinate != null) {
            //震源の画像を張る場合
            BufferedImage epicenter = ImageIO.read(new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\震源.png"));
            builder.addImage(epicenter, coordinate);
        }

        BufferedImage image = builder.build();
        BufferedImage add_image = ImageIO.read(new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\5強.png"));
        for (List<Filter> filterList : draw_colormap.values()) {
            for (Filter filter : filterList) {
                SimpleFeatureCollection source_filter = builder.getMapFeatureSource().getFeatures(filter);

                List<Geometry> geometries = new ArrayList<>();
                try (SimpleFeatureIterator it = source_filter.features()) {
                    while (it.hasNext()) {
                        Object geomObj = it.next().getDefaultGeometry();
                        if (geomObj instanceof Geometry) {
                            geometries.add((Geometry) geomObj);
                        }
                    }
                }
                GeometryCollection geometryCollection = new GeometryFactory().createGeometryCollection(
                        geometries.toArray(new Geometry[0])
                );
                Geometry centroidGeom = geometryCollection.union().getInteriorPoint();

                org.locationtech.jts.geom.Coordinate coordinatee = centroidGeom.getCoordinate();
                AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(builder.getEnvelope(), new Rectangle(builder.getWidth(), builder.getHeight()));
                Point2D screenPoint = worldToScreen.transform(new Point2D.Double(coordinatee.x, coordinatee.y), null);
                int sc_x = (int) (screenPoint.getX() - ((double) add_image.getWidth() / 2));
                int sc_y = (int) (screenPoint.getY() - ((double) add_image.getHeight() / 2));
                image.createGraphics().drawImage(add_image, sc_x, sc_y, null);
                System.out.println(sc_x + ":::" + sc_y);
            }
        }

        return image;
    }

    public static BufferedImage getCustomTsunamiMap(List<Filter> draw_areaList, Map<TsunamiType, List<Filter>> draw_colormap) throws IOException {
        //府県予報区
        File eew = new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\AreaForecastLocalM_prefecture_GIS\\府県予報区等.shp");
        MapBuilder eew_map = new MapBuilder(eew, 1280, 1080)
                .addFill(Color.LIGHT_GRAY, 1f, null); //全部に色を塗る
        List<ReferencedEnvelope> envelope_list = new ArrayList<>();
        for (Filter filter : draw_areaList) {
            ReferencedEnvelope area = MapBuilder.getAreaEnvelope(eew_map.getMapFeatureSource(), filter); //都道府県ごとのReferencedEnvelopeを取得する
            envelope_list.add(area); //都道府県ごとのReferencedEnvelopeを追加
            System.out.println(area.toString());
        }
        eew_map.setDrawArea(envelope_list); //描画範囲を決定する
        BufferedImage eew_image = eew_map.build();
        ImageIO.write(eew_image, "png", new File("AreaForecastLocalM_prefecture.png"));

        //津波予報区
        File tsunami_file = new File("G:\\開発\\snow\\HazardLib\\src\\main\\resources\\AreaTsunami_GIS\\津波予報区.shp");
        MapBuilder tsunami_map = new MapBuilder(tsunami_file, eew_image.getWidth(), eew_image.getHeight())
                .addStroke(Color.LIGHT_GRAY, 3f, null); //全部に色を塗る
        for (Map.Entry<TsunamiType, List<Filter>> entry : draw_colormap.entrySet()) {
            TsunamiType type = entry.getKey(); //津波のタイプ
            List<Filter> values = entry.getValue(); //津波のタイプごとのフィルダーリスト（塗りたい場所）
            for (Filter filter : values) {
                tsunami_map.addStroke(type.getColor(), 6f, filter); //津波のタイプごとに色を塗る
            }
        }

        List<Layer> layers = tsunami_map.getMap().layers(); //津波GISから、レイヤーリストを取得する
        for (int n = 1; n < layers.size(); n++) {
            eew_map.getMap().addLayer(layers.get(n)); //府県予報区等GISに、津波GISレイヤーを追加する（ふち取りがある状態に）
        }
        //tsunami_map.getMap().addLayer(eew_map.getMap().layers().getFirst());
        // tsunami_map.setEnvelope(eew_map.getEnvelope());

        ImageIO.write(eew_map.build(), "png", new File("tsunamiii.png"));
        return tsunami_map.build();
    }

    public enum MapType {
        AREA_FORECAST_LOCAL_EEW("緊急地震速報_府県予報区"),
        AREA_FORECAST_LOCAL_PREFECTURE("府県予報区等"),
        AREA_TSUNAMI("津波予報区");

        private final String name;

        MapType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
