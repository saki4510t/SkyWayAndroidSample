/*
 * Copyright (c) 2020.  t_saki@serenegiant.com All rights reserved.
 */

package com.serenegiant.skywaytest.api;

import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;

public enum GsonFieldNamingPolicy implements FieldNamingStrategy {
	/**
	 * Using this naming policy with Gson will ensure that the first "letter" of the Java
	 * field name is lower case letter when serialized to its JSON form.
	 *
	 * <p>Here's a few examples of the form "Java Field Name" ---> "JSON Field Name":</p>
	 * <ul>
	 *   <li>someFieldName ---> someFieldName</li>
	 *   <li>_someFieldName ---> _someFieldName</li>
	 * </ul>
	 */
	LOWER_CAMEL_CASE() {
		@Override
		public String translateName(Field f) {
			return lowerCaseFirstLetter(f.getName());
		}
	};

	/**
	 * Converts the field name that uses camel-case define word separation into
	 * separate words that are separated by the provided {@code separatorString}.
	 */
	static String separateCamelCase(String name, String separator) {
		StringBuilder translation = new StringBuilder();
		for (int i = 0, length = name.length(); i < length; i++) {
			char character = name.charAt(i);
			if (Character.isUpperCase(character) && translation.length() != 0) {
				translation.append(separator);
			}
			translation.append(character);
		}
		return translation.toString();
	}

	/**
	 * Ensures the JSON field names begins with an upper case letter.
	 */
	static String upperCaseFirstLetter(String name) {
		StringBuilder fieldNameBuilder = new StringBuilder();
		int index = 0;
		char firstCharacter = name.charAt(index);
		int length = name.length();

		while (index < length - 1) {
			if (Character.isLetter(firstCharacter)) {
				break;
			}

			fieldNameBuilder.append(firstCharacter);
			firstCharacter = name.charAt(++index);
		}

		if (!Character.isUpperCase(firstCharacter)) {
			String modifiedTarget = modifyString(Character.toUpperCase(firstCharacter), name, ++index);
			return fieldNameBuilder.append(modifiedTarget).toString();
		} else {
			return name;
		}
	}

	/**
	 * Ensures the JSON field names begins with an lower case letter.
	 */
	static String lowerCaseFirstLetter(String name) {
		StringBuilder fieldNameBuilder = new StringBuilder();
		int index = 0;
		char firstCharacter = name.charAt(index);
		int length = name.length();

		while (index < length - 1) {
			if (Character.isLetter(firstCharacter)) {
				break;
			}

			fieldNameBuilder.append(firstCharacter);
			firstCharacter = name.charAt(++index);
		}

		if (!Character.isLowerCase(firstCharacter)) {
			String modifiedTarget = modifyString(Character.toLowerCase(firstCharacter), name, ++index);
			return fieldNameBuilder.append(modifiedTarget).toString();
		} else {
			return name;
		}
	}

	private static String modifyString(char firstCharacter, String srcString, int indexOfSubstring) {
		return (indexOfSubstring < srcString.length())
			? firstCharacter + srcString.substring(indexOfSubstring)
			: String.valueOf(firstCharacter);
	}
}
