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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.AWTException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeromq.ZMQ;

/**
 * Durable subscriber
 */
public class ZeroMqMouse {

    private final static Log logger = LogFactory.getLog(ZeroMqMouse.class);
    private final static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void main(String[] args) throws AWTException, java.io.IOException {
        if (args.length < 1) {
            System.err.println("Must specify ip:port value for the ZeroMQ publish server.");
            return;
        }

        ZMQ.Context context = ZMQ.context(1);

        // Connect our subscriber socket
        ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
        subscriber.setIdentity(ZeroMqMouse.class.getName().getBytes());

        subscriber.subscribe("".getBytes());
        subscriber.connect("tcp://" + args[0]);

        // Get updates, expect random Ctrl-C death
        String msg = "";
        MouseDriver md = new MouseDriver();
        while (!msg.equalsIgnoreCase("END")) {
            boolean success = false;
            byte[] recv = subscriber.recv(0);
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(recv);
                //Try reading the data as a serialized Java object:
                ObjectInput in = new ObjectInputStream(bis);
                Object o = in.readObject();
                //get the state: add/remove/update
                if (TUIO.TuioCursor.class.isAssignableFrom(o.getClass())) {
                    TUIO.TuioCursor tcur = (TUIO.TuioCursor) o;
                    switch (tcur.getTuioState()) {
                        case TUIO.TuioCursor.TUIO_ADDED:
                            md.addTuioCursor(tcur);
                            break;
                        case TUIO.TuioCursor.TUIO_REMOVED:
                            md.removeTuioCursor(tcur);
                            break;
                        default:
                            md.updateTuioCursor(tcur);
                            break;
                    }
                } else if (TUIO.TuioBlob.class.isAssignableFrom(o.getClass())) {
                    TUIO.TuioBlob tblb = (TUIO.TuioBlob) o;
                    switch (tblb.getTuioState()) {
                        case TUIO.TuioBlob.TUIO_ADDED:
                            md.addTuioBlob(tblb);
                            break;
                        case TUIO.TuioBlob.TUIO_REMOVED:
                            md.removeTuioBlob(tblb);
                            break;
                        default:
                            md.updateTuioBlob(tblb);
                            break;
                    }
                } else if (TUIO.TuioObject.class.isAssignableFrom(o.getClass())) {
                    TUIO.TuioObject tobj = (TUIO.TuioObject) o;
                    switch (tobj.getTuioState()) {
                        case TUIO.TuioObject.TUIO_ADDED:
                            md.addTuioObject(tobj);
                            break;
                        case TUIO.TuioObject.TUIO_REMOVED:
                            md.removeTuioObject(tobj);
                            break;
                        default:
                            md.updateTuioObject(tobj);
                            break;
                    }
                }
                success = true;
                bis.close();
            } catch (java.io.IOException ex) {
            } catch (ClassNotFoundException ex) {
            } finally {
            }
            //try reading the data as a string (JSON)
            if (!success) {
                msg = new String(recv);
                HashMap val = mapper.readValue(msg, HashMap.class);
                String c = (String) val.get("class");
                if (c.equals("TUIO.TuioCursor")) {
                    TUIO.TuioCursor tcur = mapper.readValue(msg, TUIO.TuioCursor.class);
                    switch (tcur.getTuioState()) {
                        case TUIO.TuioCursor.TUIO_ADDED:
                            md.addTuioCursor(tcur);
                            break;
                        case TUIO.TuioCursor.TUIO_REMOVED:
                            md.removeTuioCursor(tcur);
                            break;
                        default:
                            md.updateTuioCursor(tcur);
                            break;
                    }
                } else if (c.equals("TUIO.TuioBlob")) {
                    TUIO.TuioBlob tblb = mapper.readValue(msg, TUIO.TuioBlob.class);
                    switch (tblb.getTuioState()) {
                        case TUIO.TuioBlob.TUIO_ADDED:
                            md.addTuioBlob(tblb);
                            break;
                        case TUIO.TuioBlob.TUIO_REMOVED:
                            md.removeTuioBlob(tblb);
                            break;
                        default:
                            md.updateTuioBlob(tblb);
                            break;
                    }
                } else if (c.equals("TUIO.TuioObject")) {
                    TUIO.TuioObject tobj = mapper.readValue(msg, TUIO.TuioObject.class);
                    switch (tobj.getTuioState()) {
                        case TUIO.TuioObject.TUIO_ADDED:
                            md.addTuioObject(tobj);
                            break;
                        case TUIO.TuioObject.TUIO_REMOVED:
                            md.removeTuioObject(tobj);
                            break;
                        default:
                            md.updateTuioObject(tobj);
                            break;
                    }
                }
            }
        }
    }
}
