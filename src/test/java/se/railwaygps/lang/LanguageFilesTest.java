package se.railwaygps.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that every language file defines the same non-blank keys as every other language file. */
class LanguageFilesTest {
	private static final Pattern KEY_PATTERN = Pattern.compile("\"(railwaygps\\.[a-z0-9_.]+)\"");
	private static final Type STRING_MAP = new TypeToken<Map<String, String>>() {
	}.getType();

	@Test
	void allLanguagesDefineTheSameNonBlankKeys() throws Exception {
		Map<String, String> english = load("en_us");
		Map<String, String> swedish = load("sv_se");

		assertFalse(english.isEmpty());
		assertEquals(english.keySet(), swedish.keySet());
		for (Map.Entry<String, String> entry : english.entrySet()) {
			assertFalse(entry.getValue().isBlank(), "Blank en_us value for " + entry.getKey());
			assertFalse(swedish.get(entry.getKey()).isBlank(), "Blank sv_se value for " + entry.getKey());
		}
	}

	@Test
	void everyTranslationKeyReferencedInSourceExistsInAllLanguages() throws Exception {
		Map<String, String> english = load("en_us");
		Map<String, String> swedish = load("sv_se");
		Set<String> referenced = new HashSet<>();
		collectReferencedKeys(Paths.get("src/main/java"), referenced);
		collectReferencedKeys(Paths.get("src/client/java"), referenced);
		assertFalse(referenced.isEmpty(), "No translation keys were found in the scanned sources");

		for (String key : referenced) {
			assertTrue(isDefined(english, key), "Missing en_us translation for " + key);
			assertTrue(isDefined(swedish, key), "Missing sv_se translation for " + key);
		}
	}

	private static boolean isDefined(Map<String, String> language, String key) {
		if (key.endsWith(".")) {
			return language.keySet().stream().anyMatch(defined -> defined.startsWith(key));
		}
		return language.containsKey(key);
	}

	private static void collectReferencedKeys(Path root, Set<String> keys) throws IOException {
		try (var files = Files.walk(root)) {
			files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				try {
					Matcher matcher = KEY_PATTERN.matcher(Files.readString(path));
					while (matcher.find()) {
						keys.add(matcher.group(1));
					}
				} catch (IOException exception) {
					throw new UncheckedIOException(exception);
				}
			});
		}
	}

	private static Map<String, String> load(String language) throws IOException {
		try (var input = LanguageFilesTest.class.getResourceAsStream("/assets/railwaygps/lang/" + language + ".json")) {
			if (input == null) {
				throw new IOException("Missing language file " + language + ".json");
			}
			Map<String, String> map = new Gson().fromJson(
					new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8), STRING_MAP);
			return map == null ? new HashMap<>() : map;
		}
	}
}
