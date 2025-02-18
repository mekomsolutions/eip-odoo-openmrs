/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OdooUtils {

    @Autowired
    private Environment environment;

    public <T> T convertToObject(Map<String, Object> data, Class<T> objectClass) {
        ObjectMapper mapper = new ObjectMapper();
        log.debug("OdooUtils: Converting map {} to object {}", data, objectClass.getName());
        try {
            T obj = mapper.convertValue(data, objectClass);

            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(JsonProperty.class) && !field.isAnnotationPresent(JsonIgnore.class)) {
                    JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                    String propertyValue = environment.getProperty(jsonProperty.value());
                    if (propertyValue != null && data.containsKey(propertyValue)) {
                        field.setAccessible(true);
                        field.set(obj, convertValueToType(data.get(propertyValue), field.getType()));
                    }
                }
            }
            log.debug("OdooUtils: Converted map {} to object {}", data, obj);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error converting map %s to object: %s", data, e.getMessage()));
        }
    }

    public Map<String, Object> convertObjectToMap(Object object) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        log.debug("OdooUtils: Converting object {} to map", object.getClass().getName());

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(JsonProperty.class) && !field.isAnnotationPresent(JsonIgnore.class)) {
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                String propertyValue = environment.getProperty(jsonProperty.value());
                String key = propertyValue == null ? jsonProperty.value() : propertyValue;
                Object value = field.get(object);
                map.put(key, value);
            }
        }
        log.debug("OdooUtils: Converted object {} to map {}", object.getClass().getName(), map);
        return map;
    }

    private Object convertValueToType(Object value, Class<?> targetType) {
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value.toString());
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value.toString());
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value.toString());
        } else if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(value.toString());
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == String.class) {
            return value.toString();
        }
        throw new IllegalArgumentException("Unsupported type: " + targetType.getName());
    }

    public static String convertEEEMMMddDateToOdooFormat(String date) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            LocalDate localDate = LocalDate.parse(date, inputFormatter);
            return localDate.format(outputFormatter);
        } catch (DateTimeParseException e) {
            log.error("Cannot convert input date to Odoo date. Error: {}", e.getMessage());
            return ""; // Returning empty string if not able to parse input date
        }
    }
}
