package fr.fullstack.shopapp.converter;

import org.springframework.data.convert.ReadingConverter;
import org.springframework.core.convert.converter.Converter;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@ReadingConverter
public class LocalTimeConverterFromLong implements Converter<Long, LocalTime> {
    @Override
    public LocalTime convert(Long source) {
        Instant instant = Instant.ofEpochMilli(source);
        return instant.atZone(ZoneId.systemDefault()).toLocalTime();
    }
}