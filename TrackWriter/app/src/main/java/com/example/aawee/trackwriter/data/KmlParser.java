package com.example.aawee.trackwriter.data;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.example.aawee.trackwriter.GpsPoint;
import com.example.aawee.trackwriter.GpsTrack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Aawee on 7/02/2017.
 */

public class KmlParser {



    public static GpsTrack parseOneTrackKml(Context context, android.net.Uri filedir) {
        // list of points that we'll be filling
        ArrayList<GpsPoint> listPts = new ArrayList<GpsPoint>();
        String trackName = "New track"; // will be changed if parse is successful

        try {
            // open file and parse it
            //InputStream is = context.getAssets().open("testdata.kml");
            InputStream is = context.getContentResolver().openInputStream(filedir);
            //InputStream is = new FileInputStream(filedir);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            // get nodes
            doc.getDocumentElement().normalize();
            Element rootElement = doc.getDocumentElement();
            NodeList placemarkList = rootElement.getElementsByTagName("Placemark");

            for (int i=0; i<placemarkList.getLength(); i++) { // go through all placemarks
                Element placemark = (Element) placemarkList.item(i);
                // get track's name (placemark is the kml format keyword, equivalent to track in our case)
                trackName = placemark.getElementsByTagName("name").item(0).getTextContent();

                NodeList pointList = placemark.getElementsByTagName("Point");
                for (int j=0; j<pointList.getLength(); j++) { // go through all the points
                    Element point = (Element) pointList.item(j);

                    // get coordinates from string where they are separated by comma
                    String coords = point.getElementsByTagName("coordinates").item(0).getTextContent();
                    String[] coordspt = coords.split(",");
                    double lat = Double.parseDouble(coordspt[0]);
                    double lon = Double.parseDouble(coordspt[1]);

                    double acc = Double.valueOf( point.getElementsByTagName("accuracy").item(0).getTextContent() );
                    double brg = Double.valueOf( point.getElementsByTagName("bearing").item(0).getTextContent() );
                    double spd = Double.valueOf( point.getElementsByTagName("speed").item(0).getTextContent() );

                    // get time of the receiving location
                    long timeCrt = Long.valueOf( point.getElementsByTagName("time").item(0).getTextContent() );
                    GpsPoint gpsPoint = new GpsPoint(lat,lon, acc, brg, spd, timeCrt);
                    listPts.add(gpsPoint);
                }

                //Log.d("PRSnot", "Placemark " + placemarkList.item(i).getA  + " \n");

            }


        } catch (Exception e) {
            Log.e("PRSerr", "Error: " + e.getMessage());
            trackName = "";
        }

        return new GpsTrack(trackName, listPts);
    }

    public static File exportTrackToKml(GpsTrack gpsTrack) {
        File file = null;

        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

        // get points
        try {
            serializer.setOutput(writer);

            serializer.startDocument("UTF-8", true);
            serializer.text("\n");
            serializer.startTag("", "kml");
            serializer.text("\n");
            serializer.startTag("", "Document");
            serializer.text("\n");

            serializer.startTag("", "Placemark");
            serializer.text("\n");
            serializer.startTag("", "name");
            serializer.text(gpsTrack.getTrackName());
            serializer.endTag("", "name");
            serializer.text("\n");

            for(GpsPoint pt: gpsTrack.getPoints()) {
                serializer.startTag("", "Point");
                serializer.text("\n");
                serializer.startTag("", "coordinates");
                serializer.text(Double.toString(pt.getLatitude()) + "," + Double.toString(pt.getLongitude()) + "," + 0);
                serializer.endTag("", "coordinates");
                serializer.text("\n");

                serializer.startTag("", "accuracy");
                serializer.text(Double.toString(pt.getAccuracy()));
                serializer.endTag("", "accuracy");
                serializer.text("\n");
                serializer.startTag("", "bearing");
                serializer.text(Double.toString(pt.getBearing()));
                serializer.endTag("", "bearing");
                serializer.text("\n");
                serializer.startTag("", "speed");
                serializer.text(Double.toString(pt.getSpeed()));
                serializer.endTag("", "speed");
                serializer.text("\n");

                serializer.startTag("", "time");
                serializer.text(Long.toString(pt.getTimeCreated()));
                serializer.endTag("", "time");
                serializer.text("\n");
                serializer.endTag("", "Point");
                serializer.text("\n");
            }
            serializer.endTag("", "Placemark");
            serializer.text("\n");

            serializer.endTag("", "Document");
            serializer.text("\n");
            serializer.endTag("", "kml");
            serializer.text("\n");
            serializer.endDocument();

            Log.d("PRSnot", writer.toString());

            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "track.kml");
            FileWriter fw = new FileWriter(file);
            fw.write(writer.toString());
            fw.close();

            Log.d("PRSnot", "Temporarily saved contents in " + file.getPath());
        }
        catch (Exception e) {
            Log.e("PRSerr", "Error: " + e.getMessage());
        }

        return file;

    }



}
