package se.icus.mag.loomassistant.util;

import com.google.common.hash.Hashing;
import java.nio.file.Path;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class StringUtils {
	private StringUtils() {
	}

	@SuppressWarnings("deprecation")
	public static String getIdPath(String id) {
		return Util.sanitizeName(id, Identifier::validPathChar)
				+ "/"
				+ Hashing.sha1().hashUnencodedChars(id);
	}

	public static String pathKeySuffix(Path path) {
		StringBuilder builder = new StringBuilder();
		for (Path part : path) {
			if (!builder.isEmpty()) {
				builder.append('/');
			}
			builder.append(part);
		}
		return builder.toString();
	}

	public static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
