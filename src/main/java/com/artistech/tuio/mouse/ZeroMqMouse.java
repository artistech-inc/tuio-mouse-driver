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

import TUIO.TuioCursor;
import java.awt.AWTException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException.IOException;

/**
 * Durable subscriber
 */
public class ZeroMqMouse {
    
    private final static Log logger = LogFactory.getLog(ZeroMqMouse.class);

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
            byte[] recv = subscriber.recv(0);
            ByteArrayInputStream bis = new ByteArrayInputStream(recv);
            ObjectInput in = null;
            boolean success = false;
            try {
                //Try reading the data as a serialized Java object:
                in = new ObjectInputStream(bis);
                Object o = in.readObject();
                //get the state: add/remove/update
                if (TuioCursor.class.isAssignableFrom(o.getClass())) {
                    TuioCursor tcur = (TuioCursor) o;
                    switch (tcur.getTuioState()) {
                        case TuioCursor.TUIO_ADDED:
                            md.addTuioCursor(tcur);
                            break;
                        case TuioCursor.TUIO_REMOVED:
                            md.removeTuioCursor(tcur);
                            break;
                        default:
                            md.updateTuioCursor(tcur);
                            break;
                    }
                }
                success = true;
            } catch (java.io.IOException ex) {
                logger.error(null, ex);
            } catch (ClassNotFoundException ex) {
                logger.error(null, ex);
            } finally {
                try {
                    bis.close();
                } catch (IOException ex) {
                    // ignore close exception
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
            //try reading the data as a string (JSON)
            if (!success) {
                msg = new String(recv);
                System.out.println(msg);
            }
        }
    }
}
