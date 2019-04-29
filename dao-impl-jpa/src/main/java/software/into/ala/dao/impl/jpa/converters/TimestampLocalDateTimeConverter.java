package software.into.ala.dao.impl.jpa.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.AttributeConverter;

public class TimestampLocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(LocalDateTime ldt) {
		return (Objects.nonNull(ldt)) ? Timestamp.valueOf(ldt) : null;
	}

	@Override
	public LocalDateTime convertToEntityAttribute(Timestamp t) {
		return (Objects.nonNull(t)) ? t.toLocalDateTime() : null;
	}
}
