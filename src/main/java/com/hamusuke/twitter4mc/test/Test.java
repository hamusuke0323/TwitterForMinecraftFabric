package com.hamusuke.twitter4mc.test;

import com.hamusuke.twitter4mc.emoji.Fitzpatrick;
import com.hamusuke.twitter4mc.utils.TwitterUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

@Environment(EnvType.CLIENT)
final class Test {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void main(String[] args) throws TwitterException {

	}

	private static String emojiToHex(String emoji) {
		char[] chars = emoji.toCharArray();
		StringBuilder res = new StringBuilder();
		for (char c : chars) {
			res.append(Integer.toHexString(c));
			res.append(chars[chars.length - 1] == c ? "" : ",");
		}
		return res.toString();
	}

	private static String splitEmojiHex(String text) {
		char[] chars = text.toCharArray();
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (i + 1 != chars.length && Character.isHighSurrogate(c)) {
				res.append(Integer.toHexString(Character.toCodePoint(c, chars[i + 1])));
				i++;
				if (i + 1 != chars.length && (chars[i + 1] == 0x200d || (i + 2 != chars.length && Fitzpatrick.isFitzpatrick(Integer.toHexString(Character.toCodePoint(chars[i + 1], chars[i + 2])))))) {
					res.append("-");
				} else {
					res.append(",");
				}
			} else if (c == 0x200d) {
				res.append(Integer.toHexString(c)).append("-");
			} else {
				res.append(Integer.toHexString(c)).append(",");
			}
		}
		return res.length() < 2 ? "" : res.deleteCharAt(res.length() - 1).toString();
	}

	private static int emojiLength(String emojiHex) {
		if (emojiHex.isEmpty()) {
			return 0;
		}
		String[] strings = emojiHex.split("-");
		int res = 0;
		for (String s : strings) {
			res += Character.charCount(Integer.decode("0x" + s));
		}
		return res;
	}
}
