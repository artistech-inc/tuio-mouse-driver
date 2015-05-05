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
import TUIO.TuioObject;
import TUIO.TuioPoint;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.GeneratedMessage;
import java.awt.AWTException;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ClassUtils;
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
                if (TuioPoint.class.isAssignableFrom(o.getClass())) {
                    TuioPoint pt = (TuioPoint) o;
                    process(pt, md);
                }

                success = true;
                bis.close();
            } catch (java.io.IOException ex) {
            } catch (ClassNotFoundException ex) {
            } finally {
            }

            if (!success) {
//                String str = new String(recv);

                com.google.protobuf.GeneratedMessage message = null;
                try {
                    message = TUIO.TuioProtos.Cursor.parseFrom(recv);
                    success = true;
                } catch (Exception ex) {
                }
                if (!success) {
                    try {
                        message = TUIO.TuioProtos.Blob.parseFrom(recv);
                        success = true;
                    } catch (Exception ex) {
                    }
                }
                if (!success) {
                    try {
                        message = TUIO.TuioProtos.Object.parseFrom(recv);
                        success = true;
                    } catch (Exception ex) {
                    }
                }
                if (!success) {
                    try {
                        message = TUIO.TuioProtos.Time.parseFrom(recv);
                        success = true;
                    } catch (Exception ex) {
                    }
                }
                if (message == null) {
                    success = false;
                }
                if (success) {
                    Object o = convertFromProtobuf(message);
                    if (o != null && TuioPoint.class.isAssignableFrom(o.getClass())) {
                        TuioPoint pt = (TuioPoint) o;
                        process(pt, md);
                        success = true;
                    }
                }
            }

            //try reading the data as a string (JSON)
            if (!success) {
                msg = new String(recv);
                HashMap val = mapper.readValue(msg, HashMap.class);
                String c = (String) val.get("class");
                TuioPoint pt = null;
                if (c.equals("TUIO.TuioCursor")) {
                    pt = mapper.readValue(msg, TUIO.TuioCursor.class);
                } else if (c.equals("TUIO.TuioBlob")) {
                    pt = mapper.readValue(msg, TUIO.TuioBlob.class);
                } else if (c.equals("TUIO.TuioObject")) {
                    pt = mapper.readValue(msg, TUIO.TuioObject.class);
                }
                if (pt != null) {
                    process(pt, md);
                    success = true;
                }
            }
        }
    }

    public static void process(TUIO.TuioPoint pt, MouseDriver md) {
        if (TUIO.TuioCursor.class.isAssignableFrom(pt.getClass())) {
            TUIO.TuioCursor tcur = (TUIO.TuioCursor) pt;
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
        } else if (TUIO.TuioBlob.class.isAssignableFrom(pt.getClass())) {
            TUIO.TuioBlob tblb = (TUIO.TuioBlob) pt;
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
        } else if (TUIO.TuioObject.class.isAssignableFrom(pt.getClass())) {
            TUIO.TuioObject tobj = (TUIO.TuioObject) pt;
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

    public static Object convertFromProtobuf(GeneratedMessage obj) {
        Object target;

        if (TUIO.TuioProtos.Blob.class.isAssignableFrom(obj.getClass())) {
            target = new TUIO.TuioBlob();
        } else if (TUIO.TuioProtos.Time.class.isAssignableFrom(obj.getClass())) {
            target = new TUIO.TuioTime();
        } else if (TUIO.TuioProtos.Cursor.class.isAssignableFrom(obj.getClass())) {
            target = new TuioCursor();
        } else if (TUIO.TuioProtos.Object.class.isAssignableFrom(obj.getClass())) {
            target = new TuioObject();
        } else {
            return null;
        }

        try {
            PropertyDescriptor[] targetProps = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();

            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] messageProps = beanInfo.getPropertyDescriptors();
            Method[] methods = obj.getClass().getMethods();

            for (PropertyDescriptor targetProp : targetProps) {
                for (PropertyDescriptor messageProp : messageProps) {
                    if (targetProp.getName().equals(messageProp.getName())) {
                        Method writeMethod = targetProp.getWriteMethod();
                        Method readMethod = null;
                        for (Method m : methods) {
                            if (writeMethod != null && m.getName().equals(writeMethod.getName().replaceFirst("set", "get"))) {
                                readMethod = m;
                                break;
                            }
                        }
                        try {
                            if (readMethod != null && targetProp.getReadMethod() != null) {
//                                System.out.println("Prop2 Name!: " + messageProp.getName());
                                boolean primitiveOrWrapper = ClassUtils.isPrimitiveOrWrapper(targetProp.getReadMethod().getReturnType());

                                if (primitiveOrWrapper) {
                                    writeMethod.invoke(target, messageProp.getReadMethod().invoke(obj));
                                } else {
                                    if (GeneratedMessage.class.isAssignableFrom(messageProp.getReadMethod().getReturnType())) {
                                        GeneratedMessage invoke = (GeneratedMessage) messageProp.getReadMethod().invoke(obj);
                                        Object val = convertFromProtobuf(invoke);
                                        writeMethod.invoke(target, val);
                                    }
//                                    System.out.println("Prop1 Name!: " + targetProp.getName());
                                }
                            }
                        } catch (NullPointerException ex) {
                            //Logger.getLogger(ZeroMqMouse.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalArgumentException ex) {
                            logger.error(ex);
                        } catch (SecurityException ex) {
                            logger.error(ex);
                        } catch (IllegalAccessException ex) {
                            logger.error(ex);
                        } catch (InvocationTargetException ex) {
                            logger.error(ex);
                        }
                        break;
                    }
                }
            }
        } catch (java.beans.IntrospectionException ex) {
            logger.fatal(ex);
        }
        return target;
    }

}
