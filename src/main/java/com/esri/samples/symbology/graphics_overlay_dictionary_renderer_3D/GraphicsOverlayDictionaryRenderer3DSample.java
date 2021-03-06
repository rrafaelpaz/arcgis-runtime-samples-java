/*
 * Copyright 2018 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.esri.samples.symbology.graphics_overlay_dictionary_renderer_3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Multipoint;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.DictionaryRenderer;
import com.esri.arcgisruntime.symbology.DictionarySymbolStyle;
import static org.joox.JOOX.$;

public class GraphicsOverlayDictionaryRenderer3DSample extends Application {

  private SceneView sceneView;
  private GraphicsOverlay graphicsOverlay;

  @Override
  public void start(Stage stage) {
    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene fxScene = new Scene(stackPane);

      // set title, size, and add scene to stage
      stage.setTitle("Graphics Overlay Dictionary Renderer 3D Sample");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(fxScene);
      stage.show();

      // create a scene and add a basemap to it
      sceneView = new SceneView();
      ArcGISScene scene = new ArcGISScene(Basemap.Type.IMAGERY);
      sceneView.setArcGISScene(scene);

      graphicsOverlay = new GraphicsOverlay(GraphicsOverlay.RenderingMode.DYNAMIC);
      sceneView.getGraphicsOverlays().add(graphicsOverlay);

      // create symbol dictionary from specification
      // the specification style file comes with the SDK in the resources/symbols directory
      // see this directory for other specification types
      DictionarySymbolStyle symbolDictionary = new DictionarySymbolStyle("mil2525d");

      // tells graphics overlay how to render graphics with symbol dictionary attributes set
      DictionaryRenderer renderer = new DictionaryRenderer(symbolDictionary);
      graphicsOverlay.setRenderer(renderer);

      // parse graphic attributes from a XML file following the mil2525d specification
      List<Map<String, Object>> messages = parseMessages();

      // create graphics with attributes and add to graphics overlay
      messages.stream()
          .map(GraphicsOverlayDictionaryRenderer3DSample::createGraphic)
          .collect(Collectors.toCollection(() -> graphicsOverlay.getGraphics()));

      // when the scene loads and the sceneview has a spatial reference, move the camera to show the graphics
      sceneView.addSpatialReferenceChangedListener(e -> {
        if (sceneView.getSpatialReference() != null) {
          sceneView.setViewpointCamera(new Camera(graphicsOverlay.getExtent().getCenter(), 15000, 0, 70, 0));
        }
      });

      // add the scene view to the stack pane
      stackPane.getChildren().add(sceneView);
    } catch (Exception ex) {
      // on any exception, print the stacktrace
      ex.printStackTrace();
    }
  }

  /**
   * Parses a XML file following the mil2525d specification and creates a message for each block of attributes found.
   */
  private List<Map<String, Object>> parseMessages() throws Exception {
    final List<Map<String, Object>> messages = new ArrayList<>();
    $(getClass().getResourceAsStream("/symbols/Mil2525DMessages.xml")) // $ reads the file
        .find("message")
        .each()
        .forEach(message -> {
          Map<String, Object> attributes = new HashMap<>();
          message.children().forEach(attr -> attributes.put(attr.getNodeName(), attr.getTextContent()));
          messages.add(attributes);
        });

    return messages;
  }

  /**
   * Creates a graphic using a symbol dictionary and the attributes that were passed.
   *
   * @param attributes tells symbol dictionary what symbol to apply to graphic
   */
  private static Graphic createGraphic(Map<String, Object> attributes) {
    // get spatial reference
    int wkid = Integer.parseInt((String) attributes.get("_wkid"));
    SpatialReference sr = SpatialReference.create(wkid);

    // get points from the coordinate string in the "_control_points" attribute (delimited with ';')
    String[] coordinates = ((String) attributes.get("_control_points")).split(";");
    List<Point> points = Stream.of(coordinates)
        .map(cs -> cs.split(",")) // get each ordinate
        .map(c -> new Point(Double.valueOf(c[0]), Double.valueOf(c[1]), sr))// create a Point with the ordinates
        .map(c -> (Point) GeometryEngine.project(c, SpatialReferences.getWgs84())) // project to display in scene
        .collect(Collectors.toList());

    // create a point collection with the points
    PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());
    pointCollection.addAll(points);

    // remove unneeded attributes, use geometry for graphic positioning instead
    attributes.remove("_control_points");
    attributes.remove("_wkid");

    // return a graphic with a multipoint geometry (some have more than one point)
    return new Graphic(new Multipoint(pointCollection), attributes);
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {

    if (sceneView != null) {
      sceneView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }
}
