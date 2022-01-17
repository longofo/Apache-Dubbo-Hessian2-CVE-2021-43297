//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.alibaba.com.caucho.hessian.io;

import com.alibaba.com.caucho.hessian.io.java8.*;

import javax.management.ObjectName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerializerFactory extends AbstractSerializerFactory {
    private static final Logger log = Logger.getLogger(SerializerFactory.class.getName());
    private static Deserializer OBJECT_DESERIALIZER = new BasicDeserializer(13);
    private static ConcurrentHashMap _unrecognizedTypeCache = new ConcurrentHashMap();
    private static HashMap _staticSerializerMap = new HashMap();
    private static HashMap _staticDeserializerMap = new HashMap();
    private static HashMap _staticTypeMap = new HashMap();
    protected Serializer _defaultSerializer;
    protected ArrayList _factories;
    protected CollectionSerializer _collectionSerializer;
    protected MapSerializer _mapSerializer;
    private ClassLoader _loader;
    private Deserializer _hashMapDeserializer;
    private Deserializer _arrayListDeserializer;
    private ConcurrentHashMap _cachedSerializerMap;
    private ConcurrentHashMap _cachedDeserializerMap;
    private ConcurrentHashMap _cachedTypeDeserializerMap;
    private boolean _isAllowNonSerializable;
    private Map<String, Object> _typeNotFoundDeserializerMap;
    private static final Object PRESENT;
    private ClassFactory _classFactory;

    public SerializerFactory() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public SerializerFactory(ClassLoader loader) {
        this._factories = new ArrayList();
        this._typeNotFoundDeserializerMap = new ConcurrentHashMap(8);
        this._loader = loader;
    }

    public Class<?> loadSerializedClass(String className) throws ClassNotFoundException {
        return this.getClassFactory().load(className);
    }

    public ClassFactory getClassFactory() {
        synchronized (this) {
            if (this._classFactory == null) {
                this._classFactory = new ClassFactory(this.getClassLoader());
            }

            return this._classFactory;
        }
    }

    private static void addBasic(Class cl, String typeName, int type) {
        _staticSerializerMap.put(cl, new BasicSerializer(type));
        Deserializer deserializer = new BasicDeserializer(type);
        _staticDeserializerMap.put(cl, deserializer);
        _staticTypeMap.put(typeName, deserializer);
    }

    public ClassLoader getClassLoader() {
        return this._loader;
    }

    public void setSendCollectionType(boolean isSendType) {
        if (this._collectionSerializer == null) {
            this._collectionSerializer = new CollectionSerializer();
        }

        this._collectionSerializer.setSendJavaType(isSendType);
        if (this._mapSerializer == null) {
            this._mapSerializer = new MapSerializer();
        }

        this._mapSerializer.setSendJavaType(isSendType);
    }

    public void addFactory(AbstractSerializerFactory factory) {
        this._factories.add(factory);
    }

    public boolean isAllowNonSerializable() {
        return this._isAllowNonSerializable;
    }

    public void setAllowNonSerializable(boolean allow) {
        this._isAllowNonSerializable = allow;
    }

    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        Serializer serializer = (Serializer) _staticSerializerMap.get(cl);
        if (serializer != null) {
            return (Serializer) serializer;
        } else {
            if (this._cachedSerializerMap != null) {
                serializer = (Serializer) this._cachedSerializerMap.get(cl);
                if (serializer != null) {
                    return (Serializer) serializer;
                }
            }

            for (int i = 0; serializer == null && this._factories != null && i < this._factories.size(); ++i) {
                AbstractSerializerFactory factory = (AbstractSerializerFactory) this._factories.get(i);
                serializer = factory.getSerializer(cl);
            }

            if (serializer == null) {
                if (isZoneId(cl)) {
                    serializer = ZoneIdSerializer.getInstance();
                } else if (isEnumSet(cl)) {
                    serializer = EnumSetSerializer.getInstance();
                } else if (JavaSerializer.getWriteReplace(cl) != null) {
                    serializer = new JavaSerializer(cl, this._loader);
                } else if (HessianRemoteObject.class.isAssignableFrom(cl)) {
                    serializer = new RemoteSerializer();
                } else if (Map.class.isAssignableFrom(cl)) {
                    if (this._mapSerializer == null) {
                        this._mapSerializer = new MapSerializer();
                    }

                    serializer = this._mapSerializer;
                } else if (Collection.class.isAssignableFrom(cl)) {
                    if (this._collectionSerializer == null) {
                        this._collectionSerializer = new CollectionSerializer();
                    }

                    serializer = this._collectionSerializer;
                } else if (cl.isArray()) {
                    serializer = new ArraySerializer();
                } else if (Throwable.class.isAssignableFrom(cl)) {
                    serializer = new ThrowableSerializer(cl, this.getClassLoader());
                } else if (InputStream.class.isAssignableFrom(cl)) {
                    serializer = new InputStreamSerializer();
                } else if (Iterator.class.isAssignableFrom(cl)) {
                    serializer = IteratorSerializer.create();
                } else if (Enumeration.class.isAssignableFrom(cl)) {
                    serializer = EnumerationSerializer.create();
                } else if (Calendar.class.isAssignableFrom(cl)) {
                    serializer = CalendarSerializer.create();
                } else if (Locale.class.isAssignableFrom(cl)) {
                    serializer = LocaleSerializer.create();
                } else if (Enum.class.isAssignableFrom(cl)) {
                    serializer = new EnumSerializer(cl);
                }
            }

            if (serializer == null) {
                serializer = this.getDefaultSerializer(cl);
            }

            if (this._cachedSerializerMap == null) {
                this._cachedSerializerMap = new ConcurrentHashMap(8);
            }

            this._cachedSerializerMap.put(cl, serializer);
            return (Serializer) serializer;
        }
    }

    protected Serializer getDefaultSerializer(Class cl) {
        this._isAllowNonSerializable = true;
        if (this._defaultSerializer != null) {
            return this._defaultSerializer;
        } else if (!Serializable.class.isAssignableFrom(cl) && !this._isAllowNonSerializable) {
            throw new IllegalStateException("Serialized class " + cl.getName() + " must implement java.io.Serializable");
        } else {
            return new JavaSerializer(cl, this._loader);
        }
    }

    public Deserializer getDeserializer(Class cl) throws HessianProtocolException {
        Deserializer deserializer = (Deserializer) _staticDeserializerMap.get(cl);
        if (deserializer != null) {
            return (Deserializer) deserializer;
        } else {
            if (this._cachedDeserializerMap != null) {
                deserializer = (Deserializer) this._cachedDeserializerMap.get(cl);
                if (deserializer != null) {
                    return (Deserializer) deserializer;
                }
            }

            for (int i = 0; deserializer == null && this._factories != null && i < this._factories.size(); ++i) {
                AbstractSerializerFactory factory = (AbstractSerializerFactory) this._factories.get(i);
                deserializer = factory.getDeserializer(cl);
            }

            if (deserializer == null) {
                if (Collection.class.isAssignableFrom(cl)) {
                    deserializer = new CollectionDeserializer(cl);
                } else if (Map.class.isAssignableFrom(cl)) {
                    deserializer = new MapDeserializer(cl);
                } else if (cl.isInterface()) {
                    deserializer = new ObjectDeserializer(cl);
                } else if (cl.isArray()) {
                    deserializer = new ArrayDeserializer(cl.getComponentType());
                } else if (Enumeration.class.isAssignableFrom(cl)) {
                    deserializer = EnumerationDeserializer.create();
                } else if (Enum.class.isAssignableFrom(cl)) {
                    deserializer = new EnumDeserializer(cl);
                } else if (Class.class.equals(cl)) {
                    deserializer = new ClassDeserializer(this._loader);
                } else {
                    deserializer = this.getDefaultDeserializer(cl);
                }
            }

            if (this._cachedDeserializerMap == null) {
                this._cachedDeserializerMap = new ConcurrentHashMap(8);
            }

            this._cachedDeserializerMap.put(cl, deserializer);
            return (Deserializer) deserializer;
        }
    }

    protected Deserializer getDefaultDeserializer(Class cl) {
        return new JavaDeserializer(cl);
    }

    public Object readList(AbstractHessianInput in, int length, String type) throws HessianProtocolException, IOException {
        Deserializer deserializer = this.getDeserializer(type);
        return deserializer != null ? deserializer.readList(in, length) : (new CollectionDeserializer(ArrayList.class)).readList(in, length);
    }

    public Object readMap(AbstractHessianInput in, String type) throws HessianProtocolException, IOException {
        return this.readMap(in, type, (Class) null, (Class) null);
    }

    public Object readMap(AbstractHessianInput in, String type, Class<?> expectKeyType, Class<?> expectValueType) throws HessianProtocolException, IOException {
        Deserializer deserializer = this.getDeserializer(type);
        if (deserializer != null) {
            return deserializer.readMap(in);
        } else if (this._hashMapDeserializer != null) {
            return this._hashMapDeserializer.readMap(in, expectKeyType, expectValueType);
        } else {
            this._hashMapDeserializer = new MapDeserializer(HashMap.class);
            return this._hashMapDeserializer.readMap(in, expectKeyType, expectValueType);
        }
    }

    public Object readObject(AbstractHessianInput in, String type, String[] fieldNames) throws HessianProtocolException, IOException {
        Deserializer deserializer = this.getDeserializer(type);
        if (deserializer != null) {
            return deserializer.readObject(in, fieldNames);
        } else if (this._hashMapDeserializer != null) {
            return this._hashMapDeserializer.readObject(in, fieldNames);
        } else {
            this._hashMapDeserializer = new MapDeserializer(HashMap.class);
            return this._hashMapDeserializer.readObject(in, fieldNames);
        }
    }

    public Deserializer getObjectDeserializer(String type, Class cl) throws HessianProtocolException {
        Deserializer reader = this.getObjectDeserializer(type);
        if (cl != null && !cl.equals(reader.getType()) && !cl.isAssignableFrom(reader.getType()) && !HessianHandle.class.isAssignableFrom(reader.getType())) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("hessian: expected '" + cl.getName() + "' at '" + type + "' (" + reader.getType().getName() + ")");
            }

            return this.getDeserializer(cl);
        } else {
            return reader;
        }
    }

    public Deserializer getObjectDeserializer(String type) throws HessianProtocolException {
        Deserializer deserializer = this.getDeserializer(type);
        if (deserializer != null) {
            return deserializer;
        } else if (this._hashMapDeserializer != null) {
            return this._hashMapDeserializer;
        } else {
            this._hashMapDeserializer = new MapDeserializer(HashMap.class);
            return this._hashMapDeserializer;
        }
    }

    public Deserializer getListDeserializer(String type, Class cl) throws HessianProtocolException {
        Deserializer reader = this.getListDeserializer(type);
        if (cl != null && !cl.equals(reader.getType()) && !cl.isAssignableFrom(reader.getType())) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("hessian: expected '" + cl.getName() + "' at '" + type + "' (" + reader.getType().getName() + ")");
            }

            return this.getDeserializer(cl);
        } else {
            return reader;
        }
    }

    public Deserializer getListDeserializer(String type) throws HessianProtocolException {
        Deserializer deserializer = this.getDeserializer(type);
        if (deserializer != null) {
            return deserializer;
        } else if (this._arrayListDeserializer != null) {
            return this._arrayListDeserializer;
        } else {
            this._arrayListDeserializer = new CollectionDeserializer(ArrayList.class);
            return this._arrayListDeserializer;
        }
    }

    public Deserializer getDeserializer(String type) throws HessianProtocolException {
        if (type != null && !type.equals("") && !this._typeNotFoundDeserializerMap.containsKey(type)) {
            if (this._cachedTypeDeserializerMap != null) {
                Deserializer deserializer = (Deserializer) this._cachedTypeDeserializerMap.get(type);
                if (deserializer != null) {
                    return deserializer;
                }
            }

            Deserializer deserializer = (Deserializer) _staticTypeMap.get(type);
            if (deserializer != null) {
                return (Deserializer) deserializer;
            } else {
                if (type.startsWith("[")) {
                    Deserializer subDeserializer = this.getDeserializer(type.substring(1));
                    if (subDeserializer != null) {
                        deserializer = new ArrayDeserializer(subDeserializer.getType());
                    } else {
                        deserializer = new ArrayDeserializer(Object.class);
                    }
                } else if (_unrecognizedTypeCache.get(type) == null) {
                    try {
                        Class cl = this.loadSerializedClass(type);
                        deserializer = this.getDeserializer(cl);
                    } catch (Exception var4) {
                        log.warning("Hessian/Burlap: '" + type + "' is an unknown class in " + this._loader + ":\n" + var4);
                        this._typeNotFoundDeserializerMap.put(type, PRESENT);
                        log.log(Level.FINER, var4.toString(), var4);
                        _unrecognizedTypeCache.put(type, new AtomicLong(1L));
                    }
                } else {
                    ((AtomicLong) _unrecognizedTypeCache.get(type)).incrementAndGet();
                    if (((AtomicLong) _unrecognizedTypeCache.get(type)).get() % 2000L == 0L) {
                        ((AtomicLong) _unrecognizedTypeCache.get(type)).getAndSet(1L);
                    }
                }

                if (deserializer != null) {
                    if (this._cachedTypeDeserializerMap == null) {
                        this._cachedTypeDeserializerMap = new ConcurrentHashMap(8);
                    }

                    this._cachedTypeDeserializerMap.put(type, deserializer);
                }

                return (Deserializer) deserializer;
            }
        } else {
            return null;
        }
    }

    private static boolean isZoneId(Class cl) {
        try {
            return isJava8() && Class.forName("java.time.ZoneId").isAssignableFrom(cl);
        } catch (ClassNotFoundException var2) {
            return false;
        }
    }

    private static boolean isEnumSet(Class cl) {
        return EnumSet.class.isAssignableFrom(cl);
    }

    private static boolean isJava8() {
        String javaVersion = System.getProperty("java.specification.version");
        return Double.valueOf(javaVersion) >= 1.8D;
    }

    static {
        addBasic(Void.TYPE, "void", 0);
        addBasic(Boolean.class, "boolean", 1);
        addBasic(Byte.class, "byte", 2);
        addBasic(Short.class, "short", 3);
        addBasic(Integer.class, "int", 4);
        addBasic(Long.class, "long", 5);
        addBasic(Float.class, "float", 6);
        addBasic(Double.class, "double", 7);
        addBasic(Character.class, "char", 9);
        addBasic(String.class, "string", 10);
        addBasic(Object.class, "object", 13);
        addBasic(Date.class, "date", 11);
        addBasic(Boolean.TYPE, "boolean", 1);
        addBasic(Byte.TYPE, "byte", 2);
        addBasic(Short.TYPE, "short", 3);
        addBasic(Integer.TYPE, "int", 4);
        addBasic(Long.TYPE, "long", 5);
        addBasic(Float.TYPE, "float", 6);
        addBasic(Double.TYPE, "double", 7);
        addBasic(Character.TYPE, "char", 8);
        addBasic(boolean[].class, "[boolean", 14);
        addBasic(byte[].class, "[byte", 15);
        addBasic(short[].class, "[short", 16);
        addBasic(int[].class, "[int", 17);
        addBasic(long[].class, "[long", 18);
        addBasic(float[].class, "[float", 19);
        addBasic(double[].class, "[double", 20);
        addBasic(char[].class, "[char", 21);
        addBasic(String[].class, "[string", 22);
        addBasic(Object[].class, "[object", 23);
        _staticSerializerMap.put(Class.class, new ClassSerializer());
        _staticDeserializerMap.put(Number.class, new BasicDeserializer(12));
        _staticSerializerMap.put(BigDecimal.class, new StringValueSerializer());

        try {
            _staticDeserializerMap.put(BigDecimal.class, new StringValueDeserializer(BigDecimal.class));
            _staticDeserializerMap.put(BigInteger.class, new BigIntegerDeserializer());
        } catch (Throwable var6) {
        }

        _staticSerializerMap.put(UUID.class, new StringValueSerializer());
        _staticDeserializerMap.put(UUID.class, new UUIDDeserializer());
        _staticSerializerMap.put(File.class, new StringValueSerializer());

        try {
            _staticDeserializerMap.put(File.class, new StringValueDeserializer(File.class));
        } catch (Throwable var5) {
        }

        _staticSerializerMap.put(ObjectName.class, new StringValueSerializer());

        try {
            _staticDeserializerMap.put(ObjectName.class, new StringValueDeserializer(ObjectName.class));
        } catch (Throwable var4) {
        }

        _staticSerializerMap.put(java.sql.Date.class, new SqlDateSerializer());
        _staticSerializerMap.put(Time.class, new SqlDateSerializer());
        _staticSerializerMap.put(Timestamp.class, new SqlDateSerializer());
        _staticSerializerMap.put(InputStream.class, new InputStreamSerializer());
        _staticDeserializerMap.put(InputStream.class, new InputStreamDeserializer());

        try {
            _staticDeserializerMap.put(java.sql.Date.class, new SqlDateDeserializer(java.sql.Date.class));
            _staticDeserializerMap.put(Time.class, new SqlDateDeserializer(Time.class));
            _staticDeserializerMap.put(Timestamp.class, new SqlDateDeserializer(Timestamp.class));
        } catch (Throwable var3) {
            var3.printStackTrace();
        }

        try {
            Class stackTrace = StackTraceElement.class;
            _staticDeserializerMap.put(stackTrace, new StackTraceElementDeserializer());
        } catch (Throwable var2) {
        }

        try {
            if (isJava8()) {
                _staticSerializerMap.put(Class.forName("java.time.LocalTime"), Java8TimeSerializer.create(LocalTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.LocalDate"), Java8TimeSerializer.create(LocalDateHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.LocalDateTime"), Java8TimeSerializer.create(LocalDateTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.Instant"), Java8TimeSerializer.create(InstantHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.Duration"), Java8TimeSerializer.create(DurationHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.Period"), Java8TimeSerializer.create(PeriodHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.Year"), Java8TimeSerializer.create(YearHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.YearMonth"), Java8TimeSerializer.create(YearMonthHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.MonthDay"), Java8TimeSerializer.create(MonthDayHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.OffsetDateTime"), Java8TimeSerializer.create(OffsetDateTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.ZoneOffset"), Java8TimeSerializer.create(ZoneOffsetHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.OffsetTime"), Java8TimeSerializer.create(OffsetTimeHandle.class));
                _staticSerializerMap.put(Class.forName("java.time.ZonedDateTime"), Java8TimeSerializer.create(ZonedDateTimeHandle.class));
            }
        } catch (Throwable var1) {
            log.warning(String.valueOf(var1.getCause()));
        }

        PRESENT = new Object();
    }
}
