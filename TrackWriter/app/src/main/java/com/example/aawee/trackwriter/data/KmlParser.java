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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import static java.lang.Double.parseDouble;

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
                    double lat = parseDouble(coordspt[0]);
                    double lon = parseDouble(coordspt[1]);

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


    public static GpsTrack readFromTxt (Context context) {

        ArrayList<GpsPoint> listPts = new ArrayList<GpsPoint>();
        String trackName = "Test track"; // will be changed if parse is successful


        try {
            InputStream is = context.getAssets().open("locations.txt");
            DataInputStream in = new DataInputStream(is);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            while ((strLine = br.readLine()) != null) {

                Matcher m = Pattern.compile("^D/MTSC: (\\d{4}\\d{2}\\d{2} \\d{6}\\.\\d{3}\\+\\d{4}).*" +
                        "\\[gps (\\d+\\.\\d+),(\\d+\\.\\d+) acc=(\\d+\\.\\d+).* alt=(\\d+\\.\\d+).*" +
                        " vel=(\\d+\\.\\d+).* bear=(\\d+\\.\\d+).*").matcher(strLine);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HHmmss.SSSZ");
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

                while (m.find()) {
                    Date date = sdf.parse(m.group(1).toString());
                    double lat = parseDouble(m.group(2));
                    double lon = parseDouble(m.group(3));
                    double acc = parseDouble(m.group(4));
                    double spd = Double.parseDouble(m.group(6));
                    double brg = Double.parseDouble(m.group(7));

                    //Log.d("PRSnot", m.group(1).toString()  + " Lat Lon: " +  Double.toString(lat) + " " + Double.toString(lon) +
                    //        " " + Double.toString(acc) + " " + Double.toString(spd) + " " + Double.toString(brg));

                    long time = date.getTime();
                    //if ( (time > 1485827938277L) && (time < 1485838472526L)) {
                        GpsPoint gpsPoint = new GpsPoint(lat, lon, acc, brg, spd, date.getTime());
                        listPts.add(gpsPoint);
                    //}

                }



//                if (Pattern.matches(input, strLine)) {
//                    Pattern p = Pattern.compile("'(.*?)'");
//                    Matcher m = p.matcher(strLine);
//                    while (m.find()) {
//                        String b = m.group(1);
//                        String c = b.toString() + ".*";
//                        System.out.println(b);
//
//                        if (Pattern.matches(c, strLine)) {
//                            Pattern pat = Pattern.compile("<(.*?)>");
//                            Matcher mat = pat.matcher(strLine);
//                            while (mat.find()) {
//                                System.out.println(m.group(1));
//
//                            }
//                        } else {
//                            System.out.println("Not found");
//                        }
//                    }
//                }





            }
        } catch (Exception e) {
            Log.e("PRSerr", e.getMessage());
        }

        return new GpsTrack(trackName, listPts);
    }



}
