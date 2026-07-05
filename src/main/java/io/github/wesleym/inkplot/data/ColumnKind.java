package io.github.wesleym.inkplot.data;

import java.util.Locale;

/**
 * Coarse data category of a column, inferred from its declared type name (e.g. "INT", "VARCHAR", "DATETIME").
 * The type name is a hint, not a contract — a blank or unrecognised type reads as {@link #OTHER} and the
 * column classifier falls back to sniffing the values themselves.
 */
public enum ColumnKind {

	NUMERIC,
	TEXT,
	TEMPORAL,
	BOOLEAN,
	OTHER;

	/**
	 * Classifies a SQL/JDBC-style type name. Exotic types (arrays, JSON/XML, binary/LOBs, geometry) fall to
	 * {@link #OTHER} so their values are sniffed rather than assumed.
	 */
	public static ColumnKind of(String typeName) {
		if (typeName == null || typeName.isBlank()) {
			return OTHER;
		}
		String t = typeName.toUpperCase(Locale.ROOT);
		if (t.contains("[]") || t.startsWith("_") || t.equals("ARRAY")) {
			return OTHER;   // PostgreSQL arrays surface as "_text", "text[]", etc.
		}
		if (t.contains("BLOB") || t.contains("BYTEA") || t.contains("BINARY") || t.contains("IMAGE")
				|| t.contains("JSON") || t.contains("XML") || t.contains("TSVECTOR")
				|| t.contains("GEOM") || t.contains("CLOB")
				// Geometry/network/range types that contain "INT"/"DEC" and would otherwise be mis-read as
				// numeric (e.g. POINT, INTERVAL).
				|| t.contains("POINT") || t.contains("POLYGON") || t.contains("LSEG") || t.contains("LINESTRING")
				|| t.contains("INTERVAL") || t.contains("INET") || t.contains("CIDR") || t.contains("MACADDR")) {
			return OTHER;
		}
		if (t.contains("BOOL") || t.equals("BIT")) {
			return BOOLEAN;
		}
		if (t.contains("INT") || t.contains("DEC") || t.contains("NUMERIC") || t.contains("NUMBER")
				|| t.contains("FLOAT") || t.contains("DOUBLE") || t.contains("REAL") || t.contains("MONEY")) {
			return NUMERIC;
		}
		if (t.contains("DATE") || t.contains("TIME")) {   // DATE, TIME, TIMESTAMP, DATETIME
			return TEMPORAL;
		}
		if (t.contains("CHAR") || t.contains("TEXT") || t.contains("STRING")) {
			return TEXT;
		}
		return OTHER;
	}
}
