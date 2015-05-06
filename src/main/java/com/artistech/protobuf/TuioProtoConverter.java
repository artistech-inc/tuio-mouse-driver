package com.artistech.protobuf;

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
import com.google.protobuf.GeneratedMessage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author matta
 */
public class TuioProtoConverter implements ProtoConverter {

    private static final Log logger = LogFactory.getLog(TuioProtoConverter.class);

    public TuioProtoConverter() {
    }
    
    @Override
    public GeneratedMessage.Builder convertToProtobuf(Object obj) {
        GeneratedMessage.Builder builder;

        if (obj.getClass().getName().equals(TUIO.TuioTime.class.getName())) {
            builder = TuioProtos.Time.newBuilder();
        } else if (obj.getClass().getName().equals(TUIO.TuioCursor.class.getName())) {
            builder = TuioProtos.Cursor.newBuilder();
        } else if (obj.getClass().getName().equals(TUIO.TuioObject.class.getName())) {
            builder = TuioProtos.Object.newBuilder();
        } else if (obj.getClass().getName().equals(TUIO.TuioBlob.class.getName())) {
            builder = TuioProtos.Blob.newBuilder();
        } else {
            return null;
        }

        try {
            PropertyDescriptor[] objProps = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
            BeanInfo beanInfo = Introspector.getBeanInfo(builder.getClass());
            PropertyDescriptor[] builderProps = beanInfo.getPropertyDescriptors();
            Method[] methods = builder.getClass().getMethods();
            for (PropertyDescriptor prop1 : objProps) {
                for (PropertyDescriptor prop2 : builderProps) {
                    if (prop1.getName().equals(prop2.getName())) {
                        Method readMethod = prop1.getReadMethod();
                        Method method = null;
                        for (Method m : methods) {
                            if (m.getName().equals(readMethod.getName().replaceFirst("get", "set"))) {
                                method = m;
                                break;
                            }
                        }
                        try {
                            if (method != null && prop1.getReadMethod() != null) {
                                boolean primitiveOrWrapper = ClassUtils.isPrimitiveOrWrapper(prop1.getReadMethod().getReturnType());

                                if (primitiveOrWrapper) {
                                    method.invoke(builder, prop1.getReadMethod().invoke(obj));
                                } else {
                                    Object invoke = prop1.getReadMethod().invoke(obj);
                                    com.google.protobuf.GeneratedMessage.Builder val = convertToProtobuf(invoke);
                                    method.invoke(builder, val);
                                }
                            }
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
                        }
                        break;
                    }
                }
            }
        } catch (IntrospectionException ex) {
            logger.fatal(ex);
        }

        return builder;
    }

    @Override
    public Object convertFromProtobuf(GeneratedMessage obj) {
        Object target;

        if (TuioProtos.Blob.class.isAssignableFrom(obj.getClass())) {
            target = new TUIO.TuioBlob();
        } else if (TuioProtos.Time.class.isAssignableFrom(obj.getClass())) {
            target = new TUIO.TuioTime();
        } else if (TuioProtos.Cursor.class.isAssignableFrom(obj.getClass())) {
            target = new TUIO.TuioCursor();
        } else if (TuioProtos.Object.class.isAssignableFrom(obj.getClass())) {
            target = new TUIO.TuioObject();
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
                            if (writeMethod != null && readMethod != null && targetProp.getReadMethod() != null) {
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
                        } catch (IllegalArgumentException | SecurityException | IllegalAccessException | InvocationTargetException ex) {
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

    @Override
    public ArrayList<ImmutablePair<String, String>> supportedConversions() {
        ArrayList<ImmutablePair<String, String>> ret = new ArrayList<>();
        ret.add(new ImmutablePair<>(com.artistech.protobuf.TuioProtos.Object.class.getName(), TUIO.TuioObject.class.getName()));
        ret.add(new ImmutablePair<>(com.artistech.protobuf.TuioProtos.Cursor.class.getName(), TUIO.TuioCursor.class.getName()));
        ret.add(new ImmutablePair<>(com.artistech.protobuf.TuioProtos.Blob.class.getName(), TUIO.TuioBlob.class.getName()));
        ret.add(new ImmutablePair<>(com.artistech.protobuf.TuioProtos.Time.class.getName(), TUIO.TuioTime.class.getName()));
        return ret;
    }

    @Override
    public boolean supportsConversion(Object obj) {
        for (ImmutablePair<String, String> pair : supportedConversions()) {
            if (pair.left.equals(obj.getClass().getName()) || pair.right.equals(obj.getClass().getName())) {
                return true;
            }
        }
        return false;
    }
}
