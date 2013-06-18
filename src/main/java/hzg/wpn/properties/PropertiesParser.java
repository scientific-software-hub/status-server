package hzg.wpn.properties;

import hzg.wpn.util.conveter.TypeConverter;
import hzg.wpn.util.conveter.TypeConverters;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @author Igor Khokhriakov <igor.khokhriakov@hzg.de>
 * @since 18.06.13
 */
public class PropertiesParser<T> {
    private final Class<T> scheme;

    @SuppressWarnings("unchecked")
    public PropertiesParser(Class<? extends T> scheme) {
        this.scheme = (Class<T>) scheme;//cast to super
    }

    /**
     * Parse properties defined in Properties annotation in the scheme
     *
     * @return properties container
     * @throws RuntimeException
     */
    public T parseProperties(){
        Properties properties = new Properties();

        hzg.wpn.properties.Properties annot = scheme.getAnnotation(hzg.wpn.properties.Properties.class);
        if(annot == null){
            throw new RuntimeException("There is no Properties annotation defined in the scheme.");
        }

        try {
            InputStream rdr = new BufferedInputStream(scheme.getClassLoader().getResourceAsStream(annot.value()));

            properties.load(rdr);

            return parseProperties(properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param properties
     * @return a newly created properties container with values set to values from properties
     * @throws RuntimeException
     */
    public T parseProperties(Properties properties){
        try {
            T container = scheme.newInstance();

            for(Field fld : scheme.getDeclaredFields()){
                Property propertyDesc = fld.getAnnotation(Property.class);
                if(propertyDesc == null) continue;

                Class<?> type = fld.getType();
                TypeConverter converter = TypeConverters.lookupStringToTypeConverter(type);
                String key = !propertyDesc.key().isEmpty() ? propertyDesc.key() : fld.getName() ;

                if(properties.get(key) == null) continue;

                Object src = properties.get(key);
                fld.set(container,converter.convert(src));
            }

            return container;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
