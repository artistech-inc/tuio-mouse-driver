/*
 * Copyright 2015 ArtisTech, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.artistech.tuio.mouse;

import TUIO.TuioPoint;
import com.artistech.protobuf.ProtoConverter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.AWTException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ServiceLoader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.zeromq.ZMQ;

/**
 * Durable subscriber
 */
public class ZeroMqMouse {

    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Main entry point for ZeroMQ integration.
     *
     * @param args
     * @throws AWTException
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws AWTException, java.io.IOException {
        //read off the TUIO port from the command line
        String zeromq_port;

        Options options = new Options();
        options.addOption("z", "zeromq-port", true, "ZeroMQ Server:Port to subscribe to. (-z localhost:5565)");
        options.addOption("h", "help", false, "Show this message.");
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLineParser parser = new org.apache.commons.cli.BasicParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("tuio-mouse-driver", options);
                return;
            } else {
                if (cmd.hasOption("z") || cmd.hasOption("zeromq-port")) {
                    zeromq_port = cmd.getOptionValue("z");
                } else {
                    System.err.println("The zeromq-port value must be specified.");
                    formatter.printHelp("tuio-mouse-driver", options);
                    return;
                }
            }
        } catch (ParseException ex) {
            System.err.println("Error Processing Command Options:");
            formatter.printHelp("tuio-mouse-driver", options);
            return;
        }

        //load conversion services
        ServiceLoader<ProtoConverter> services = ServiceLoader.load(ProtoConverter.class);

        //create zeromq context
        ZMQ.Context context = ZMQ.context(1);

        // Connect our subscriber socket
        ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
        subscriber.setIdentity(ZeroMqMouse.class.getName().getBytes());

        //this could change I guess so we can get different data subscrptions.
        subscriber.subscribe("TuioCursor".getBytes());
//        subscriber.subscribe("TuioTime".getBytes());
        subscriber.connect("tcp://" + zeromq_port);

        System.out.println("Subscribed to " + zeromq_port + " for ZeroMQ messages.");

        // Get updates, expect random Ctrl-C death
        String msg = "";
        MouseDriver md = new MouseDriver();
        while (!msg.equalsIgnoreCase("END")) {
            boolean success = false;
            byte[] recv = subscriber.recv();

            com.google.protobuf.GeneratedMessage message = null;
            TuioPoint pt = null;
            String type = recv.length > 0 ? new String(recv) : "";
            recv = subscriber.recv();
            switch (type) {
                case "TuioCursor.PROTOBUF":
                    try {
                        //it is a cursor?
                        message = com.artistech.protobuf.TuioProtos.Cursor.parseFrom(recv);
                        success = true;
                    } catch (Exception ex) {
                    }
                    break;
                case "TuioTime.PROTOBUF":
//                    try {
//                        //it is a cursor?
//                        message = com.artistech.protobuf.TuioProtos.Time.parseFrom(recv);
//                        success = true;
//                    } catch (Exception ex) {
//                    }
                    break;
                case "TuioObject.PROTOBUF":
                    try {
                        //it is a cursor?
                        message = com.artistech.protobuf.TuioProtos.Object.parseFrom(recv);
                        success = true;
                    } catch (Exception ex) {
                    }
                    break;
                case "TuioBlob.PROTOBUF":
                    try {
                        //it is a cursor?
                        message = com.artistech.protobuf.TuioProtos.Blob.parseFrom(recv);
                        success = true;
                    } catch (Exception ex) {
                    }
                    break;
                case "TuioCursor.JSON":
                    try {
                        //it is a cursor?
                        pt = mapper.readValue(recv, TUIO.TuioCursor.class);
                        success = true;
                    } catch (Exception ex) {
                    }
                    break;
                case "TuioTime.JSON":
//                    try {
//                        //it is a cursor?
//                        pt = mapper.readValue(recv, TUIO.TuioTime.class);
//                        success = true;
//                    } catch (Exception ex) {
//                    }
                    break;
                case "TuioObject.JSON":
                    try {
                        //it is a cursor?
                        pt = mapper.readValue(recv, TUIO.TuioObject.class);
                        success = true;
                    } catch (Exception ex) {
                    }
                    break;
                case "TuioBlob.JSON":
                    try {
                        //it is a cursor?
                        pt = mapper.readValue(recv, TUIO.TuioBlob.class);
                        success = true;
                    } catch (Exception ex) {
                    }
                    break;
                case "TuioTime.OBJECT":
                    break;
                case "TuioCursor.OBJECT":
                case "TuioObject.OBJECT":
                case "TuioBlob.OBJECT":
                    try {
                        //Try reading the data as a serialized Java object:
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(recv)) {
                            //Try reading the data as a serialized Java object:
                            ObjectInput in = new ObjectInputStream(bis);
                            Object o = in.readObject();
                            //if it is of type Point (Cursor, Object, Blob), process:
                            if (TuioPoint.class.isAssignableFrom(o.getClass())) {
                                pt = (TuioPoint) o;
                                process(pt, md);
                            }

                            success = true;
                        }
                    } catch (java.io.IOException | ClassNotFoundException ex) {
                    } finally {
                    }
                    break;
                default:
                    success = false;
                    break;
            }

            if (message != null && success) {
                //ok, so we have a message that is not null, so it was protobuf:
                Object o = null;

                //look for a converter that will suppor this objec type and convert:
                for (ProtoConverter converter : services) {
                    if (converter.supportsConversion(message)) {
                        o = converter.convertFromProtobuf(message);
                        break;
                    }
                }

                //if the type is of type Point (Cursor, Blob, Object), process:
                if (o != null && TuioPoint.class.isAssignableFrom(o.getClass())) {
                    pt = (TuioPoint) o;
                }
            }

            if (pt != null) {
                process(pt, md);
            }
        }
    }

    /**
     * Process a point.
     *
     * @param pt
     * @param md
     */
    public static void process(TUIO.TuioPoint pt, MouseDriver md) {
        if (TUIO.TuioCursor.class.isAssignableFrom(pt.getClass())) {
            //if it is a cursor, process:
            TUIO.TuioCursor tcur = (TUIO.TuioCursor) pt;
            switch (tcur.getTuioState()) {
                case TUIO.TuioCursor.TUIO_ADDED:
                    //cursor added:
                    md.addTuioCursor(tcur);
                    break;
                case TUIO.TuioCursor.TUIO_REMOVED:
                    //cursor removed:
                    md.removeTuioCursor(tcur);
                    break;
                default:
                    //cursor updated:
                    md.updateTuioCursor(tcur);
                    break;
            }
        } else if (TUIO.TuioBlob.class.isAssignableFrom(pt.getClass())) {
            //if it is a blob, process:
            TUIO.TuioBlob tblb = (TUIO.TuioBlob) pt;
            switch (tblb.getTuioState()) {
                case TUIO.TuioBlob.TUIO_ADDED:
                    //blob added:
                    md.addTuioBlob(tblb);
                    break;
                case TUIO.TuioBlob.TUIO_REMOVED:
                    //blob removed:
                    md.removeTuioBlob(tblb);
                    break;
                default:
                    //blob updated:
                    md.updateTuioBlob(tblb);
                    break;
            }
        } else if (TUIO.TuioObject.class.isAssignableFrom(pt.getClass())) {
            //if it is an object, process:
            TUIO.TuioObject tobj = (TUIO.TuioObject) pt;
            switch (tobj.getTuioState()) {
                case TUIO.TuioObject.TUIO_ADDED:
                    //object added:
                    md.addTuioObject(tobj);
                    break;
                case TUIO.TuioObject.TUIO_REMOVED:
                    //object removed:
                    md.removeTuioObject(tobj);
                    break;
                default:
                    //object updated:
                    md.updateTuioObject(tobj);
                    break;
            }
        }
    }
}
